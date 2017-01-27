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
package org.apache.metron.dataloads.nonbulk.flatfile;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.file.ReaderSpliterator;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentUpdateConfig;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.common.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class SimpleEnrichmentFlatFileLoader {
  private static abstract class OptionHandler implements Function<String, Option> {}
  public static enum LoadOptions {
    HELP("h", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    })
    ,HBASE_TABLE("t", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "hbase_table", true, "HBase table to ingest the data into.");
        o.setArgName("TABLE");
        o.setRequired(true);
        return o;
      }
    })
    ,HBASE_CF("c", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "hbase_cf", true, "HBase column family to ingest the data into.");
        o.setArgName("CF");
        o.setRequired(true);
        return o;
      }
    })
    ,EXTRACTOR_CONFIG("e", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "extractor_config", true, "JSON Document describing the extractor for this input data source");
        o.setArgName("JSON_FILE");
        o.setRequired(true);
        return o;
      }
    })
    ,ENRICHMENT_CONFIG("n", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "enrichment_config", true
                , "JSON Document describing the enrichment configuration details." +
                "  This is used to associate an enrichment type with a field type in zookeeper."
        );
        o.setArgName("JSON_FILE");
        o.setRequired(false);
        return o;
      }
    })
    ,LOG4J_PROPERTIES("l", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "log4j", true, "The log4j properties file to load");
        o.setArgName("FILE");
        o.setRequired(false);
        return o;
      }
    })
    ,NUM_THREADS("p", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "threads", true, "The number of threads to use when extracting data.  The default is the number of cores of your machine.");
        o.setArgName("NUM_THREADS");
        o.setRequired(false);
        return o;
      }
    })
    ,BATCH_SIZE("b", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "batchSize", true, "The batch size to use for HBase puts");
        o.setArgName("SIZE");
        o.setRequired(false);
        return o;
      }
    })
    ,INPUT("i", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "input", true, "The CSV File to load");
        o.setArgName("FILE");
        o.setRequired(true);
        return o;
      }
    })
    ;
    Option option;
    String shortCode;
    LoadOptions(String shortCode, OptionHandler optionHandler) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if(HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        System.exit(-1);
        return null;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "SimpleEnrichmentFlatFileLoader", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(LoadOptions o : LoadOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }
  public static List<File> getFiles(File root) {
    if(!root.isDirectory())  {
      return ImmutableList.of(root);
    }
    List<File> ret = new ArrayList<>();
    Stack<File> stack = new Stack<File>();
    stack.push(root);
    while(!stack.isEmpty()) {
      File f = stack.pop();
      if(f.isDirectory()) {
        for(File child : f.listFiles()) {
          stack.push(child);
        }
      }
      else {
        ret.add(f);
      }
    }
    return ret;
  }

  public HTableProvider getProvider() {
    return new HTableProvider();
  }

  public List<Put> extract( String line
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

  public void load( final Iterable<Stream<String>> streams
                  , final ThreadLocal<ExtractorState> state
                  , final String cf
                  , int numThreads
                  )
  {
    System.out.println("Number of threads: " + numThreads);
    for(Stream<String> stream : streams) {
      try {
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
      } catch (InterruptedException e) {
        throw new IllegalStateException(e.getMessage(), e);
      } catch (ExecutionException e) {
        throw new IllegalStateException(e.getMessage(), e);
      } finally {
        stream.close();
      }
    }
  }

  private static Iterable<Stream<String>> streamify(List<File> files, int batchSize, boolean lineByLine) throws FileNotFoundException {
    List<Stream<String>> ret = new ArrayList<>();
    if(!lineByLine) {
      ret.add(files.stream().map(f -> {
        try {
          return FileUtils.readFileToString(f);
        } catch (IOException e) {
          throw new IllegalStateException("File " + f.getName() + " not found.");
        }
      }));
    }
    else {
      for(File f : files) {
        ret.add(ReaderSpliterator.lineStream(new BufferedReader(new FileReader(f)), batchSize));
      }
    }
    return ret;
  }

  public static void main(String... argv) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

    CommandLine cli = LoadOptions.parse(new PosixParser(), otherArgs);
    if(LoadOptions.LOG4J_PROPERTIES.has(cli)) {
      PropertyConfigurator.configure(LoadOptions.LOG4J_PROPERTIES.get(cli));
    }
    ExtractorHandler handler = ExtractorHandler.load(
            FileUtils.readFileToString(new File(LoadOptions.EXTRACTOR_CONFIG.get(cli)))
    );
    int batchSize = 128;
    if(LoadOptions.BATCH_SIZE.has(cli)) {
      batchSize = ConversionUtils.convert(LoadOptions.BATCH_SIZE.get(cli), Integer.class);
    }
    int numThreads = Runtime.getRuntime().availableProcessors();
    if(LoadOptions.NUM_THREADS.has(cli)) {
      numThreads = ConversionUtils.convert(LoadOptions.NUM_THREADS.get(cli), Integer.class);
    }
    boolean lineByLine = !handler.getInputFormatHandler().getClass().equals(WholeFileFormat.class);
    SensorEnrichmentUpdateConfig sensorEnrichmentUpdateConfig = null;
    if(LoadOptions.ENRICHMENT_CONFIG.has(cli)) {
      sensorEnrichmentUpdateConfig = JSONUtils.INSTANCE.load( new File(LoadOptions.ENRICHMENT_CONFIG.get(cli))
              , SensorEnrichmentUpdateConfig.class
      );
    }
    List<File> inputFiles = getFiles(new File(LoadOptions.INPUT.get(cli)));
    SimpleEnrichmentFlatFileLoader loader = new SimpleEnrichmentFlatFileLoader();
    ThreadLocal<ExtractorState> state = new ThreadLocal<ExtractorState>() {
      @Override
      protected ExtractorState initialValue() {
        try {
          ExtractorHandler handler = ExtractorHandler.load(
            FileUtils.readFileToString(new File(LoadOptions.EXTRACTOR_CONFIG.get(cli)))
          );
          HTableInterface table = loader.getProvider().getTable(conf, LoadOptions.HBASE_TABLE.get(cli));
          return new ExtractorState(table, handler.getExtractor(), new EnrichmentConverter());
        } catch (IOException e1) {
          throw new IllegalStateException("Unable to get table: " + e1);
        }
      }
    };

    loader.load(streamify(inputFiles, batchSize, lineByLine), state, LoadOptions.HBASE_CF.get(cli), numThreads);

    if(sensorEnrichmentUpdateConfig != null) {
      sensorEnrichmentUpdateConfig.updateSensorConfigs();
    }
  }
}
