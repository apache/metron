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
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.dataloads.extractor.Extractor;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.extractor.inputformat.WholeFileFormat;
import org.apache.metron.enrichment.EnrichmentConfig;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.converters.HbaseConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentConverter;
import org.apache.metron.hbase.converters.enrichment.EnrichmentKey;
import org.apache.metron.hbase.converters.enrichment.EnrichmentValue;
import org.apache.metron.reference.lookup.LookupKV;
import org.apache.metron.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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


  public void loadFile( File inputFile
                      , Extractor extractor
                      , HTableInterface table
                      , String cf
                      , HbaseConverter converter
                      , boolean lineByLine
                      ) throws IOException
  {
    if(!lineByLine) {
      table.put(extract(FileUtils.readFileToString(inputFile), extractor, cf, converter));
    }
    else {
      BufferedReader br = new BufferedReader(new FileReader(inputFile));
      for(String line = null;(line = br.readLine()) != null;) {
        table.put(extract(line, extractor, cf, converter));
      }
    }
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
    boolean lineByLine = !handler.getInputFormatHandler().getClass().equals(WholeFileFormat.class);
    Extractor e = handler.getExtractor();
    EnrichmentConfig enrichmentConfig = null;
    if(LoadOptions.ENRICHMENT_CONFIG.has(cli)) {
      enrichmentConfig = JSONUtils.INSTANCE.load( new File(LoadOptions.ENRICHMENT_CONFIG.get(cli))
              , EnrichmentConfig.class
      );
    }
    HbaseConverter converter = new EnrichmentConverter();
    List<File> inputFiles = getFiles(new File(LoadOptions.INPUT.get(cli)));
    SimpleEnrichmentFlatFileLoader loader = new SimpleEnrichmentFlatFileLoader();
    HTableInterface table = loader.getProvider()
            .getTable(conf, LoadOptions.HBASE_TABLE.get(cli));

    for (File f : inputFiles) {
      loader.loadFile(f, e, table, LoadOptions.HBASE_CF.get(cli), converter, lineByLine);
    }
    if(enrichmentConfig != null) {
      enrichmentConfig.updateSensorConfigs();
    }
  }
}
