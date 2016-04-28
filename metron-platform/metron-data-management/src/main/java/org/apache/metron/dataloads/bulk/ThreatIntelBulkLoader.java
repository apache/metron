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
package org.apache.metron.dataloads.bulk;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.hbase.mr.BulkLoadMapper;
import org.apache.metron.common.configuration.EnrichmentConfig;
import org.apache.metron.enrichment.converter.HbaseConverter;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.common.utils.JSONUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.*;
import java.util.Date;

public class ThreatIntelBulkLoader  {
  private static abstract class OptionHandler implements Function<String, Option> {}
  public enum BulkLoadOptions {
    HELP("h", new OptionHandler() {

      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        return new Option(s, "help", false, "Generate Help screen");
      }
    })
    ,TABLE("t", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "table", true, "HBase table to import data into");
        o.setRequired(true);
        o.setArgName("HBASE_TABLE");
        return o;
      }
    })
    ,COLUMN_FAMILY("f", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "column_family", true, "Column family of the HBase table to import into");
        o.setRequired(true);
        o.setArgName("CF_NAME");
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
    ,INPUT_DATA("i", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "input", true, "Input directory in HDFS for the data to import into HBase");
        o.setArgName("DIR");
        o.setRequired(true);
        return o;
      }
    })
    ,AS_OF_TIME("a", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "as_of", true, "The last read timestamp to mark the records with (omit for time of execution)");
        o.setArgName("datetime");
        o.setRequired(false);
        return o;
      }
    })
    ,AS_OF_TIME_FORMAT("z", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "as_of_format", true, "The format of the as_of time (only used in conjunction with the as_of option)");
        o.setArgName("format");
        o.setRequired(false);
        return o;
      }
    })
    ,CONVERTER("c", new OptionHandler() {
      @Nullable
      @Override
      public Option apply(@Nullable String s) {
        Option o = new Option(s, "converter", true, "The HBase converter class to use (Default is threat intel)");
        o.setArgName("class");
        o.setRequired(false);
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
    ;
    Option option;
    String shortCode;
    BulkLoadOptions(String shortCode, OptionHandler optionHandler) {
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
        if(ThreatIntelBulkLoader.BulkLoadOptions.HELP.has(cli)) {
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
      formatter.printHelp( "ThreatIntelBulkLoader", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(BulkLoadOptions o : BulkLoadOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  private static long getTimestamp(CommandLine cli) throws java.text.ParseException {
    if(BulkLoadOptions.AS_OF_TIME.has(cli)) {
      if(!BulkLoadOptions.AS_OF_TIME_FORMAT.has(cli)) {
        throw new IllegalStateException("Unable to proceed: Specified as_of_time without an associated format.");
      }
      else {
        DateFormat format = new SimpleDateFormat(BulkLoadOptions.AS_OF_TIME_FORMAT.get(cli));
        Date d = format.parse(BulkLoadOptions.AS_OF_TIME.get(cli));
        return d.getTime();
      }
    }
    else {
      return System.currentTimeMillis();
    }
  }
  private static String readExtractorConfig(File configFile) throws IOException {
    return Joiner.on("\n").join(Files.readLines(configFile, Charset.defaultCharset()));
  }

  public static Job createJob(Configuration conf, String input, String table, String cf, String extractorConfigContents, long ts, HbaseConverter converter) throws IOException {
    Job job = new Job(conf);
    job.setJobName("ThreatIntelBulkLoader: " + input + " => " +  table + ":" + cf);
    System.out.println("Configuring " + job.getJobName());
    job.setJarByClass(ThreatIntelBulkLoader.class);
    job.setMapperClass(org.apache.metron.dataloads.hbase.mr.BulkLoadMapper.class);
    job.setOutputFormatClass(TableOutputFormat.class);
    job.getConfiguration().set(TableOutputFormat.OUTPUT_TABLE, table);
    job.getConfiguration().set(BulkLoadMapper.COLUMN_FAMILY_KEY, cf);
    job.getConfiguration().set(BulkLoadMapper.CONFIG_KEY, extractorConfigContents);
    job.getConfiguration().set(BulkLoadMapper.LAST_SEEN_KEY, "" + ts);
    job.getConfiguration().set(BulkLoadMapper.CONVERTER_KEY, converter.getClass().getName());
    job.setOutputKeyClass(ImmutableBytesWritable.class);
    job.setOutputValueClass(Put.class);
    job.setNumReduceTasks(0);
    ExtractorHandler handler = ExtractorHandler.load(extractorConfigContents);
    handler.getInputFormatHandler().set(job, new Path(input), handler.getConfig());
    return job;
  }

  public static void main(String... argv) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

    CommandLine cli = BulkLoadOptions.parse(new PosixParser(), otherArgs);
    Long ts = getTimestamp(cli);
    String input = BulkLoadOptions.INPUT_DATA.get(cli);
    String table = BulkLoadOptions.TABLE.get(cli);
    String cf = BulkLoadOptions.COLUMN_FAMILY.get(cli);
    String extractorConfigContents = readExtractorConfig(new File(BulkLoadOptions.EXTRACTOR_CONFIG.get(cli)));
    String converterClass = EnrichmentConverter.class.getName();
    if(BulkLoadOptions.CONVERTER.has(cli)) {
      converterClass = BulkLoadOptions.CONVERTER.get(cli);
    }
    EnrichmentConfig enrichmentConfig = null;
    if(BulkLoadOptions.ENRICHMENT_CONFIG.has(cli)) {
      enrichmentConfig = JSONUtils.INSTANCE.load( new File(BulkLoadOptions.ENRICHMENT_CONFIG.get(cli))
              , EnrichmentConfig.class
      );
    }

    HbaseConverter converter = (HbaseConverter) Class.forName(converterClass).newInstance();
    Job job = createJob(conf, input, table, cf, extractorConfigContents, ts, converter);
    System.out.println(conf);
    boolean jobRet = job.waitForCompletion(true);
    if(!jobRet) {
      System.exit(1);
    }
    if(enrichmentConfig != null) {
        enrichmentConfig.updateSensorConfigs();
    }
    System.exit(0);
  }
}
