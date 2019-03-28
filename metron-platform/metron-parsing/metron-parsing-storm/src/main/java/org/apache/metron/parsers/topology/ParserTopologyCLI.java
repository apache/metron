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

import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.parsers.topology.config.Arg;
import org.apache.metron.parsers.topology.config.ConfigHandlers;
import org.apache.metron.parsers.topology.config.ValueSupplier;
import org.apache.metron.storm.kafka.flux.SpoutConfiguration;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.utils.Utils;

public class ParserTopologyCLI {

  public static final String STORM_JOB_SEPARATOR = "__";
  public static final String TOPOLOGY_OPTION_SEPARATOR = ",";

  public enum ParserOptions {
    HELP("h", code -> {
      Option o = new Option(code, "help", false, "This screen");
      o.setRequired(false);
      return o;
    }),
    ZK_QUORUM("z", code -> {
      Option o = new Option(code, "zk", true, "Zookeeper Quorum URL (zk1:2181,zk2:2181,...");
      o.setArgName("ZK_QUORUM");
      o.setRequired(true);
      return o;
    }),
    BROKER_URL("k", code -> {
      Option o = new Option(code, "kafka", true, "Kafka Broker URL");
      o.setArgName("BROKER_URL");
      o.setRequired(false);
      return o;
    }),
    SENSOR_TYPES("s", code -> {
      Option o = new Option(code, "sensor", true, "Sensor Types as comma-separated list");
      o.setArgName("SENSOR_TYPES");
      o.setRequired(true);
      return o;
    }),
    SPOUT_PARALLELISM("sp", code -> {
      Option o = new Option(code, "spout_p", true, "Spout Parallelism Hint. If multiple sensors are specified, this should be a comma separated list in the same order.");
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
      Option o = new Option(code, "spout_num_tasks", true, "Spout Num Tasks. If multiple sensors are specified, this should be a comma separated list in the same order.");
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
      Option o = new Option(code, "extra_topology_options", true
                           , "Extra options in the form of a JSON file with a map for content." +
                             "  Available options are those in the Kafka Consumer Configs at http://kafka.apache.org/0100/documentation.html#newconsumerconfigs" +
                             " and " + Joiner.on(TOPOLOGY_OPTION_SEPARATOR).join(SpoutConfiguration.allOptions())
                           );
      o.setArgName("JSON_FILE");
      o.setRequired(false);
      o.setType(String.class);
      return o;
    }, new ConfigHandlers.LoadJSONHandler()
    )
    ,SPOUT_CONFIG("esc", code -> {
      Option o = new Option(code
                           , "extra_kafka_spout_config"
                           , true
                           , "Extra spout config options in the form of a JSON file with a map for content."
                           );
      o.setArgName("JSON_FILE");
      o.setRequired(false);
      o.setType(String.class);
      return o;
    }
    )
    ,SECURITY_PROTOCOL("ksp", code -> {
      Option o = new Option(code
                           , "kafka_security_protocol"
                           , true
                           , "The kafka security protocol to use (if running with a kerberized cluster).  E.g. PLAINTEXTSASL"
                           );
      o.setArgName("SECURITY_PROTOCOL");
      o.setRequired(false);
      o.setType(String.class);
      return o;
    }
    )
    ,OUTPUT_TOPIC("ot", code -> {
      Option o = new Option(code
                           , "output_topic"
                           , true
                           , "The output kafka topic for the parser.  If unset, the default is " + Constants.ENRICHMENT_TOPIC
                           );
      o.setArgName("KAFKA_TOPIC");
      o.setRequired(false);
      o.setType(String.class);
      return o;
    }
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

    public static Optional<Config> getConfig(CommandLine cli) {
      return getConfig(cli, new Config());
    }

    public static Optional<Config> getConfig(CommandLine cli, Config config) {
      if(EXTRA_OPTIONS.has(cli)) {
        Map<String, Object> extraOptions = readJSONMapFromFile(new File(EXTRA_OPTIONS.get(cli)));
        config.putAll(extraOptions);
      }
      for(ParserOptions option : ParserOptions.values()) {
        config = option.configHandler.apply(new Arg(config, option.get(cli)));
      }
      return config.isEmpty()?Optional.empty():Optional.of(config);
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

  private static CommandLine parse(Options options, String[] args) {
    /*
     * The general gist is that in order to pass args to storm jar,
     * we have to disregard options that we don't know about in the CLI.
     * Storm will ignore our args, we have to do the same.
     */
    CommandLineParser parser = new PosixParser() {
      @Override
      protected void processOption(String arg, ListIterator iter) throws ParseException {
        if(getOptions().hasOption(arg)) {
          super.processOption(arg, iter);
        }
      }
    };
    try {
      return ParserOptions.parse(parser, args);
    } catch (ParseException pe) {
      pe.printStackTrace();
      final HelpFormatter usageFormatter = new HelpFormatter();
      usageFormatter.printHelp("ParserTopologyCLI", null, options, null, true);
      System.exit(-1);
      return null;
    }
  }

  public ParserTopologyBuilder.ParserTopology createParserTopology(final CommandLine cmd) throws Exception {
    String zookeeperUrl = ParserOptions.ZK_QUORUM.get(cmd);
    Optional<String> brokerUrl = ParserOptions.BROKER_URL.has(cmd)?Optional.of(ParserOptions.BROKER_URL.get(cmd)):Optional.empty();
    String sensorTypeRaw= ParserOptions.SENSOR_TYPES.get(cmd);
    List<String> sensorTypes = Arrays.stream(sensorTypeRaw.split(TOPOLOGY_OPTION_SEPARATOR)).map(String::trim).collect(
        Collectors.toList());

    /*
     * It bears mentioning why we're creating this ValueSupplier indirection here.
     * As a separation of responsibilities, the CLI class defines the order of precedence
     * for the various topological and structural properties for creating a parser.  This is
     * desirable because there are now (i.e. integration tests)
     * and may be in the future (i.e. a REST service to start parsers without using the CLI)
     * other mechanisms to construct parser topologies.  It's sensible to split those concerns..
     *
     * Unfortunately, determining the structural parameters for a parser requires interacting with
     * external services (e.g. zookeeper) that are set up well within the ParserTopology class.
     * Rather than pulling the infrastructure to interact with those services out and moving it into the
     * CLI class and breaking that separation of concerns, we've created a supplier
     * indirection where are providing the logic as to how to create precedence in the CLI class
     * without owning the responsibility of constructing the infrastructure where the values are
     * necessarily supplied.
     *
     */

    // kafka spout parallelism
    ValueSupplier<List> spoutParallelism = (parserConfigs, clazz) -> {
      if(ParserOptions.SPOUT_PARALLELISM.has(cmd)) {
        // Handle the case where there's only one and we can default reasonably
        if( parserConfigs.size() == 1) {
          return Collections.singletonList(Integer.parseInt(
              ParserOptions.SPOUT_PARALLELISM.get(cmd, "1")));
        }

        // Handle the multiple explicitly passed spout parallelism's case.
        String parallelismRaw = ParserOptions.SPOUT_PARALLELISM.get(cmd, "1");
        List<String> parallelisms = Arrays.stream(parallelismRaw.split(TOPOLOGY_OPTION_SEPARATOR)).map(String::trim).collect(
            Collectors.toList());
        if (parallelisms.size() != parserConfigs.size()) {
          throw new IllegalArgumentException("Spout parallelism should match number of sensors 1:1");
        }
        List<Integer> spoutParallelisms = new ArrayList<>();
        for (String s : parallelisms) {
          spoutParallelisms.add(Integer.parseInt(s));
        }
        return spoutParallelisms;
      }

      List<Integer> spoutParallelisms = new ArrayList<>();
      for (SensorParserConfig parserConfig : parserConfigs) {
        spoutParallelisms.add(parserConfig.getSpoutParallelism());
      }
      return spoutParallelisms;
    };

    // kafka spout number of tasks
    ValueSupplier<List> spoutNumTasks = (parserConfigs, clazz) -> {
      if(ParserOptions.SPOUT_NUM_TASKS.has(cmd)) {
        // Handle the case where there's only one and we can default reasonably
        if( parserConfigs.size() == 1) {
          return Collections.singletonList(Integer.parseInt(
              ParserOptions.SPOUT_NUM_TASKS.get(cmd, "1")));
        }

        // Handle the multiple explicitly passed spout parallelism's case.
        String numTasksRaw = ParserOptions.SPOUT_NUM_TASKS.get(cmd, "1");
        List<String> numTasks = Arrays.stream(numTasksRaw.split(TOPOLOGY_OPTION_SEPARATOR)).map(String::trim).collect(
            Collectors.toList());
        if (numTasks.size() != parserConfigs.size()) {
          throw new IllegalArgumentException("Spout num tasks should match number of sensors 1:1");
        }
        List<Integer> spoutTasksList = new ArrayList<>();
        for (String s : numTasks) {
          spoutTasksList.add(Integer.parseInt(s));
        }
        return spoutTasksList;
      }

      List<Integer> numTasks = new ArrayList<>();
      for (SensorParserConfig parserConfig : parserConfigs) {
        numTasks.add(parserConfig.getSpoutNumTasks());
      }
      return numTasks;
    };

    // parser bolt parallelism
    ValueSupplier<Integer> parserParallelism = (parserConfigs, clazz) -> {
      if(ParserOptions.PARSER_PARALLELISM.has(cmd)) {
        return Integer.parseInt(ParserOptions.PARSER_PARALLELISM.get(cmd, "1"));
      }
      int retValue = 1;
      for (SensorParserConfig config : parserConfigs) {
        Integer configValue = config.getParserParallelism();
        retValue = configValue == null ? retValue : configValue;
      }
      return retValue;
    };

    // parser bolt number of tasks
    ValueSupplier<Integer> parserNumTasks = (parserConfigs, clazz) -> {
      if(ParserOptions.PARSER_NUM_TASKS.has(cmd)) {
        return Integer.parseInt(ParserOptions.PARSER_NUM_TASKS.get(cmd, "1"));
      }
      int retValue = 1;
      for (SensorParserConfig config : parserConfigs) {
        Integer configValue = config.getParserNumTasks();
        retValue = configValue == null ? retValue : configValue;
      }
      return retValue;
    };

    // error bolt parallelism
    ValueSupplier<Integer> errorParallelism = (parserConfigs, clazz) -> {
      if(ParserOptions.ERROR_WRITER_PARALLELISM.has(cmd)) {
        return Integer.parseInt(ParserOptions.ERROR_WRITER_PARALLELISM.get(cmd, "1"));
      }
      int retValue = 1;
      for (SensorParserConfig config : parserConfigs) {
        Integer configValue = config.getErrorWriterParallelism();
        retValue = configValue == null ? retValue : configValue;
      }
      return retValue;
    };

    // error bolt number of tasks
    ValueSupplier<Integer> errorNumTasks = (parserConfigs, clazz) -> {
      if(ParserOptions.ERROR_WRITER_NUM_TASKS.has(cmd)) {
        return Integer.parseInt(ParserOptions.ERROR_WRITER_NUM_TASKS.get(cmd, "1"));
      }
      int retValue = 1;
      for (SensorParserConfig config : parserConfigs) {
        Integer configValue = config.getErrorWriterNumTasks();
        retValue = configValue == null ? retValue : configValue;
      }
      return retValue;
    };

    // kafka spout config
    ValueSupplier<List> spoutConfig = (parserConfigs, clazz) -> {
      if(ParserOptions.SPOUT_CONFIG.has(cmd)) {
        return Collections.singletonList(readJSONMapFromFile(new File(ParserOptions.SPOUT_CONFIG.get(cmd))));
      }
      List<Map<String, Object>> retValue = new ArrayList<>();
      for (SensorParserConfig config : parserConfigs) {
        retValue.add(config.getSpoutConfig());
      }
      return retValue;
    };

    // security protocol
    ValueSupplier<String> securityProtocol = (parserConfigs, clazz) -> {
      Optional<String> sp = Optional.empty();
      if (ParserOptions.SECURITY_PROTOCOL.has(cmd)) {
        sp = Optional.of(ParserOptions.SECURITY_PROTOCOL.get(cmd));
      }
      // Need to adjust to handle list of spoutConfigs. Any non-plaintext wins
      if (!sp.isPresent()) {
        sp = getSecurityProtocol(sp, spoutConfig.get(parserConfigs, List.class));
      }
      // Need to look through parserConfigs for any non-plaintext
      String parserConfigSp = SecurityProtocol.PLAINTEXT.name;
      for (SensorParserConfig config : parserConfigs) {
        String configSp = config.getSecurityProtocol();
        if (!SecurityProtocol.PLAINTEXT.name.equals(configSp)) {
          // We have a winner
          parserConfigSp = configSp;
        }
      }

      return sp.orElse(Optional.ofNullable(parserConfigSp).orElse(null));
    };

    // storm configuration
    ValueSupplier<Config> stormConf = (parserConfigs, clazz) -> {
      // Last one wins
      Config finalConfig = new Config();
      for (SensorParserConfig parserConfig : parserConfigs) {
        Map<String, Object> c = parserConfig.getStormConfig();
        if (c != null && !c.isEmpty()) {
          finalConfig.putAll(c);
        }
        if (parserConfig.getNumAckers() != null) {
          Config.setNumAckers(finalConfig, parserConfig.getNumAckers());
        }
        if (parserConfig.getNumWorkers() != null) {
          Config.setNumWorkers(finalConfig, parserConfig.getNumWorkers());
        }
      }
      return ParserOptions.getConfig(cmd, finalConfig).orElse(finalConfig);
    };

    // output topic
    ValueSupplier<String> outputTopic = (parserConfigs, clazz) -> {
      String topic = null;

      if(ParserOptions.OUTPUT_TOPIC.has(cmd)) {
        topic = ParserOptions.OUTPUT_TOPIC.get(cmd);
      }

      return topic;
    };

    // Error topic will throw an exception if the topics aren't all the same.
    ValueSupplier<String> errorTopic = (parserConfigs, clazz) -> {
      // topic will to set to the 'parser.error.topic' setting in globals when the error bolt is created
      String topic = null;
      for (SensorParserConfig parserConfig : parserConfigs) {
        String currentTopic = parserConfig.getErrorTopic();
        if(topic != null && !topic.equals(currentTopic)) {
          throw new IllegalArgumentException(
              "Parser Aggregation specified with differing error topics");
        }
        topic = currentTopic;
      }

      return topic;
    };

    return getParserTopology(
            zookeeperUrl,
            brokerUrl,
            sensorTypes,
            spoutParallelism,
            spoutNumTasks,
            parserParallelism,
            parserNumTasks,
            errorParallelism,
            errorNumTasks,
            spoutConfig,
            securityProtocol,
            stormConf,
            outputTopic,
            errorTopic);
  }

  protected ParserTopologyBuilder.ParserTopology getParserTopology( String zookeeperUrl,
                                                                    Optional<String> brokerUrl,
                                                                    List<String> sensorTypes,
                                                                    ValueSupplier<List> spoutParallelism,
                                                                    ValueSupplier<List> spoutNumTasks,
                                                                    ValueSupplier<Integer> parserParallelism,
                                                                    ValueSupplier<Integer> parserNumTasks,
                                                                    ValueSupplier<Integer> errorParallelism,
                                                                    ValueSupplier<Integer> errorNumTasks,
                                                                    ValueSupplier<List> spoutConfig,
                                                                    ValueSupplier<String> securityProtocol,
                                                                    ValueSupplier<Config> stormConf,
                                                                    ValueSupplier<String> outputTopic,
                                                                    ValueSupplier<String> errorTopic) throws Exception {
    return ParserTopologyBuilder.build(
            zookeeperUrl,
            brokerUrl,
            sensorTypes,
            spoutParallelism,
            spoutNumTasks,
            parserParallelism,
            parserNumTasks,
            errorParallelism,
            errorNumTasks,
            spoutConfig,
            securityProtocol,
            outputTopic,
            errorTopic,
            stormConf
        );
  }


  public static void main(String[] args) {

    try {
      Options options = new Options();
      final CommandLine cmd = parse(options, args);
      if (cmd.hasOption("h")) {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("ParserTopologyCLI", null, options, null, true);
        System.exit(0);
      }
      ParserTopologyCLI cli = new ParserTopologyCLI();
      ParserTopologyBuilder.ParserTopology topology = cli.createParserTopology(cmd);
      String sensorTypes = ParserOptions.SENSOR_TYPES.get(cmd);
      String topologyName = sensorTypes.replaceAll(TOPOLOGY_OPTION_SEPARATOR, STORM_JOB_SEPARATOR);
      if (ParserOptions.TEST.has(cmd)) {
        topology.getTopologyConfig().put(Config.TOPOLOGY_DEBUG, true);
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(topologyName, topology.getTopologyConfig(), topology.getBuilder().createTopology());
        Utils.sleep(300000);
        cluster.shutdown();
      } else {
        StormSubmitter.submitTopology(topologyName, topology.getTopologyConfig(), topology.getBuilder().createTopology());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static Optional<String> getSecurityProtocol(Optional<String> protocol, List<Map<String, Object>> spoutConfig) {
    Optional<String> ret = protocol;
    if(ret.isPresent() && protocol.get().equalsIgnoreCase(SecurityProtocol.PLAINTEXT.name)) {
      ret = Optional.empty();
    }
    if(!ret.isPresent()) {
      // Need to look through spoutConfig for any non-plaintext
      String spoutConfigSp = null;
      for (Map<String, Object> config: spoutConfig) {
        String configSp = (String) config.get(KafkaUtils.SECURITY_PROTOCOL);
        if (configSp != null && !SecurityProtocol.PLAINTEXT.name.equals(configSp)) {
          // We have a winner
          spoutConfigSp = configSp;
        } else if (configSp != null) {
          // Use something explicitly defined.
          spoutConfigSp = configSp;
        }
      }
      ret = Optional.ofNullable(spoutConfigSp);
    }
    if(ret.isPresent() && ret.get().equalsIgnoreCase(SecurityProtocol.PLAINTEXT.name)) {
      ret = Optional.empty();
    }
    return ret;
  }

  private static Map<String, Object> readJSONMapFromFile(File inputFile) {
    String json = null;
    if (inputFile.exists()) {
      try {
        json = FileUtils.readFileToString(inputFile);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to process JSON file " + inputFile, e);
      }
    }
    else {
      throw new IllegalArgumentException("Unable to load JSON file at " + inputFile.getAbsolutePath());
    }
    try {
      return JSONUtils.INSTANCE.load(json, JSONUtils.MAP_SUPPLIER);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to process JSON.", e);
    }
  }
}
