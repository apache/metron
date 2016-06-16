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
import backtype.storm.ConfigValidation;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.parsers.topology.config.Arg;
import org.apache.metron.parsers.topology.config.ConfigHandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class ParserTopologyCLI {

  public enum ParserOptions {
    HELP("h", code -> {
      Option o = new Option(code, "help", false, "This screen");
      o.setRequired(false);
      return o;
    }),
    ZK_QUORUM("z", code -> {
      Option o = new Option(code, "zk", true, "Zookeeper Quroum URL (zk1:2181,zk2:2181,...");
      o.setArgName("ZK_QUORUM");
      o.setRequired(true);
      return o;
    }),
    BROKER_URL("k", code -> {
      Option o = new Option(code, "kafka", true, "Kafka Broker URL");
      o.setArgName("BROKER_URL");
      o.setRequired(true);
      return o;
    }),
    SENSOR_TYPE("s", code -> {
      Option o = new Option(code, "sensor", true, "Sensor Type");
      o.setArgName("SENSOR_TYPE");
      o.setRequired(true);
      return o;
    }),
    SPOUT_PARALLELISM("sp", code -> {
      Option o = new Option(code, "spout_p", true, "Spout Parallelism Hint");
      o.setArgName("SPOUT_PARALLELISM_HINT");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    PARSER_PARALLELISM("pp", code -> {
      Option o = new Option(code, "parser_p", true, "Parser Parallelism Hint");
      o.setArgName("PARALLELISM_HINT");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    INVALID_WRITER_PARALLELISM("iwp", code -> {
      Option o = new Option(code, "invalid_writer_p", true, "Invalid Message Writer Parallelism Hint");
      o.setArgName("PARALLELISM_HINT");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    ERROR_WRITER_PARALLELISM("ewp", code -> {
      Option o = new Option(code, "error_writer_p", true, "Error Writer Parallelism Hint");
      o.setArgName("PARALLELISM_HINT");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    SPOUT_NUM_TASKS("snt", code -> {
      Option o = new Option(code, "spout_num_tasks", true, "Spout Num Tasks");
      o.setArgName("NUM_TASKS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    PARSER_NUM_TASKS("pnt", code -> {
      Option o = new Option(code, "parser_num_tasks", true, "Parser Num Tasks");
      o.setArgName("NUM_TASKS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    INVALID_WRITER_NUM_TASKS("iwnt", code -> {
      Option o = new Option(code, "invalid_writer_num_tasks", true, "Invalid Writer Num Tasks");
      o.setArgName("NUM_TASKS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    ERROR_WRITER_NUM_TASKS("ewnt", code -> {
      Option o = new Option(code, "error_writer_num_tasks", true, "Error Writer Num Tasks");
      o.setArgName("NUM_TASKS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }),
    NUM_WORKERS("nw", code -> {
      Option o = new Option(code, "num_workers", true, "Number of Workers");
      o.setArgName("NUM_WORKERS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
      }, new ConfigHandlers.SetNumWorkersHandler()
    )
    ,NUM_ACKERS("na", code -> {
      Option o = new Option(code, "num_ackers", true, "Number of Ackers");
      o.setArgName("NUM_ACKERS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }, new ConfigHandlers.SetNumAckersHandler()
    )
    ,NUM_MAX_TASK_PARALLELISM("mtp", code -> {
      Option o = new Option(code, "max_task_parallelism", true, "Max task parallelism");
      o.setArgName("MAX_TASK");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }, new ConfigHandlers.SetMaxTaskParallelismHandler()
    )
    ,MESSAGE_TIMEOUT("mt", code -> {
      Option o = new Option(code, "message_timeout", true, "Message Timeout in Seconds");
      o.setArgName("TIMEOUT_IN_SECS");
      o.setRequired(false);
      o.setType(Number.class);
      return o;
    }, new ConfigHandlers.SetMessageTimeoutHandler()
    )
    ,EXTRA_OPTIONS("e", code -> {
      Option o = new Option(code, "extra_options", true, "Extra options in the form of a JSON file with a map for content.");
      o.setArgName("JSON_FILE");
      o.setRequired(false);
      o.setType(String.class);
      return o;
    }, new ConfigHandlers.LoadJSONHandler()
    )
    ,TEST("t", code ->
    {
      Option o = new Option("t", "test", true, "Run in Test Mode");
      o.setArgName("TEST");
      o.setRequired(false);
      return o;
    })
    ;
    Option option;
    String shortCode;
    Function<Arg, Config> configHandler;
    ParserOptions(String shortCode
                 , Function<String, Option> optionHandler
                 ) {
      this(shortCode, optionHandler, arg -> arg.getConfig());
                 }
    ParserOptions(String shortCode
                 , Function<String, Option> optionHandler
                 , Function<Arg, Config> configHandler
                 ) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
      this.configHandler = configHandler;
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }
    public String get(CommandLine cli, String def) {
      return has(cli)?cli.getOptionValue(shortCode):def;
    }

    public static Config getConfig(CommandLine cli) {
      Config config = new Config();
      for(ParserOptions option : ParserOptions.values()) {
        config = option.configHandler.apply(new Arg(config, option.get(cli)));
      }
      return config;
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) throws ParseException {
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
        throw e;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "ParserTopologyCLI", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for(ParserOptions o : ParserOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }

  public static void main(String[] args) {
    Options options = new Options();

    try {
      CommandLineParser parser = new PosixParser();
      CommandLine cmd = null;
      try {
        cmd = ParserOptions.parse(parser, args);
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
      String zookeeperUrl = ParserOptions.ZK_QUORUM.get(cmd);;
      String brokerUrl = ParserOptions.BROKER_URL.get(cmd);
      String sensorType= ParserOptions.SENSOR_TYPE.get(cmd);
      int spoutParallelism = Integer.parseInt(ParserOptions.SPOUT_PARALLELISM.get(cmd, "1"));
      int spoutNumTasks = Integer.parseInt(ParserOptions.SPOUT_NUM_TASKS.get(cmd, "1"));
      int parserParallelism = Integer.parseInt(ParserOptions.PARSER_PARALLELISM.get(cmd, "1"));
      int parserNumTasks= Integer.parseInt(ParserOptions.PARSER_NUM_TASKS.get(cmd, "1"));
      int errorParallelism = Integer.parseInt(ParserOptions.ERROR_WRITER_PARALLELISM.get(cmd, "1"));
      int errorNumTasks= Integer.parseInt(ParserOptions.ERROR_WRITER_NUM_TASKS.get(cmd, "1"));
      int invalidParallelism = Integer.parseInt(ParserOptions.INVALID_WRITER_PARALLELISM.get(cmd, "1"));
      int invalidNumTasks= Integer.parseInt(ParserOptions.INVALID_WRITER_NUM_TASKS.get(cmd, "1"));
      SpoutConfig.Offset offset = cmd.hasOption("t") ? SpoutConfig.Offset.BEGINNING : SpoutConfig.Offset.WHERE_I_LEFT_OFF;
      TopologyBuilder builder = ParserTopologyBuilder.build(zookeeperUrl,
              brokerUrl,
              sensorType,
              offset,
              spoutParallelism,
              spoutNumTasks,
              parserParallelism,
              parserNumTasks,
              invalidParallelism,
              invalidNumTasks,
              errorParallelism,
              errorNumTasks
      );
      Config stormConf = ParserOptions.getConfig(cmd);

      if (ParserOptions.TEST.has(cmd)) {
        stormConf.put(Config.TOPOLOGY_DEBUG, true);
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(sensorType, stormConf, builder.createTopology());
        Utils.sleep(300000);
        cluster.shutdown();
      } else {
        StormSubmitter.submitTopology(sensorType, stormConf, builder.createTopology());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
