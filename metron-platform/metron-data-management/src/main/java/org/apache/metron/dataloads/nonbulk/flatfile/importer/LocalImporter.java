/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.dataloads.nonbulk.flatfile.importer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.metron.common.utils.file.ReaderSpliterator;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.dataloads.nonbulk.flatfile.ExtractorState;
import org.apache.metron.dataloads.nonbulk.flatfile.LoadOptions;
import org.apache.metron.dataloads.nonbulk.flatfile.location.Location;
import org.apache.metron.dataloads.nonbulk.flatfile.location.LocationStrategy;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.hbase.HTableProvider;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum LocalImporter implements Importer {
  INSTANCE;

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
          return new ExtractorState(table, handler.getExtractor(), new EnrichmentConverter(), hadoopConfig);
        } catch (IOException e1) {
          throw new IllegalStateException("Unable to get table: " + e1);
        }
      }
    };
    boolean quiet = (boolean) config.get(LoadOptions.QUIET).get();
    boolean lineByLine = !handler.getInputFormat().getClass().equals(WholeFileFormat.class);
    List<String> inputs = (List<String>) config.get(LoadOptions.INPUT).get();
    String cf = (String) config.get(LoadOptions.HBASE_CF).get();
    if(!lineByLine) {
      extractWholeFiles(inputs, state, cf, quiet);
    }
    else {
      int batchSize = (int) config.get(LoadOptions.BATCH_SIZE).get();
      int numThreads = (int) config.get(LoadOptions.NUM_THREADS).get();
      extractLineByLine(inputs, state, cf, batchSize, numThreads, quiet);
    }

  }

  public void extractLineByLine( List<String> inputs
                               , ThreadLocal<ExtractorState> state
                               , String cf
                               , int batchSize
                               , int numThreads
                               , boolean quiet
                               ) throws IOException {
    inputs.stream().map(input -> LocationStrategy.getLocation(input, state.get().getFileSystem()))
                   .forEach( loc -> {
                      final Progress progress = new Progress();
                      if(!quiet) {
                        System.out.println("\nProcessing " + loc.toString());
                      }
                      try (Stream<String> stream = ReaderSpliterator.lineStream(loc.openReader(), batchSize)) {
                        ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
                        forkJoinPool.submit(() ->
                          stream.parallel().forEach(input -> {
                            ExtractorState es = state.get();
                            try {
                              es.getTable().put(extract(input, es.getExtractor(), cf, es.getConverter(), progress, quiet));
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

  public void extractWholeFiles( List<String> inputs, ThreadLocal<ExtractorState> state, String cf, boolean quiet) throws IOException {
    final Progress progress = new Progress();
    final List<Location> locations = new ArrayList<>();
      Location.fileVisitor(inputs, loc -> locations.add(loc), state.get().getFileSystem());
      locations.parallelStream().forEach(loc -> {
        try(BufferedReader br = loc.openReader()) {
          String s = br.lines().collect(Collectors.joining());
          state.get().getTable().put(extract( s
                                            , state.get().getExtractor()
                                            , cf, state.get().getConverter()
                                            , progress
                                            , quiet
                                            )
                                    );
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read " + loc + ": " + e.getMessage(), e);
        }
      });
  }


  public List<Put> extract(String line
                     , Extractor extractor
                     , String cf
                     , HbaseConverter converter
                     , final Progress progress
                     , final boolean quiet
                     ) throws IOException
  {
    List<Put> ret = new ArrayList<>();
    Iterable<LookupKV> kvs = extractor.extract(line);
    for(LookupKV kv : kvs) {
      Put put = converter.toPut(cf, kv.getKey(), kv.getValue());
      ret.add(put);
    }
    if(!quiet) {
      progress.update();
    }
    return ret;
  }


  public static class Progress {
    private int count = 0;
    private String anim= "|/-\\";

    public synchronized void update() {
      int currentCount = count++;
      System.out.print("\rProcessed " + currentCount + " - " + anim.charAt(currentCount % anim.length()));
    }
  }

}
