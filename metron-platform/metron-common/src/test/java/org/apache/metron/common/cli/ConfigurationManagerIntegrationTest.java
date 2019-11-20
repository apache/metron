/*
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

package org.apache.metron.common.cli;

import static org.apache.metron.common.cli.ConfigurationManager.PatchMode.ADD;
import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.INDEXING;
import static org.apache.metron.common.configuration.ConfigurationType.PARSER;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;
import static org.apache.metron.common.utils.StringUtils.stripLines;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.metron.TestConstants;
import org.apache.metron.common.cli.ConfigurationManager.PatchMode;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigurationManagerIntegrationTest {
  private TestingServer testZkServer;
  private CuratorFramework client;
  private String zookeeperUrl;
  private String outDir = "target/configs";
  private File tmpDir;
  private File configDir;
  private File parsersDir;
  private File enrichmentsDir;
  private File indexingDir;
  private Set<String> sensors = new HashSet<>();

  private void cleanDir(File rootDir) throws IOException {
    if(rootDir.isDirectory()) {
      try {
        Files.delete(Paths.get(rootDir.toURI()));
      } catch (DirectoryNotEmptyException dne) {
        for(File f : rootDir.listFiles()) {
          cleanDir(f);
        }
        rootDir.delete();
      }
    }
    else {
      rootDir.delete();
    }
  }

  @BeforeEach
  public void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
    File sensorDir = new File(new File(TestConstants.SAMPLE_CONFIG_PATH), ENRICHMENT.getDirectory());
    sensors.addAll(Collections2.transform(
             Arrays.asList(sensorDir.list())
            , s -> Iterables.getFirst(Splitter.on('.').split(s), "null")));
    tmpDir = TestUtils.createTempDir(this.getClass().getName());
    configDir = TestUtils.createDir(tmpDir, "config");
    parsersDir = TestUtils.createDir(configDir, "parsers");
    enrichmentsDir = TestUtils.createDir(configDir, "enrichments");
    indexingDir = TestUtils.createDir(configDir, "indexing");
    pushAllConfigs();
  }

  private void pushAllConfigs() throws Exception {
    pushAllConfigs(TestConstants.SAMPLE_CONFIG_PATH);
  }

  private void pushAllConfigs(String inputDir) throws Exception {
    String[] args = new String[]{
            "-z", zookeeperUrl
            , "--mode", "PUSH"
            , "--input_dir", inputDir
    };
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  private void pullConfigs(boolean force) throws Exception {
    String[] args = null;
    if(force) {
      args = new String[]{
              "-z", zookeeperUrl
              , "--mode", "PULL"
              , "--output_dir", outDir
              , "--force"
      };

    } else {
      args = new String[]{
              "-z", zookeeperUrl
              , "--mode", "PULL"
              , "--output_dir", outDir
      };
    }
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  private void validateConfigsOnDisk(File configDir) throws IOException {
    File globalConfigFile = new File(configDir, "global.json");
    assertTrue(globalConfigFile.exists(), "Global config does not exist");
    validateConfig("global", GLOBAL, new String(Files.readAllBytes(Paths.get(globalConfigFile.toURI())),
        StandardCharsets.UTF_8));
    for(String sensor : sensors) {
      File sensorFile = new File(configDir, ENRICHMENT.getDirectory() + "/" + sensor + ".json");
      assertTrue(sensorFile.exists(), sensor + " config does not exist");
      validateConfig(sensor, ENRICHMENT, new String(Files.readAllBytes(Paths.get(sensorFile.toURI())),
          StandardCharsets.UTF_8));
    }
  }

  @Test
  public void testPull() throws Exception {
    cleanDir(new File(outDir));
    pullConfigs(false);
    validateConfigsOnDisk(new File(outDir));

    //second time without force should error
    assertThrows(IllegalStateException.class, () -> pullConfigs(false), "Should have failed to pull configs in a directory structure that already exists.");

    //make sure we didn't bork anything
    validateConfigsOnDisk(new File(outDir));

    pullConfigs(true);
    validateConfigsOnDisk(new File(outDir));
  }

  private void validateConfig(String name, ConfigurationType type, String data) {
        assertDoesNotThrow(() -> type.deserialize(data), "Unable to load config " + name + ": " + data);
  }

  @Test
  public void testPushAll() throws Exception {

    // push all configs; parser, enrichment, indexing, etc
    pushAllConfigs();

    // validate
    final Set<String> sensorsInZookeeper = new HashSet<>();
    final BooleanWritable foundGlobal = new BooleanWritable(false);
    ConfigurationsUtils.visitConfigs(client, new ConfigurationsUtils.ConfigurationVisitor() {
      @Override
      public void visit(ConfigurationType configurationType, String name, String data) {
        assertTrue(data.length() > 0);
        validateConfig(name, configurationType, data);
        if(configurationType == GLOBAL) {
          validateConfig(name, configurationType, data);
          foundGlobal.set(true);
        }
        else {
          sensorsInZookeeper.add(name);
        }
      }
    });
    assertTrue(foundGlobal.get());
    assertEquals(sensorsInZookeeper, sensors);
  }

  @Test
  public void testPushAllWithBadConfig() throws Exception {

    // create a bad global config
    File globalConfigFile = new File(configDir, "global.json");
    TestUtils.write(globalConfigFile, badGlobalConfig);

    // create a parser config
    File squidConfigFile = new File(parsersDir, "squid.json");
    TestUtils.write(squidConfigFile, badParserConfig);

    // exception expected as the global and parser config is invalid
    assertThrows(RuntimeException.class, () -> pushAllConfigs(configDir.getAbsolutePath()));
  }

  /**
   * { "a": "b" }
   */
  @Multiline
  private static String globalConfig;

  @Test
  public void testPushGlobal() throws Exception {

    // create the config
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, globalConfig);

    // push the global config
    pushConfigs(GLOBAL, configDir);

    // validate
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(globalConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * { "invalid as needs to be key/values" }
   */
  @Multiline
  private static String badGlobalConfig;

  @Test
  public void testPushGlobalWithBadConfig() throws Exception {

    // create the config
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, badGlobalConfig);

    // push the global config
    // exception expected as the global config is invalid
    assertThrows(RuntimeException.class, () -> pushConfigs(GLOBAL, configDir));
  }

  private void pushConfigs(ConfigurationType type, File configPath) throws Exception {
    pushConfigs(type, configPath, Optional.empty());
  }

  private void pushConfigs(ConfigurationType type, File configPath, Optional<String> configName) throws Exception {
    String[] args = new String[]{
        "-z", zookeeperUrl,
        "--mode", "PUSH",
        "--config_type", type.toString(),
        "--input_dir", configPath.getAbsolutePath()
    };
    if (configName.isPresent()) {
      args = ArrayUtils.addAll(args, "--config_name", configName.get());
    }
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  private String dumpConfigs(ConfigurationType type) throws Exception {
    return dumpConfigs(type, Optional.empty());
  }

  private String dumpConfigs(ConfigurationType type, Optional<String> configName) throws Exception {
    String[] args = new String[]{
        "-z", zookeeperUrl,
        "--mode", "DUMP",
        "--config_type", type.toString()
    };
    if (configName.isPresent()) {
      args = ArrayUtils.addAll(args, "--config_name", configName.get());
    }
    ConfigurationManager manager = new ConfigurationManager();
    return redirectSystemOut(args, (a) -> {
        manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), a));
    });
  }

  public interface RedirectCallback {
    void call(String[] args) throws Exception;
  }

  private String redirectSystemOut(final String[] args, RedirectCallback callback) throws Exception {
    PrintStream os = System.out;
    try (OutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos, false, StandardCharsets.UTF_8.name())) {
      System.setOut(ps);
      callback.call(args);
      System.out.flush();
      System.setOut(os);
      return baos.toString();

    } finally {
      System.setOut(os);
    }
  }

  /**
   *  {
   *    "parserClassName": "org.apache.metron.parsers.GrokParser",
   *    "sensorTopic": "squid",
   *    "parserConfig": {
   *      "grokPath": "/patterns/squid",
   *      "patternLabel": "SQUID_DELIMITED",
   *      "timestampField": "timestamp"
   *    },
   *    "fieldTransformations" : [
   *      {
   *        "transformation" : "STELLAR",
   *        "output" : [ "full_hostname", "domain_without_subdomains" ],
   *        "config" : {
   *          "full_hostname" : "URL_TO_HOST(url)",
   *          "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   *        }
   *      }
   *    ]
   *  }
   */
  @Multiline
  private static String squidParserConfig;

  @Test
  public void testPushParser() throws Exception {

    // create a parser config
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);

    // push the parser config
    pushConfigs(PARSER, configDir, Optional.of("myparser"));

    // validate
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(squidParserConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   *  {
   *    "parserClassName": "org.apache.metron.parsers.GrokParser",
   *    "invalidFieldForParserConfig": "22"
   *  }
   */
  @Multiline
  private static String badParserConfig;

  @Test
  public void testPushParserWithBadConfig() throws Exception {

    // create a parser config
    File configFile = new File(parsersDir, "badparser.json");
    TestUtils.write(configFile, badParserConfig);

    // push the parser config
    // exception expected as the parser config is invalid
    assertThrows(RuntimeException.class, () -> pushConfigs(PARSER, configDir, Optional.of("badparser")));
  }

  /**
   * {
   *  "enrichment" : {
   *    "fieldMap": {
   *      "geo": ["ip_dst_addr", "ip_src_addr"],
   *      "host": ["host"]
   *    }
   *  },
   *  "threatIntel": {
   *    "fieldMap": {
   *      "hbaseThreatIntel": ["ip_src_addr", "ip_dst_addr"]
   *    },
   *    "fieldToTypeMap": {
   *      "ip_src_addr" : ["malicious_ip"],
   *      "ip_dst_addr" : ["malicious_ip"]
   *    }
   *   }
   * }
   */
  @Multiline
  private static String someEnrichmentConfig;

  @Test
  public void testPushEnrichment() throws Exception {

    // create enrichment config
    File configFile = new File(enrichmentsDir, "myenrichment.json");
    TestUtils.write(configFile, someEnrichmentConfig);

    // push enrichment config
    pushConfigs(ENRICHMENT, configDir, Optional.of("myenrichment"));

    // validate
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someEnrichmentConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(ENRICHMENT, Optional.of("myenrichment")), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * {
   *  "enrichment" : {
   *    "fieldMap": {
   *      "geo": ["ip_dst_addr", "ip_src_addr"],
   *      "host": ["host"]
   *    }
   *  },
   *  "invalidField": {
   *
   *  }
   * }
   */
  @Multiline
  private static String badEnrichmentConfig;

  @Test
  public void testPushEnrichmentWithBadConfig() throws Exception {

    // create enrichment config
    File configFile = new File(enrichmentsDir, "badenrichment.json");
    TestUtils.write(configFile, badEnrichmentConfig);

    // push enrichment config
    // exception expected as the enrichment config is invalid
    assertThrows(RuntimeException.class, () -> pushConfigs(ENRICHMENT, configDir, Optional.of("badenrichment")));
  }

  /**
   * {
   *  "hdfs" : {
   *      "index": "myindex",
   *      "batchSize": 5,
   *      "enabled" : true
   *   },
   *   "elasticsearch" : {
   *      "index": "myindex",
   *      "batchSize": 5,
   *      "enabled" : true
   *   },
   *   "solr" : {
   *      "index": "myindex",
   *      "batchSize": 5,
   *      "enabled" : true
   *   }
   * }
   */
  @Multiline
  private static String someIndexingConfig;

  @Test
  public void testPushIndexing() throws Exception {

    // write the indexing config
    File configFile = new File(indexingDir, "myindex.json");
    TestUtils.write(configFile, someIndexingConfig);

    // push the index config
    pushConfigs(INDEXING, configDir, Optional.of("myindex"));

    // validate
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someIndexingConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(INDEXING, Optional.of("myindex")), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * {
   *  "hdfs"
   * }
   */
  @Multiline
  private static String badIndexingConfig;

  @Test
  public void testPushIndexingWithBadConfig() throws Exception {

    // write the indexing config
    File configFile = new File(indexingDir, "myindex.json");
    TestUtils.write(configFile, badIndexingConfig);

    // push the index config
    // exception expected as the indexing config is invalid
    assertThrows(RuntimeException.class, () -> pushConfigs(INDEXING, configDir, Optional.of("myindex")));
  }

  /**
   * {
   *  "profiles": [
   *    {
   *      "profile": "hello-world",
   *      "onlyif":  "exists(ip_src_addr)",
   *      "foreach": "ip_src_addr",
   *      "init":    { "count": "0" },
   *      "update":  { "count": "count + 1" },
   *      "result":  "count"
   *    }
   *  ]
   * }
   */
  @Multiline
  private static String someProfilerConfig;

  @Test
  public void testPushProfiler() throws Exception {

    // create the profiler config
    File configFile = new File(configDir, "profiler.json");
    TestUtils.write(configFile, someProfilerConfig);

    // push the profiler config
    Optional<String> configName = Optional.empty();
    pushConfigs(PROFILER, configDir, configName);

    // validate
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someProfilerConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PROFILER, configName), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * {
   *  "profiles": [
   *    {
   *      "profile": "invalid; missing foreach, result, etc"
   *    }
   *  ]
   * }
   */
  @Multiline
  private static String badProfilerConfig;

  @Test
  public void testPushProfilerWithBadConfig() throws Exception {

    // write the indexing config
    File configFile = new File(configDir, "profiler.json");
    TestUtils.write(configFile, badProfilerConfig);

    // push the index config
    // exception expected as the profiler config is invalid
    Optional<String> configName = Optional.empty();
    assertThrows(RuntimeException.class, () -> pushConfigs(PROFILER, configDir, configName));
  }

  /**
   * [ { "op": "replace", "path": "/a", "value": [ "new1", "new2" ] } ]
   */
  @Multiline
  private static String somePatchConfig;

  /**
   * { "a": [ "new1", "new2" ] }
   */
  @Multiline
  private static String expectedSomeConfig;

  @Test
  public void testPatchGlobalFromFile() throws Exception {

    // create a patch file
    File patchFile = new File(tmpDir, "global-config-patch.json");
    TestUtils.write(patchFile, somePatchConfig);

    // create the global config
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, globalConfig);
    pushConfigs(GLOBAL, configDir, Optional.of("global"));

    // patch the global config
    patchConfigs(GLOBAL, Optional.of(patchFile), Optional.of("global"), Optional.empty(), Optional.empty(), Optional.empty());

    // validate the patch
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedSomeConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL, Optional.of("global")), 1));
    assertThat(actual, equalTo(expected));
  }

  private void patchConfigs(ConfigurationType type, Optional<File> patchPath,
      Optional<String> configName, Optional<PatchMode> patchMode, Optional<String> key,
      Optional<String> value) throws Exception {
    String[] args = new String[]{
        "-z", zookeeperUrl,
        "--mode", "PATCH",
        "--config_type", type.toString()
    };
    if (configName.isPresent()) {
      args = ArrayUtils.addAll(args, "--config_name", configName.get());
    }
    if (patchPath.isPresent()) {
      args = ArrayUtils.addAll(args, "--patch_file", patchPath.get().getAbsolutePath());
    } else if (patchMode.isPresent()) {
      args = ArrayUtils.addAll(args,
          "--patch_mode", patchMode.get().toString(),
          "--patch_key", key.get(),
          "--patch_value", value.get());
    }
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  /**
   * [
   *   {
   *    "op": "replace",
   *    "path": "/parserConfig/timestampField",
   *    "value": "heyjoe"
   *   }
   * ]
   */
  @Multiline
  public static String someParserPatch;

  /**
   * {
   *    "parserClassName": "org.apache.metron.parsers.GrokParser",
   *    "sensorTopic": "squid",
   *    "parserConfig": {
   *       "grokPath": "/patterns/squid",
   *       "patternLabel": "SQUID_DELIMITED",
   *       "timestampField": "heyjoe"
   *    },
   *    "fieldTransformations" : [
   *      {
   *        "transformation" : "STELLAR",
   *        "output" : [ "full_hostname", "domain_without_subdomains" ],
   *        "config" : {
   *          "full_hostname" : "URL_TO_HOST(url)",
   *          "domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   *        }
   *      }
   *    ]
   * }
   */
  @Multiline
  public static String expectedPatchedParser;

  @Test
  public void testPatchParserFromFile() throws Exception {

    // create a patch file
    File patchFile = new File(tmpDir, "parser-patch.json");
    TestUtils.write(patchFile, someParserPatch);

    // create a parser configuration
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));

    // patch the configuration
    patchConfigs(PARSER, Optional.of(patchFile), Optional.of("myparser"), Optional.empty(), Optional.empty(), Optional.empty());

    // validate the patch
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedPatchedParser);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    assertThat(actual, equalTo(expected));
  }

  @Test
  public void testPatchParserFromKeyValue() throws Exception {

    // push the parser config
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));

    // patch the parser configuration
    patchConfigs(PARSER, Optional.empty(), Optional.of("myparser"), Optional.of(ADD), Optional.of("/parserConfig/timestampField"), Optional.of("\"\"heyjoe\"\""));

    // validate the patch
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedPatchedParser);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * [
   *   {
   *    "op": "add",
   *    "path": "/invalidFieldForParserConfig",
   *    "value": "22"
   *   }
   * ]
   */
  @Multiline
  public static String badParserPatch;

  @Test
  public void testPatchParserWithBadConfig() throws Exception {

    // create a patch file that when applied makes the parser config invalid
    File patchFile = new File(tmpDir, "parser-patch.json");
    TestUtils.write(patchFile, badParserPatch);

    // create a parser configuration
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));

    // patch the configuration
    assertThrows(
        RuntimeException.class,
        () ->
            patchConfigs(
                PARSER,
                Optional.of(patchFile),
                Optional.of("myparser"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
  }

  /**
   * {
   *   "a": "b",
   *   "foo": {
   *     "bar": {
   *       "baz": [ "bazval1", "bazval2" ]
   *     }
   *   }
   * }
   */
  @Multiline
  private static String expectedComplexConfig;

  @Test
  public void testPatchGlobalFromComplexKeyValue() throws Exception {

    // write a global configuration
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, globalConfig);
    pushConfigs(GLOBAL, configDir, Optional.of("global"));

    // patch the global configuration
    patchConfigs(GLOBAL, Optional.empty(), Optional.of("global"), Optional.of(ADD), Optional.of("/foo"), Optional.of("{ \"bar\" : { \"baz\" : [ \"bazval1\", \"bazval2\" ] } }"));

    // validate the patch
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedComplexConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL, Optional.of("global")), 1));
    assertThat(actual, equalTo(expected));
  }

  /**
   * [
   *   {
   *    "op": "add",
   *    "path": "/invalidFieldForProfilerConfig",
   *    "value": "22"
   *   }
   * ]
   */
  @Multiline
  public static String badProfilerPatch;

  @Test
  public void testPatchProfilerWithBadConfig() throws Exception {

    // create a patch file that when applied makes the profiler config invalid
    File patchFile = new File(tmpDir, "patch.json");
    TestUtils.write(patchFile, badProfilerPatch);

    // create the profiler config
    File configFile = new File(configDir, "profiler.json");
    TestUtils.write(configFile, someProfilerConfig);

    // push the profiler config
    pushConfigs(PROFILER, configDir, Optional.empty());

    // patch the profiler config
    assertThrows(
        RuntimeException.class,
        () ->
            patchConfigs(
                PROFILER,
                Optional.of(patchFile),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
  }


  @AfterEach
  public void tearDown() throws IOException {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }
}
