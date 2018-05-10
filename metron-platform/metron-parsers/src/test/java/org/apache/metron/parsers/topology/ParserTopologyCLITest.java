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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.cli.Parser;
import org.apache.log4j.Level;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.parsers.topology.config.ValueSupplier;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.Config;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .build(longOpt);
    Assert.assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    Assert.assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    Assert.assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPE.get(cli));
  }
  @Test
  public void testCLI_happyPath() throws ParseException {
    happyPath(true);
    happyPath(false);
  }

  @Test(expected=ParseException.class)
  public void testCLI_insufficientArg() throws ParseException {
    UnitTestHelper.setLog4jLevel(Parser.class, Level.FATAL);
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .build(true);
    UnitTestHelper.setLog4jLevel(Parser.class, Level.ERROR);
  }

  public void happyPath(boolean longOpt) throws ParseException {
    CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .build(longOpt);
    Assert.assertEquals("myzk", ParserTopologyCLI.ParserOptions.ZK_QUORUM.get(cli));
    Assert.assertEquals("mybroker", ParserTopologyCLI.ParserOptions.BROKER_URL.get(cli));
    Assert.assertEquals("mysensor", ParserTopologyCLI.ParserOptions.SENSOR_TYPE.get(cli));
  }

  @Test
  public void testConfig_noExtra() throws ParseException {
    testConfig_noExtra(true);
    testConfig_noExtra(false);
  }

  public void testConfig_noExtra(boolean longOpt) throws ParseException {
   CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                     .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                     .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_WORKERS, "1")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_ACKERS, "2")
                                     .with(ParserTopologyCLI.ParserOptions.NUM_MAX_TASK_PARALLELISM, "3")
                                     .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
                                     .build(longOpt);
    Optional<Config> configOptional = ParserTopologyCLI.ParserOptions.getConfig(cli);
    Config config = configOptional.get();
    Assert.assertEquals(1, config.get(Config.TOPOLOGY_WORKERS));
    Assert.assertEquals(2, config.get(Config.TOPOLOGY_ACKER_EXECUTORS));
    Assert.assertEquals(3, config.get(Config.TOPOLOGY_MAX_TASK_PARALLELISM));
    Assert.assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
  }

  @Test
  public void testOutputTopic() throws Exception {
    testOutputTopic(true);
    testOutputTopic(false);
  }

  public void testOutputTopic(boolean longOpt) throws ParseException {
     CommandLine cli = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
                                      .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
                                      .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
                                      .with(ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC, "my_topic")
                                      .build(longOpt);
    Assert.assertEquals("my_topic", ParserTopologyCLI.ParserOptions.OUTPUT_TOPIC.get(cli));
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
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor")
              .with(ParserTopologyCLI.ParserOptions.MESSAGE_TIMEOUT, "4")
              .with(ParserTopologyCLI.ParserOptions.EXTRA_OPTIONS, extraFile.getAbsolutePath())
              .build(longOpt);
      Optional<Config> configOptional = ParserTopologyCLI.ParserOptions.getConfig(cli);
      Config config = configOptional.get();
      Assert.assertEquals(4, config.get(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS));
      Assert.assertEquals("foo", config.get("string"));
      Assert.assertEquals(1, config.get("integer"));
    } finally{
      extraFile.deleteOnExit();
    }
  }

  private static class ParserInput {
    private Integer spoutParallelism;
    private Integer spoutNumTasks;
    private Integer parserParallelism;
    private Integer parserNumTasks;
    private Integer errorParallelism;
    private Integer errorNumTasks;
    private Map<String, Object> spoutConfig;
    private String securityProtocol;
    private Config stormConf;
    private String outputTopic;
    private String errorTopic;

    public ParserInput(ValueSupplier<Integer> spoutParallelism,
                       ValueSupplier<Integer> spoutNumTasks,
                       ValueSupplier<Integer> parserParallelism,
                       ValueSupplier<Integer> parserNumTasks,
                       ValueSupplier<Integer> errorParallelism,
                       ValueSupplier<Integer> errorNumTasks,
                       ValueSupplier<Map> spoutConfig,
                       ValueSupplier<String> securityProtocol,
                       ValueSupplier<Config> stormConf,
                       ValueSupplier<String> outputTopic,
                       ValueSupplier<String> errorTopic,
                       SensorParserConfig config
                      )
    {
      this.spoutParallelism = spoutParallelism.get(config, Integer.class);
      this.spoutNumTasks = spoutNumTasks.get(config, Integer.class);
      this.parserParallelism = parserParallelism.get(config, Integer.class);
      this.parserNumTasks = parserNumTasks.get(config, Integer.class);
      this.errorParallelism = errorParallelism.get(config, Integer.class);
      this.errorNumTasks = errorNumTasks.get(config, Integer.class);
      this.spoutConfig = spoutConfig.get(config, Map.class);
      this.securityProtocol = securityProtocol.get(config, String.class);
      this.stormConf = stormConf.get(config, Config.class);
      this.outputTopic = outputTopic.get(config, String.class);
      this.errorTopic = outputTopic.get(config, String.class);
    }

    public Integer getSpoutParallelism() {
      return spoutParallelism;
    }

    public Integer getSpoutNumTasks() {
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

    public Map<String, Object> getSpoutConfig() {
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
                    , input -> input.getSpoutParallelism().equals(10)
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setSpoutParallelism(20);
                      return config;
                    }
                    , input -> input.getSpoutParallelism().equals(20)
                    );
  }

  @Test
  public void testSpoutNumTasks() throws Exception {
    testConfigOption(ParserTopologyCLI.ParserOptions.SPOUT_NUM_TASKS
                    , "10"
                    , input -> input.getSpoutNumTasks().equals(10)
                    , () -> {
                      SensorParserConfig config = getBaseConfig();
                      config.setSpoutNumTasks(20);
                      return config;
                    }
                    , input -> input.getSpoutNumTasks().equals(20)
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
                      return config;
                    }
                    , input -> input.getParserParallelism().equals(20)
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
                      return config;
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
                      return config;
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
                      return config;
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
                      return config;
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
                return config;
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
                return config;
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
                        return config;
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
                        return config;
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
      return input.getSpoutConfig().get("extra_config").equals("from_file");
    };

    Predicate<ParserInput> configOverrideExpected = input -> {
      return input.getSpoutConfig().get("extra_config").equals("from_zk")
                                  ;
    };

    Supplier<SensorParserConfig> configSupplier = () -> {
      SensorParserConfig config = getBaseConfig();
      config.setSpoutConfig(
              new HashMap<String, Object>() {{
                put("extra_config", "from_zk");
              }}
      );
      return config;
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
                               , Supplier<SensorParserConfig> configSupplier
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
                               , Supplier<SensorParserConfig> configSupplier
                               , Predicate<ParserInput> configOverrideCondition
  ) throws Exception {
    //CLI Override
    SensorParserConfig config = configSupplier.get();
    {
      CLIBuilder builder = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor");
      for(Map.Entry<ParserTopologyCLI.ParserOptions, String> entry : options.entrySet()) {
        builder.with(entry.getKey(), entry.getValue());
      }
      CommandLine cmd = builder.build(true);
      ParserInput input = getInput(cmd, config);
      Assert.assertTrue(cliOverrideCondition.test(input));
    }
    // Config Override
    {
      CLIBuilder builder = new CLIBuilder().with(ParserTopologyCLI.ParserOptions.BROKER_URL, "mybroker")
              .with(ParserTopologyCLI.ParserOptions.ZK_QUORUM, "myzk")
              .with(ParserTopologyCLI.ParserOptions.SENSOR_TYPE, "mysensor");
      CommandLine cmd = builder.build(true);
      ParserInput input = getInput(cmd, config);
      Assert.assertTrue(configOverrideCondition.test(input));
    }
  }

  private static ParserInput getInput(CommandLine cmd, SensorParserConfig config ) throws Exception {
    final ParserInput[] parserInput = new ParserInput[]{null};
    new ParserTopologyCLI() {
      @Override
      protected ParserTopologyBuilder.ParserTopology getParserTopology(
              String zookeeperUrl,
              Optional<String> brokerUrl,
              String sensorType,
              ValueSupplier<Integer> spoutParallelism,
              ValueSupplier<Integer> spoutNumTasks,
              ValueSupplier<Integer> parserParallelism,
              ValueSupplier<Integer> parserNumTasks,
              ValueSupplier<Integer> errorParallelism,
              ValueSupplier<Integer> errorNumTasks,
              ValueSupplier<Map> spoutConfig,
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
               config
       );

        return null;
      }
    }.createParserTopology(cmd);
    return parserInput[0];
  }

}
