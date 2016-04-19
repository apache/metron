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
import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.hbase.mr.PrunerMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LeastRecentlyUsedPruner {
    private static abstract class OptionHandler implements Function<String, Option> {}
    public enum BulkLoadOptions {
        HELP("h", new OptionHandler() {

            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                return new Option(s, "help", false, "Generate Help screen");
            }
        }), TABLE("t", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                Option o = new Option(s, "table", true, "HBase table to prune");
                o.setRequired(true);
                o.setArgName("HBASE_TABLE");
                return o;
            }
        }), COLUMN_FAMILY("f", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                Option o = new Option(s, "column_family", true, "Column family of the HBase table to prune");
                o.setRequired(false);
                o.setArgName("CF_NAME");
                return o;
            }
        })
        ,AS_OF_TIME("a", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                Option o = new Option(s, "as_of", true, "The earliest access tracker you want to use.");
                o.setArgName("datetime");
                o.setRequired(true);
                return o;
            }
        })
        ,AS_OF_TIME_FORMAT("v", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                String defaultFormat = new SimpleDateFormat().toLocalizedPattern();
                Option o = new Option(s, "as_of_format", true, "The format of the as_of time (only used in conjunction with the as_of option) (Default is: " + defaultFormat + ")");
                o.setArgName("format");
                o.setRequired(false);
                return o;
            }
        })
        ,ACCESS_TABLE("u", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                Option o = new Option(s, "access_table", true, "HBase table containing the access trackers.");
                o.setRequired(true);
                o.setArgName("HBASE_TABLE");
                return o;
            }
        }), ACCESS_COLUMN_FAMILY("z", new OptionHandler() {
            @Nullable
            @Override
            public Option apply(@Nullable String s) {
                Option o = new Option(s, "access_column_family", true, "Column family of the HBase table containing the access trackers");
                o.setRequired(true);
                o.setArgName("CF_NAME");
                return o;
            }
        });
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
        private static long getTimestamp(CommandLine cli) throws java.text.ParseException {
            Date d = getFormat(cli).parse(BulkLoadOptions.AS_OF_TIME.get(cli));
            return d.getTime();
        }

        private static DateFormat getFormat(CommandLine cli) {
            DateFormat format = new SimpleDateFormat();
            if (BulkLoadOptions.AS_OF_TIME_FORMAT.has(cli)) {
                 format = new SimpleDateFormat(BulkLoadOptions.AS_OF_TIME_FORMAT.get(cli));
            }
            return format;
        }

        public static CommandLine parse(CommandLineParser parser, String[] args) {
            try {
                CommandLine cli = parser.parse(getOptions(), args);
                if(BulkLoadOptions.HELP.has(cli)) {
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
            formatter.printHelp( "LeastRecentlyUsedPruner", getOptions());
        }

        public static Options getOptions() {
            Options ret = new Options();
            for(BulkLoadOptions o : BulkLoadOptions.values()) {
               ret.addOption(o.option);
            }
            return ret;
        }
    }

    public static void setupHBaseJob(Job job, String sourceTable, String cf) throws IOException {
        Scan scan = new Scan();
        if(cf != null) {
            scan.addFamily(Bytes.toBytes(cf));
        }
        scan.setCaching(500);        // 1 is the default in Scan, which will be bad for MapReduce jobs
        scan.setCacheBlocks(false);  // don't set to true for MR jobs
// set other scan attrs

        TableMapReduceUtil.initTableMapperJob(
                sourceTable,      // input table
                scan,	          // Scan instance to control CF and attribute selection
                PrunerMapper.class,   // mapper class
                null,	          // mapper output key
                null,	          // mapper output value
                job);
        TableMapReduceUtil.initTableReducerJob(
                sourceTable,      // output table
                null,             // reducer class
                job);
    }

    public static Job createJob( Configuration conf
                               , String table
                               , String cf
                               , String accessTrackerTable
                               , String accessTrackerColumnFamily
                               , Long ts
                               ) throws IOException
    {
        Job job = new Job(conf);
        job.setJobName("LeastRecentlyUsedPruner: Pruning " +  table + ":" + cf + " since " + new SimpleDateFormat().format(new Date(ts)));
        System.out.println("Configuring " + job.getJobName());
        job.setJarByClass(LeastRecentlyUsedPruner.class);
        job.getConfiguration().setLong(PrunerMapper.TIMESTAMP_CONF, ts);
        job.getConfiguration().set(PrunerMapper.ACCESS_TRACKER_NAME_CONF, table);
        job.getConfiguration().set(PrunerMapper.ACCESS_TRACKER_CF_CONF, accessTrackerColumnFamily);
        job.getConfiguration().set(PrunerMapper.ACCESS_TRACKER_TABLE_CONF, accessTrackerTable);
        setupHBaseJob(job, table, cf);
        job.setNumReduceTasks(0);
        return job;
    }

    public static void main(String... argv) throws IOException, java.text.ParseException, ClassNotFoundException, InterruptedException {
        Configuration conf = HBaseConfiguration.create();
        String[] otherArgs = new GenericOptionsParser(conf, argv).getRemainingArgs();

        CommandLine cli = BulkLoadOptions.parse(new PosixParser(), otherArgs);
        Long ts = BulkLoadOptions.getTimestamp(cli);
        String table = BulkLoadOptions.TABLE.get(cli);
        String cf = BulkLoadOptions.COLUMN_FAMILY.get(cli);
        String accessTrackerTable = BulkLoadOptions.ACCESS_TABLE.get(cli);
        String accessTrackerCF = BulkLoadOptions.ACCESS_COLUMN_FAMILY.get(cli);
        Job job = createJob(conf, table, cf, accessTrackerTable, accessTrackerCF, ts);
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
