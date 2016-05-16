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
package org.apache.metron.parsers.topology;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.metron.common.spout.kafka.SpoutConfig;

import java.util.HashMap;
import java.util.Map;

public class ParserTopologyCLI {

  public static void main(String[] args) {
    Options options = new Options();
    {
      Option o = new Option("h", "help", false, "This screen");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("z", "zk", true, "Zookeeper Quroum URL (zk1:2181,zk2:2181,...");
      o.setArgName("ZK_QUORUM");
      o.setRequired(true);
      options.addOption(o);
    }
    {
      Option o = new Option("k", "kafka", true, "Kafka Broker URL");
      o.setArgName("BROKER_URL");
      o.setRequired(true);
      options.addOption(o);
    }
    {
      Option o = new Option("s", "sensor", true, "Sensor Type");
      o.setArgName("SENSOR_TYPE");
      o.setRequired(true);
      options.addOption(o);
    }
    {
      Option o = new Option("sp", "spout_p", true, "Spout Parallelism");
      o.setArgName("SPOUT_PARALLELISM");
      o.setRequired(false);
      o.setType(Number.class);
      options.addOption(o);
    }
    {
      Option o = new Option("pp", "parser_p", true, "Parser Parallelism");
      o.setArgName("PARSER_PARALLELISM");
      o.setRequired(false);
      o.setType(Number.class);
      options.addOption(o);
    }
    {
      Option o = new Option("t", "test", true, "Run in Test Mode");
      o.setArgName("TEST");
      o.setRequired(false);
      options.addOption(o);
    }
    try {
      CommandLineParser parser = new PosixParser();
      CommandLine cmd = null;
      try {
        cmd = parser.parse(options, args);
      } catch (ParseException pe) {
        pe.printStackTrace();
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("ParserTopologyCLI", null, options, null, true);
        System.exit(-1);
      }
      if (cmd.hasOption("h")) {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("ParserTopologyCLI", null, options, null, true);
        System.exit(0);
      }
      String zookeeperUrl = cmd.getOptionValue("z");
      String brokerUrl = cmd.getOptionValue("k");
      String sensoryType = cmd.getOptionValue("s");
      int spoutParallelism = Integer.parseInt(cmd.getOptionValue("sp", "1"));
      int parserParallelism = Integer.parseInt(cmd.getOptionValue("pp", "1"));
      SpoutConfig.Offset offset = cmd.hasOption("t") ? SpoutConfig.Offset.BEGINNING : SpoutConfig.Offset.WHERE_I_LEFT_OFF;
      TopologyBuilder builder = ParserTopologyBuilder.build(zookeeperUrl,
              brokerUrl,
              sensoryType,
              offset,
              spoutParallelism,
              parserParallelism);
      if (cmd.hasOption("t")) {
        Map<String, Object> stormConf = new HashMap<>();
        stormConf.put(Config.TOPOLOGY_DEBUG, true);
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(sensoryType, stormConf, builder.createTopology());
        Utils.sleep(300000);
        cluster.shutdown();
      } else {
        StormSubmitter.submitTopology(sensoryType, new HashMap<>(), builder.createTopology());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
