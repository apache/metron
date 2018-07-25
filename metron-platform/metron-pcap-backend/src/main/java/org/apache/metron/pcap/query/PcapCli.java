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
package org.apache.metron.pcap.query;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.UUID;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.job.JobException;
import org.apache.metron.job.Pageable;
import org.apache.metron.pcap.config.PcapOptions;
import org.apache.metron.pcap.config.FixedPcapConfig;
import org.apache.metron.pcap.config.PcapConfig;
import org.apache.metron.pcap.config.QueryPcapConfig;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.finalizer.PcapFinalizerStrategies;
import org.apache.metron.pcap.mr.PcapJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcapCli {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final PcapConfig.PrefixStrategy PREFIX_STRATEGY = clock -> {
    String timestamp = clock.currentTimeFormatted("yyyyMMddHHmm");
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");
    return String.format("%s-%s", timestamp, uuid);
  };
  private final PcapJob jobRunner;
  private final PcapConfig.PrefixStrategy prefixStrategy;

  public static void main(String[] args) {
    int status = new PcapCli(new PcapJob(), PREFIX_STRATEGY).run(args);
    System.exit(status);
  }

  public PcapCli(PcapJob jobRunner, PcapConfig.PrefixStrategy prefixStrategy) {
    this.jobRunner = jobRunner;
    this.prefixStrategy = prefixStrategy;
  }


  public int run(String[] args) {
    if (args.length < 1) {
      printBasicHelp();
      return -1;
    }
    String jobType = args[0];
    String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
    Configuration hadoopConf = new Configuration();
    String[] otherArgs = null;
    try {
      otherArgs = new GenericOptionsParser(hadoopConf, commandArgs).getRemainingArgs();
    } catch (IOException e) {
      LOGGER.error("Failed to configure hadoop with provided options: {}", e.getMessage(), e);
      return -1;
    }
    PcapConfig commonConfig = null;
    Pageable<Path> results;
    // write to local FS in the executing directory
    String execDir = System.getProperty("user.dir");

    if ("fixed".equals(jobType)) {
      FixedCliParser fixedParser = new FixedCliParser(prefixStrategy);
      FixedPcapConfig config = null;
      try {
        config = fixedParser.parse(otherArgs);
        commonConfig = config;
      } catch (ParseException | java.text.ParseException e) {
        System.err.println(e.getMessage());
        System.err.flush();
        fixedParser.printHelp();
        return -1;
      }
      if (config.showHelp()) {
        fixedParser.printHelp();
        return 0;
      }
      PcapOptions.FILTER_IMPL.put(commonConfig, new FixedPcapFilter.Configurator());
      PcapOptions.HADOOP_CONF.put(commonConfig, hadoopConf);
      try {
        PcapOptions.FILESYSTEM.put(commonConfig, FileSystem.get(hadoopConf));
        results = jobRunner.submit(PcapFinalizerStrategies.CLI, commonConfig).get();
      } catch (IOException|InterruptedException | JobException e) {
        LOGGER.error("Failed to execute fixed filter job: {}", e.getMessage(), e);
        return -1;
      }
    } else if ("query".equals(jobType)) {
      QueryCliParser queryParser = new QueryCliParser(prefixStrategy);
      QueryPcapConfig config = null;
      try {
        config = queryParser.parse(otherArgs);
        commonConfig = config;
      } catch (ParseException | java.text.ParseException e) {
        System.err.println(e.getMessage());
        queryParser.printHelp();
        return -1;
      }
      if (config.showHelp()) {
        queryParser.printHelp();
        return 0;
      }
      PcapOptions.FILTER_IMPL.put(commonConfig, new FixedPcapFilter.Configurator());
      PcapOptions.HADOOP_CONF.put(commonConfig, hadoopConf);
      try {
        PcapOptions.FILESYSTEM.put(commonConfig, FileSystem.get(hadoopConf));
        results = jobRunner.submit(PcapFinalizerStrategies.CLI, commonConfig).get();
      } catch (IOException| InterruptedException | JobException e) {
        LOGGER.error("Failed to execute fixed filter job: {}", e.getMessage(), e);
        return -1;
      }
    } else {
      printBasicHelp();
      return -1;
    }

    return 0;
  }

  private Pair<Long, Long> timeAsNanosecondsSinceEpoch(long start, long end) {
    long revisedStart = start;
    if (revisedStart < 0) {
      revisedStart = 0L;
    }
    long revisedEnd = end;
    if (revisedEnd < 0) {
      revisedEnd = System.currentTimeMillis();
    }
    //convert to nanoseconds since the epoch
    revisedStart = TimestampConverters.MILLISECONDS.toNanoseconds(revisedStart);
    revisedEnd = TimestampConverters.MILLISECONDS.toNanoseconds(revisedEnd);
    return Pair.of(revisedStart, revisedEnd);
  }

  public void printBasicHelp() {
    System.out.println("Usage: [fixed|query]");
  }

}
