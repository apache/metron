package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.common.utils.file.ReaderSpliterator;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.HTableProvider;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.tools.javac.jvm.ByteCodes.ret;

public enum LocalImporter implements Importer {
  INSTANCE;
  private static ThreadLocal<FileSystem> fs = new ThreadLocal<FileSystem>(){

    @Override
    protected FileSystem initialValue() {
      try {
        return FileSystem.get(new Configuration());
      } catch (IOException e) {
        throw new IllegalStateException("Unable to retrieve the filesystem: " + e.getMessage(), e);
      }
    }
  };

  /**
   * Location can be either a local file or a file on HDFS.
   */
  private static final class Location {
    private String loc;
    private boolean isLocal;

    public Location(String loc) {
      this(loc, !loc.startsWith("hdfs://"));
    }
    public Location(String loc, boolean isLocal) {
      this.loc = loc;
      this.isLocal = isLocal;
    }

    public Optional<List<Location>> getChildren() throws IOException {
        if(exists() && isDirectory()) {
          List<Location> children = new ArrayList<>();
          for(String child : list().orElse(new ArrayList<>())) {
            children.add(new Location(child, isLocal));
          }
          return Optional.of(children);
        }
        else {
          return Optional.empty();
        }
    }

    private Optional<List<String>> list() throws IOException {
      List<String> children = new ArrayList<>();
      if(isLocal) {
        for(File f : new File(loc).listFiles()) {
          children.add(f.getPath());
        }
      }
      else {
        for(FileStatus f : fs.get().listStatus(new Path(loc)) ) {
          children.add(f.getPath().toString());
        }
      }
      return Optional.of(children);
    }

    public boolean exists() throws IOException {
      if(isLocal) {
        return new File(loc).exists();
      }
      else {
        return fs.get().exists(new Path(loc));
      }
    }

    public boolean isDirectory() throws IOException {
      if(isLocal) {
        return new File(loc).isDirectory();
      }
      else {
        return fs.get().isDirectory(new Path(loc));
      }
    }

    public BufferedReader openReader() throws IOException {
      if(isLocal) {
        return new BufferedReader(new FileReader(new File(loc)));
      }
      else {
        return new BufferedReader(new InputStreamReader(fs.get().open(new Path(loc))));
      }
    }

    @Override
    public String toString() {
      return loc;
    }
  }

  public interface HTableProviderRetriever {
    HTableProvider retrieve();
  }


  @Override
  public void importData( final EnumMap<LoadOptions, Optional<Object>> config
                        , final ExtractorHandler handler
                        , final Configuration hadoopConfig
                         ) throws IOException {
    importData(config, handler, hadoopConfig, () -> new HTableProvider());

  }
  public void importData( final EnumMap<LoadOptions, Optional<Object>> config
                        , final ExtractorHandler handler
                        , final Configuration hadoopConfig
                        , final HTableProviderRetriever provider
                         ) throws IOException {
    ThreadLocal<ExtractorState> state = new ThreadLocal<ExtractorState>() {
      @Override
      protected ExtractorState initialValue() {
        try {
          HTableInterface table = provider.retrieve().getTable(hadoopConfig, (String) config.get(LoadOptions.HBASE_TABLE).get());
          return new ExtractorState(table, handler.getExtractor(), new EnrichmentConverter());
        } catch (IOException e1) {
          throw new IllegalStateException("Unable to get table: " + e1);
        }
      }
    };

    boolean lineByLine = !handler.getInputFormatHandler().getClass().equals(WholeFileFormat.class);
    List<String> inputs = (List<String>) config.get(LoadOptions.INPUT).get();
    String cf = (String) config.get(LoadOptions.HBASE_CF).get();
    if(!lineByLine) {
      extractWholeFiles(inputs, state, cf);
    }
    else {
      int batchSize = (int) config.get(LoadOptions.BATCH_SIZE).get();
      int numThreads = (int) config.get(LoadOptions.NUM_THREADS).get();
      extractLineByLine(inputs, state, cf, batchSize, numThreads);
    }

  }

  public void extractLineByLine( List<String> inputs
                               , ThreadLocal<ExtractorState> state
                               , String cf
                               , int batchSize
                               , int numThreads
                               ) throws IOException {
    inputs.stream().map(input -> new Location(input))
                   .forEach( loc -> {
                             try (Stream<String> stream = ReaderSpliterator.lineStream(loc.openReader(), batchSize)) {

                               ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
                               forkJoinPool.submit(() ->
                                       stream.parallel().forEach(input -> {
                                                 ExtractorState es = state.get();
                                                 try {
                                                   es.getTable().put(extract(input, es.getExtractor(), cf, es.getConverter()));
                                                 } catch (IOException e) {
                                                   throw new IllegalStateException("Unable to continue: " + e.getMessage(), e);
                                                 }
                                               }
                                       )
                               ).get();
                             } catch (Exception e) {
                               throw new IllegalStateException(e.getMessage(), e);
                             }
                           }
                   );
  }

  public void extractWholeFiles( List<String> inputs, ThreadLocal<ExtractorState> state, String cf) throws IOException {
    final List<Location> locations = new ArrayList<>();
      fileVisitor(inputs, loc -> locations.add(loc));
      locations.parallelStream().forEach(loc -> {
        try(BufferedReader br = loc.openReader()) {
          String s = br.lines().collect(Collectors.joining());
          state.get().getTable().put(extract(s, state.get().getExtractor(), cf, state.get().getConverter()));
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read " + loc + ": " + e.getMessage(), e);
        }
      });
  }


  public List<Put> extract(String line
                     , Extractor extractor
                     , String cf
                     , HbaseConverter converter
                     ) throws IOException
  {
    List<Put> ret = new ArrayList<>();
    Iterable<LookupKV> kvs = extractor.extract(line);
    for(LookupKV kv : kvs) {
      Put put = converter.toPut(cf, kv.getKey(), kv.getValue());
      ret.add(put);
    }
    return ret;
  }


  public void fileVisitor(List<String> inputs
                         , final Consumer<Location> importConsumer
                         ) throws IOException {
    Stack<Location> stack = new Stack<>();
    for(String input : inputs) {
      Location loc = new Location(input);
      if(loc.exists()) {
        stack.add(loc);
      }
    }
    while(!stack.empty()) {
      Location loc = stack.pop();
      if(loc.isDirectory()) {
        for(Location child : loc.getChildren().orElse(Collections.emptyList())) {
          stack.push(child);
        }
      }
      else {
        importConsumer.accept(loc);
      }
    }
  }

}
