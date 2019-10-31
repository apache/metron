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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.topology.config.ValueSupplier;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.Config;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTopologyCLITest {


  public static class CLIBuilder {
    EnumMap<ParserTopologyCLI.ParserOptions, String> map = new EnumMap<>(ParserTopologyCLI.ParserOptions.class);

    public CLIBuilder with(ParserTopologyCLI.ParserOptions option, String val) {
      map.put(option, val);
      return this;
    }
    public CLIBuilder with(ParserTopologyCLI.ParserOptions option) {
      map.put(option, null);
      return this;
    }
    public CommandLine build(boolean longOpt) throws ParseException {
      return getCLI(map, longOpt);
    }
    private CommandLine getCLI(EnumMap<ParserTopologyCLI.ParserOptions, String> options, boolean longOpt) throws ParseException {
      ArrayList<String> args = new ArrayList<>();
      for (Map.Entry<ParserTopologyCLI.ParserOptions, String> option : options.entrySet()) {
        boolean hasLongOpt = option.getKey().option.hasLongOpt();
        if (hasLongOpt && longOpt) {
          args.add("--" + option.getKey().option.getLongOpt());
          if (option.getKey().option.hasArg() && option.getValue() != null) {
            args.add(option.getValue());
          }
        } else if (hasLongOpt && !longOpt) {
          args.add("-" + option.getKey().shortCode);
          if (option.getKey().option.hasArg() && option.getValue() != null) {
            args.add(option.getValue());
          }
        }
      }
      return ParserTopologyCLI.ParserOptions.parse(new PosixParser(), args.toArray(new String[args.size()]));
    }
  }

  @Test
  public void testNoOverlappingArgs() throws Exception {
    Set<String> optionStrs = new HashSet<>();
    for(ParserTopologyCLI.ParserOptions option : ParserTopologyCLI.ParserOptions.values()) {
      if(optionStrs.contains(option.option.getLongOpt())) {
        throw new IllegalStateException("Reused long option: " + option.option.getLongOpt());
      }
      if(optionStrs.contains(option.shortCode)) {
        throw new IllegalStateException("Reused short option: " + option.shortCode);
      }
      optionStrs.add(option.option.getLongOpt());
      optionStrs.add(option.shortCode);
    }
  }

  @Test
  public void testKafkaOffset_happyPath() throws ParseException {
    kafkaOffset(true);
    kafkaOffset(false);
  }
  public void kafkaOffset(boolean longOpt) throws ParseException {
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor")
                                      .build(longOpt);
    assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPES.get(cli));
  }
  @Test
  public void testCLI_happyPath() throws ParseException {
    happyPath(true);
    happyPath(false);
  }

  @Test
  public void testCLI_insufficientArg() {
    UnitTestHelper.setLog4jLevel(Parser.class, Level.FATAL);
      assertThrows(ParseException.class, () ->
              new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .build(true));
    UnitTestHelper.setLog4jLevel(Parser.class, Level.ERROR);
  }

  public void happyPath(boolean longOpt) throws ParseException {
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor")
                                      .build(longOpt);
    assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPES.get(cli));
  }

  @Test
  public void testConfig_noExtra() throws ParseException {
    testConfig_noExtra(true);
    testConfig_noExtra(false);
  }

  public void testConfig_noExtra(boolean longOpt) throws ParseException {
   CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                     .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                     .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_WORKERS, "1")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_ACKERS, "2")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_MAX_TASK_PARALLELISM, "3")
                                     .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
                                     .build(longOpt);
    Optional<Config> configOptional = ParserTopologyCLI.ParserOptions.getConfig(cli);
    Config config = configOptional.get();
    assertEquals(1, config.get(Config.TOPOLOGY_WORKERS));
    assertEquals(2, config.get(Config.TOPOLOGY_ACKER_EXECUTORS));
    assertEquals(3, config.get(Config.TOPOLOGY_MAX_TASK_PARALLELISM));
    assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
  }

  @Test
  public void testOutputTopic() throws Exception {
    testOutputTopic(true);
    testOutputTopic(false);
  }

  public void testOutputTopic(boolean longOpt) throws ParseException {
     CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor")
                                      .with(ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC, "my_topic")
                                      .build(longOpt);
    assertEquals("my_topic", ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC.get(cli));
  }

  /**
    {
      "string" : "foo"
     ,"integer" : 1
    }
   */
  @Multiline
  public static String extraConfig;

  @Test
  public void testConfig_extra() throws Exception {
    testConfig_extra(true);
    testConfig_extra(false);
  }

  public void testConfig_extra(boolean longOpt) throws IOException, ParseException {
    File extraFile = File.createTempFile("extra", "json");
    try {
      FileUtils.write(extraFile, extraConfig);
      CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor")
              .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
              .with(ParserTopologyCLI.ParserOptions.EXTRA_OPTIONS, extraFile.getAbsolutePath())
              .build(longOpt);
      Optional<Config> configOptional = ParserTopologyCLI.ParserOptions.getConfig(cli);
      Config config = configOptional.get();
      assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
      assertEquals("foo", config.get("string"));
      assertEquals(1, config.get("integer"));
    } finally{
      extraFile.deleteOnExit();
    }
  }

  private static class ParserInput {
    private List<Integer> spoutParallelism;
    private List<Integer> spoutNumTasks;
    private Integer parserParallelism;
    private Integer parserNumTasks;
    private Integer errorParallelism;
    private Integer errorNumTasks;
    private List<Map<String, Object>> spoutConfig;
    private String securityProtocol;
    private Config stormConf;
    private String outputTopic;
    private String errorTopic;

    public ParserInput(ValueSupplier<List> spoutParallelism,
                       ValueSupplier<List> spoutNumTasks,
                       ValueSupplier<Integer> parserParallelism,
                       ValueSupplier<Integer> parserNumTasks,
                       ValueSupplier<Integer> errorParallelism,
                       ValueSupplier<Integer> errorNumTasks,
                       ValueSupplier<List> spoutConfig,
                       ValueSupplier<String> securityProtocol,
                       ValueSupplier<Config> stormConf,
                       ValueSupplier<String> outputTopic,
                       ValueSupplier<String> errorTopic,
                       List<SensorParserConfig> configs
                      )
    {
      this.spoutParallelism = spoutParallelism.get(configs, List.class);
      this.spoutNumTasks = spoutNumTasks.get(configs, List.class);
      this.parserParallelism = parserParallelism.get(configs, Integer.class);
      this.parserNumTasks = parserNumTasks.get(configs, Integer.class);
      this.errorParallelism = errorParallelism.get(configs, Integer.class);
      this.errorNumTasks = errorNumTasks.get(configs, Integer.class);
      this.spoutConfig = spoutConfig.get(configs, List.class);
      this.securityProtocol = securityProtocol.get(configs, String.class);
      this.stormConf = stormConf.get(configs, Config.class);
      this.outputTopic = outputTopic.get(configs, String.class);
      this.errorTopic = errorTopic.get(configs, String.class);
    }

    public List<Integer> getSpoutParallelism() {
      return spoutParallelism;
    }

    public List<Integer> getSpoutNumTasks() {
      return spoutNumTasks;
    }

    public Integer getParserParallelism() {
      return parserParallelism;
    }

    public Integer getParserNumTasks() {
      return parserNumTasks;
    }

    public Integer getErrorParallelism() {
      return errorParallelism;
    }

    public Integer getErrorNumTasks() {
      return errorNumTasks;
    }

    public List<Map<String, Object>> getSpoutConfig() {
      return spoutConfig;
    }

    public String getSecurityProtocol() {
      return securityProtocol;
    }

    public Config getStormConf() {
      return stormConf;
    }

    public String getOutputTopic() {
      return outputTopic;
    }

    public String getErrorTopic() {
      return errorTopic;
    }
  }

  /**
   * {
   *   "parserClassName": "org.apache.metron.parsers.GrokParser",
   *   "sensorTopic": "squid",
   *   "parserConfig": {
   *      "grokPath": "/patterns/squid",
   *      "patternLabel": "SQUID_DELIMITED",
   *      "timestampField": "timestamp"
   *   },
   *   "fieldTransformations" : [
   *      {
   *        "transformation" : "STELLAR",
   *        "output" : [
   *            "full_hostname",
   *            "domain_without_subdomains"
   *        ],
   *        "config" : {
   *            "full_hostname" : "URL_TO_HOST(url)",
   *            "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   *        }
   *      }
   *   ]
   * }
   */
  @Multiline
  public static String baseConfig;

  private static SensorParserConfig getBaseConfig() {
    try {
      return JSONUtils.INSTANCE.load(baseConfig, SensorParserConfig.class);
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Test
  public void testSpoutParallelism() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.SPOUT_PARALLELISM
        , "10"
        , input -> input.getSpoutParallelism().equals(Collections.singletonList(10))
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setSpoutParallelism(20);
          return Collections.singletonList(config);
        }
        , input -> input.getSpoutParallelism().equals(Collections.singletonList(20))
    );
  }

  @Test
  public void testSpoutParallelismMultiple() throws Exception {
    // Each spout uses it's own
    // Return one per spout.
    List<Integer> spoutParCli = new ArrayList<>();
    spoutParCli.add(10);
    spoutParCli.add(12);
    List<Integer> spoutParConfig = new ArrayList<>();
    spoutParConfig.add(20);
    spoutParConfig.add(30);
    testConfigOption(ParserTopologyCLI.ParserOptions.SPOUT_PARALLELISM
        , "10,12"
        , input -> input.getSpoutParallelism().equals(spoutParCli)
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setSpoutParallelism(20);
          SensorParserConfig config2 = getBaseConfig();
          config2.setSpoutParallelism(30);
          List<SensorParserConfig> configs = new ArrayList<>();
          configs.add(config);
          configs.add(config2);
          return configs;
        }
        , input -> input.getSpoutParallelism().equals(spoutParConfig)
    );
  }

  @Test
  public void testSpoutNumTasks() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.SPOUT_NUM_TASKS
                    , "10"
                    , input -> input.getSpoutNumTasks().equals(Collections.singletonList(10))
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setSpoutNumTasks(20);
                      return Collections.singletonList(config);
                    }
                    , input -> input.getSpoutNumTasks().equals(Collections.singletonList(20))
                    );
  }

  @Test
  public void testSpoutNumTasksMultiple() throws Exception {
    // Return one per spout.
    List<Integer> numTasksCli = new ArrayList<>();
    numTasksCli.add(10);
    numTasksCli.add(12);
    List<Integer> numTasksConfig = new ArrayList<>();
    numTasksConfig.add(20);
    numTasksConfig.add(30);
    testConfigOption(ParserTopologyCLI.ParserOptions.SPOUT_NUM_TASKS
        , "10,12"
        , input -> input.getSpoutNumTasks().equals(numTasksCli)
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setSpoutNumTasks(20);
          SensorParserConfig config2 = getBaseConfig();
          config2.setSpoutNumTasks(30);
          List<SensorParserConfig> configs = new ArrayList<>();
          configs.add(config);
          configs.add(config2);
          return configs;
        }
        , input -> input.getSpoutNumTasks().equals(numTasksConfig)
    );
  }

  @Test
  public void testParserParallelism() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.PARSER_PARALLELISM
        , "10"
        , input -> input.getParserParallelism().equals(10)
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setParserParallelism(20);
          return Collections.singletonList(config);
        }
        , input -> input.getParserParallelism().equals(20)
    );
  }

  @Test
  public void testParserParallelismMultiple() throws Exception {
    // Last one wins
    testConfigOption(ParserTopologyCLI.ParserOptions.PARSER_PARALLELISM
        , "10"
        , input -> input.getParserParallelism().equals(10)
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setParserParallelism(20);
          SensorParserConfig config2 = getBaseConfig();
          config2.setParserParallelism(30);
          List<SensorParserConfig> configs = new ArrayList<>();
          configs.add(config);
          configs.add(config2);
          return configs;
        }
        , input -> input.getParserParallelism().equals(30)
    );
  }

  @Test
  public void testParserNumTasks() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.PARSER_NUM_TASKS
                    , "10"
                    , input -> input.getParserNumTasks().equals(10)
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setParserNumTasks(20);
                      SensorParserConfig config2 = getBaseConfig();
                      config2.setParserNumTasks(30);
                      List<SensorParserConfig> configs = new ArrayList<>();
                      configs.add(config);
                      configs.add(config2);
                      return configs;
                    }
                    , input -> input.getParserNumTasks().equals(30)
                    );
  }

  @Test
  public void testParserNumTasksMultiple() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.PARSER_NUM_TASKS
        , "10"
        , input -> input.getParserNumTasks().equals(10)
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setParserNumTasks(20);
          return Collections.singletonList(config);
        }
        , input -> input.getParserNumTasks().equals(20)
    );
  }

  @Test
  public void testErrorParallelism() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.ERROR_WRITER_PARALLELISM
                    , "10"
                    , input -> input.getErrorParallelism().equals(10)
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setErrorWriterParallelism(20);
                      return Collections.singletonList(config);
                    }
                    , input -> input.getErrorParallelism().equals(20)
                    );
  }

  @Test
  public void testErrorNumTasks() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.ERROR_WRITER_NUM_TASKS
                    , "10"
                    , input -> input.getErrorNumTasks().equals(10)
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setErrorWriterNumTasks(20);
                      return Collections.singletonList(config);
                    }
                    , input -> input.getErrorNumTasks().equals(20)
                    );
  }

  @Test
  public void testSecurityProtocol_fromCLI() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.SECURITY_PROTOCOL
                    , "PLAINTEXT"
                    , input -> input.getSecurityProtocol().equals("PLAINTEXT")
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setSecurityProtocol("KERBEROS");
                      return Collections.singletonList(config);
                    }
                    , input -> input.getSecurityProtocol().equals("KERBEROS")
                    );
  }

  @Test
  public void testSecurityProtocol_fromCLIMultipleUniform() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.SECURITY_PROTOCOL
        , "PLAINTEXT"
        , input -> input.getSecurityProtocol().equals("PLAINTEXT")
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setSecurityProtocol("PLAINTEXT");
          SensorParserConfig config2 = getBaseConfig();
          config2.setSecurityProtocol("PLAINTEXT");
          List<SensorParserConfig> configs = new ArrayList<>();
          configs.add(config);
          configs.add(config2);
          return configs;
        }
        , input -> input.getSecurityProtocol().equals("PLAINTEXT")
    );
  }

  @Test
  public void testSecurityProtocol_fromCLIMultipleMixed() throws Exception {
    // Non plaintext wins
    testConfigOption(ParserTopologyCLI.ParserOptions.SECURITY_PROTOCOL
        , "PLAINTEXT"
        , input -> input.getSecurityProtocol().equals("PLAINTEXT")
        , () -> {
          SensorParserConfig config = getBaseConfig();
          config.setSecurityProtocol("PLAINTEXT");
          SensorParserConfig config2 = getBaseConfig();
          config2.setSecurityProtocol("KERBEROS");
          SensorParserConfig config3 = getBaseConfig();
          config3.setSecurityProtocol("PLAINTEXT");
          List<SensorParserConfig> configs = new ArrayList<>();
          configs.add(config);
          configs.add(config2);
          configs.add(config3);
          return configs;
        }
        , input -> input.getSecurityProtocol().equals("KERBEROS")
    );
  }

  @Test
  public void testSecurityProtocol_fromSpout() throws Exception {
    //Ultimately the order of precedence is CLI > spout config > parser config
    File extraConfig = File.createTempFile("spoutConfig", "json");
      extraConfig.deleteOnExit();
      writeMap(extraConfig, new HashMap<String, Object>() {{
        put("security.protocol", "PLAINTEXTSASL");
      }});
    {
      //Ensure that the CLI spout config takes precedence

      testConfigOption(new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class) {{
                         put(ParserTopologyCLI.ParserOptions.SPOUT_CONFIG, extraConfig.getAbsolutePath());
                         put(ParserTopologyCLI.ParserOptions.SECURITY_PROTOCOL, "PLAINTEXT");
                       }}
              , input -> input.getSecurityProtocol().equals("PLAINTEXT")
              , () -> {
                SensorParserConfig config = getBaseConfig();
                config.setSecurityProtocol("PLAINTEXTSASL_FROM_ZK");
                return Collections.singletonList(config);
              }
              , input -> input.getSecurityProtocol().equals("PLAINTEXTSASL_FROM_ZK")
      );
    }
    {
      //Ensure that the spout config takes precedence
      testConfigOption(new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class) {{
                         put(ParserTopologyCLI.ParserOptions.SPOUT_CONFIG, extraConfig.getAbsolutePath());
                       }}
              , input -> input.getSecurityProtocol().equals("PLAINTEXTSASL")
              , () -> {
                SensorParserConfig config = getBaseConfig();
                config.setSecurityProtocol("PLAINTEXTSASL_FROM_ZK");
                return Collections.singletonList(config);
              }
              , input -> input.getSecurityProtocol().equals("PLAINTEXTSASL_FROM_ZK")
      );
    }
  }

  @Test
  public void testTopologyConfig_fromConfigExplicitly() throws Exception {
    testConfigOption(new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class)
                    {{
                      put(ParserTopologyCLI.ParserOptions.NUM_WORKERS, "10");
                      put(ParserTopologyCLI.ParserOptions.NUM_ACKERS, "20");
                    }}
                    , input -> {
                        Config c = input.getStormConf();
                        return (int)c.get(Config.TOPOLOGY_WORKERS) == 10
                            && (int)c.get(Config.TOPOLOGY_ACKER_EXECUTORS) == 20;
                      }
                      , () -> {
                        SensorParserConfig config = getBaseConfig();
                        config.setNumWorkers(100);
                        config.setNumAckers(200);
                        return Collections.singletonList(config);
                              }
                      , input -> {
                          Config c = input.getStormConf();
                          return (int)c.get(Config.TOPOLOGY_WORKERS) == 100
                              && (int)c.get(Config.TOPOLOGY_ACKER_EXECUTORS) == 200
                                  ;
                                 }
                    );
  }

  @Test
  public void testTopologyConfig() throws Exception {
    File extraConfig = File.createTempFile("topologyConfig", "json");
    extraConfig.deleteOnExit();
    writeMap(extraConfig, new HashMap<String, Object>() {{
      put(Config.TOPOLOGY_DEBUG, true);
    }});
    testConfigOption(new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class)
                    {{
                      put(ParserTopologyCLI.ParserOptions.NUM_WORKERS, "10");
                      put(ParserTopologyCLI.ParserOptions.NUM_ACKERS, "20");
                      put(ParserTopologyCLI.ParserOptions.EXTRA_OPTIONS, extraConfig.getAbsolutePath());
                    }}
                    , input -> {
                        Config c = input.getStormConf();
                        return (int)c.get(Config.TOPOLOGY_WORKERS) == 10
                            && (int)c.get(Config.TOPOLOGY_ACKER_EXECUTORS) == 20
                            && (boolean)c.get(Config.TOPOLOGY_DEBUG);
                      }
                      , () -> {
                        SensorParserConfig config = getBaseConfig();
                        config.setStormConfig(
                          new HashMap<String, Object>() {{
                            put(Config.TOPOLOGY_WORKERS, 100);
                            put(Config.TOPOLOGY_ACKER_EXECUTORS, 200);
                          }}
                                             );
                        return Collections.singletonList(config);
                              }
                      , input -> {
                          Config c = input.getStormConf();
                          return (int)c.get(Config.TOPOLOGY_WORKERS) == 100
                              && (int)c.get(Config.TOPOLOGY_ACKER_EXECUTORS) == 200
                              && !c.containsKey(Config.TOPOLOGY_DEBUG);
                                 }
                    );
  }

  @Test
  public void testSpoutConfig() throws Exception {
    File extraConfig = File.createTempFile("spoutConfig", "json");
    extraConfig.deleteOnExit();
    writeMap(extraConfig, new HashMap<String, Object>() {{
      put("extra_config", "from_file");
    }});
    EnumMap<ParserTopologyCLI.ParserOptions, String> cliOptions = new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class)
                    {{
                      put(ParserTopologyCLI.ParserOptions.SPOUT_CONFIG, extraConfig.getAbsolutePath());
                    }};
    Predicate<ParserInput> cliOverrideExpected = input -> {
      return input.getSpoutConfig().get(0).get("extra_config").equals("from_file");
    };

    Predicate<ParserInput> configOverrideExpected = input -> {
      return input.getSpoutConfig().get(0).get("extra_config").equals("from_zk");
    };

    Supplier<List<SensorParserConfig>> configSupplier = () -> {
      SensorParserConfig config = getBaseConfig();
      config.setSpoutConfig(
              new HashMap<String, Object>() {{
                put("extra_config", "from_zk");
              }}
      );
      return Collections.singletonList(config);
    };
    testConfigOption( cliOptions
                    , cliOverrideExpected
                    , configSupplier
                    , configOverrideExpected
                    );
  }

  private void writeMap(File outFile, Map<String, Object> config) throws IOException {
    FileUtils.write(outFile, JSONUtils.INSTANCE.toJSON(config, true));
  }

  private void testConfigOption( ParserTopologyCLI.ParserOptions option
                               , String cliOverride
                               , Predicate<ParserInput> cliOverrideCondition
                               , Supplier<List<SensorParserConfig>> configSupplier
                               , Predicate<ParserInput> configOverrideCondition
  ) throws Exception {
    testConfigOption(
            new EnumMap<ParserTopologyCLI.ParserOptions, String>(ParserTopologyCLI.ParserOptions.class) {{
              put(option, cliOverride);
            }},
            cliOverrideCondition,
            configSupplier,
            configOverrideCondition
    );
  }

  private void testConfigOption( EnumMap<ParserTopologyCLI.ParserOptions, String> options
                               , Predicate<ParserInput> cliOverrideCondition
                               , Supplier<List<SensorParserConfig>> configSupplier
                               , Predicate<ParserInput> configOverrideCondition
  ) throws Exception {
    //CLI Override
    List<SensorParserConfig> configs = configSupplier.get();
    {
      CLIBuilder builder = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor");
      for(Map.Entry<ParserTopologyCLI.ParserOptions, String> entry : options.entrySet()) {
        builder.with(entry.getKey(), entry.getValue());
      }
      CommandLine cmd = builder.build(true);
      ParserInput input = getInput(cmd, configs);
      assertTrue(cliOverrideCondition.test(input));
    }
    // Config Override
    {
      CLIBuilder builder = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPES, "mysensor");
      CommandLine cmd = builder.build(true);
      ParserInput input = getInput(cmd, configs);
      assertTrue(configOverrideCondition.test(input));
    }
  }

  private static ParserInput getInput(CommandLine cmd, List<SensorParserConfig> configs ) throws Exception {
    final ParserInput[] parserInput = new ParserInput[]{null};
    new ParserTopologyCLI() {
      @Override
      protected ParserTopologyBuilder.ParserTopology getParserTopology(
              String zookeeperUrl,
              Optional<String> brokerUrl,
              List<String> sensorType,
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

       parserInput[0] = new ParserInput(
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
               errorTopic,
               configs
       );

        return null;
      }
    }.createParserTopology(cmd);
    return parserInput[0];
  }

}
