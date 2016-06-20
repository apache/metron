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

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.timestamp.TimestampConverters;
import org.apache.metron.pcap.filter.fixed.FixedPcapFilter;
import org.apache.metron.pcap.filter.query.QueryPcapFilter;
import org.apache.metron.pcap.mr.PcapJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PcapCli {
  private static final Logger LOGGER = LoggerFactory.getLogger(PcapCli.class);
  private final PcapJob jobRunner;
  private final ResultsWriter resultsWriter;
  private final Clock clock;

  public static void main(String[] args) {
    int status = new PcapCli(new PcapJob(), new ResultsWriter(), new Clock()).run(args);
    System.exit(status);
  }

  public PcapCli(PcapJob jobRunner, ResultsWriter resultsWriter, Clock clock) {
    this.jobRunner = jobRunner;
    this.resultsWriter = resultsWriter;
    this.clock = clock;
  }

  public int run(String[] args) {
    if (args.length < 1) {
      printBasicHelp();
      return -1;
    }
    String jobType = args[0];
    List<byte[]> results = new ArrayList<>();
    String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
    Configuration hadoopConf = new Configuration();
    String[] otherArgs = null;
    try {
      otherArgs = new GenericOptionsParser(hadoopConf, commandArgs).getRemainingArgs();
    } catch (IOException e) {
      LOGGER.error("Failed to configure hadoop with provided options: " + e.getMessage(), e);
      return -1;
    }
    if ("fixed".equals(jobType)) {
      FixedCliParser fixedParser = new FixedCliParser();
      FixedCliConfig config = null;
      try {
        config = fixedParser.parse(otherArgs);
      } catch (ParseException | java.text.ParseException e) {
        System.err.println(e.getMessage());
        fixedParser.printHelp();
        return -1;
      }
      if (config.showHelp()) {
        fixedParser.printHelp();
        return 0;
      }
      Pair<Long, Long> time = timeAsNanosecondsSinceEpoch(config.getStartTime(), config.getEndTime());
      long startTime = time.getLeft();
      long endTime = time.getRight();

      try {
        results = jobRunner.query(
                new Path(config.getBasePath()),
                new Path(config.getBaseOutputPath()),
                startTime,
                endTime,
                config.getFixedFields(),
                hadoopConf,
                FileSystem.get(hadoopConf),
                new FixedPcapFilter.Configurator());
      } catch (IOException | ClassNotFoundException e) {
        LOGGER.error("Failed to execute fixed filter job: " + e.getMessage(), e);
        return -1;
      } catch (InterruptedException e) {
        LOGGER.error("Failed to execute fixed filter job: " + e.getMessage(), e);
        return -1;
      }
    } else if ("query".equals(jobType)) {
      QueryCliParser queryParser = new QueryCliParser();
      QueryCliConfig config = null;
      try {
        config = queryParser.parse(otherArgs);
      } catch (ParseException | java.text.ParseException e) {
        System.err.println(e.getMessage());
        queryParser.printHelp();
        return -1;
      }
      if (config.showHelp()) {
        queryParser.printHelp();
        return 0;
      }
      Pair<Long, Long> time = timeAsNanosecondsSinceEpoch(config.getStartTime(), config.getEndTime());
      long startTime = time.getLeft();
      long endTime = time.getRight();

      try {
        results = jobRunner.query(
                new Path(config.getBasePath()),
                new Path(config.getBaseOutputPath()),
                startTime,
                endTime,
                config.getQuery(),
                hadoopConf,
                FileSystem.get(hadoopConf),
                new QueryPcapFilter.Configurator());
      } catch (IOException | ClassNotFoundException e) {
        LOGGER.error("Failed to execute query filter job: " + e.getMessage(), e);
        return -1;
      } catch (InterruptedException e) {
        LOGGER.error("Failed to execute query filter job: " + e.getMessage(), e);
        return -1;
      }
    } else {
      printBasicHelp();
      return -1;
    }
    String timestamp = clock.currentTimeFormatted("yyyyMMddHHmmssSSSZ");
    String outFileName = String.format("pcap-data-%s.pcap", timestamp);
    try {
      resultsWriter.write(results, outFileName);
    } catch (IOException e) {
      LOGGER.error("Unable to write file", e);
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
