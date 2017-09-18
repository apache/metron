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
import static org.apache.metron.common.utils.StringUtils.stripLines;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.fail;

import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

  @Before
  public void setup() throws Exception {
    testZkServer = new TestingServer(true);
    zookeeperUrl = testZkServer.getConnectString();
    client = ConfigurationsUtils.getClient(zookeeperUrl);
    client.start();
    File sensorDir = new File(new File(TestConstants.SAMPLE_CONFIG_PATH), ENRICHMENT.getDirectory());
    sensors.addAll(Collections2.transform(
             Arrays.asList(sensorDir.list())
            ,s -> Iterables.getFirst(Splitter.on('.').split(s), "null")
                                         )
                  );
    tmpDir = TestUtils.createTempDir(this.getClass().getName());
    configDir = TestUtils.createDir(tmpDir, "config");
    parsersDir = TestUtils.createDir(configDir, "parsers");
    enrichmentsDir = TestUtils.createDir(configDir, "enrichments");
    indexingDir = TestUtils.createDir(configDir, "indexing");
    pushConfigs();
  }

  private void pushConfigs() throws Exception {
    String[] args = new String[]{
            "-z", zookeeperUrl
            , "--mode", "PUSH"
            , "--input_dir", TestConstants.SAMPLE_CONFIG_PATH
    };
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  public void pullConfigs(boolean force) throws Exception {
    String[] args = null;
    if(force) {
      args = new String[]{
              "-z", zookeeperUrl
              , "--mode", "PULL"
              , "--output_dir", outDir
              , "--force"
      };
    }
    else {
      args = new String[]{
              "-z", zookeeperUrl
              , "--mode", "PULL"
              , "--output_dir", outDir
      };
    }
    ConfigurationManager manager = new ConfigurationManager();
    manager.run(ConfigurationManager.ConfigurationOptions.parse(new PosixParser(), args));
  }

  public void validateConfigsOnDisk(File configDir) throws IOException {
    File globalConfigFile = new File(configDir, "global.json");
    Assert.assertTrue("Global config does not exist", globalConfigFile.exists());
    validateConfig("global", GLOBAL, new String(Files.readAllBytes(Paths.get(globalConfigFile.toURI()))));
    for(String sensor : sensors) {
      File sensorFile = new File(configDir, ENRICHMENT.getDirectory() + "/" + sensor + ".json");
      Assert.assertTrue(sensor + " config does not exist", sensorFile.exists());
      validateConfig(sensor, ENRICHMENT, new String(Files.readAllBytes(Paths.get(sensorFile.toURI()))));
    }
  }

  @Test
  public void testPull() throws Exception {
    cleanDir(new File(outDir));
    pullConfigs(false);
    validateConfigsOnDisk(new File(outDir));
    try {
      //second time without force should
      pullConfigs(false);
      fail("Should have failed to pull configs in a directory structure that already exists.");
    }
    catch(IllegalStateException t) {
      //make sure we didn't bork anything
      validateConfigsOnDisk(new File(outDir));
    }
    pullConfigs(true);
    validateConfigsOnDisk(new File(outDir));
  }
  public void validateConfig(String name, ConfigurationType type, String data)
  {
      try {
        type.deserialize(data);
      } catch (Exception e) {
        fail("Unable to load config " + name + ": " + data);
      }
  }
  @Test
  public void testPush() throws Exception {
    pushConfigs();
    final Set<String> sensorsInZookeeper = new HashSet<>();
    final BooleanWritable foundGlobal = new BooleanWritable(false);
    ConfigurationsUtils.visitConfigs(client, new ConfigurationsUtils.ConfigurationVisitor() {
      @Override
      public void visit(ConfigurationType configurationType, String name, String data) {
        Assert.assertTrue(data.length() > 0);
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
    Assert.assertEquals(true, foundGlobal.get());
    Assert.assertEquals(sensorsInZookeeper, sensors);
  }

  /**
   * { "a": "b" }
   */
  @Multiline
  private static String someConfig;

  @Test
  public void writes_global_config_to_zookeeper() throws Exception {
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, someConfig);
    pushConfigs(GLOBAL, configDir);
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL), 1));
    Assert.assertThat(actual, equalTo(expected));
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

  private String redirectSystemOut(final String[] args, RedirectCallback callback)
      throws Exception {
    PrintStream os = System.out;
    try (OutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos)) {
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
   *{
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squid",
   "parserConfig": {
   "grokPath": "/patterns/squid",
   "patternLabel": "SQUID_DELIMITED",
   "timestampField": "timestamp"
   },
   "fieldTransformations" : [
   {
   "transformation" : "STELLAR"
   ,"output" : [ "full_hostname", "domain_without_subdomains" ]
   ,"config" : {
   "full_hostname" : "URL_TO_HOST(url)"
   ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   }
   }
   ]
   }
   */
  @Multiline
  private static String squidParserConfig;

  @Test
  public void writes_single_parser_config_to_zookeeper() throws Exception {
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(squidParserConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    Assert.assertThat(actual, equalTo(expected));
  }

  /**
   * {
   "enrichment" : {
   "fieldMap": {
   "geo": ["ip_dst_addr", "ip_src_addr"],
   "host": ["host"]
   }
   },
   "threatIntel": {
   "fieldMap": {
   "hbaseThreatIntel": ["ip_src_addr", "ip_dst_addr"]
   },
   "fieldToTypeMap": {
   "ip_src_addr" : ["malicious_ip"],
   "ip_dst_addr" : ["malicious_ip"]
   }
   }
   }
   */
  @Multiline
  private static String someEnrichmentConfig;

  @Test
  public void writes_single_enrichment_config_to_zookeeper() throws Exception {
    File configFile = new File(enrichmentsDir, "myenrichment.json");
    TestUtils.write(configFile, someEnrichmentConfig);
    pushConfigs(ENRICHMENT, configDir, Optional.of("myenrichment"));
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someEnrichmentConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(ENRICHMENT, Optional.of("myenrichment")), 1));
    Assert.assertThat(actual, equalTo(expected));
  }

  /**
   * {
   "hdfs" : {
   "index": "myindex",
   "batchSize": 5,
   "enabled" : true
   },
   "elasticsearch" : {
   "index": "myindex",
   "batchSize": 5,
   "enabled" : true
   },
   "solr" : {
   "index": "myindex",
   "batchSize": 5,
   "enabled" : true
   }
   }
   */
  @Multiline
  private static String someIndexingConfig;

  @Test
  public void writes_single_indexing_config_to_zookeeper() throws Exception {
    File configFile = new File(indexingDir, "myindex.json");
    TestUtils.write(configFile, someIndexingConfig);
    pushConfigs(INDEXING, configDir, Optional.of("myindex"));
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(someIndexingConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(INDEXING, Optional.of("myindex")), 1));
    Assert.assertThat(actual, equalTo(expected));
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
  public void patches_global_config_from_file() throws Exception {
    File patchFile = new File(tmpDir, "global-config-patch.json");
    TestUtils.write(patchFile, somePatchConfig);
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, someConfig);
    pushConfigs(GLOBAL, configDir, Optional.of("global"));
    patchConfigs(GLOBAL, Optional.of(patchFile), Optional.of("global"), Optional.empty(), Optional.empty(), Optional.empty());
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedSomeConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL, Optional.of("global")), 1));
    Assert.assertThat(actual, equalTo(expected));
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
   * [ { "op": "replace", "path": "/parserConfig/timestampField", "value": "heyjoe" } ]
   */
  @Multiline
  public static String someParserPatch;

  /**
   *{
   "parserClassName": "org.apache.metron.parsers.GrokParser",
   "sensorTopic": "squid",
   "parserConfig": {
   "grokPath": "/patterns/squid",
   "patternLabel": "SQUID_DELIMITED",
   "timestampField": "heyjoe"
   },
   "fieldTransformations" : [
   {
   "transformation" : "STELLAR"
   ,"output" : [ "full_hostname", "domain_without_subdomains" ]
   ,"config" : {
   "full_hostname" : "URL_TO_HOST(url)"
   ,"domain_without_subdomains" : "DOMAIN_REMOVE_SUBDOMAINS(full_hostname)"
   }
   }
   ]
   }
   */
  @Multiline
  public static String expectedPatchedParser;

  @Test
  public void patches_parser_config_from_file() throws Exception {
    File patchFile = new File(tmpDir, "parser-patch.json");
    TestUtils.write(patchFile, someParserPatch);
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));
    patchConfigs(PARSER, Optional.of(patchFile), Optional.of("myparser"), Optional.empty(), Optional.empty(), Optional.empty());
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedPatchedParser);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    Assert.assertThat(actual, equalTo(expected));
  }

  @Test
  public void patches_parser_config_from_key_value() throws Exception {
    File configFile = new File(parsersDir, "myparser.json");
    TestUtils.write(configFile, squidParserConfig);
    pushConfigs(PARSER, configDir, Optional.of("myparser"));
    patchConfigs(PARSER, Optional.empty(), Optional.of("myparser"), Optional.of(ADD), Optional.of("/parserConfig/timestampField"), Optional.of("\"\"heyjoe\"\""));
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedPatchedParser);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(PARSER, Optional.of("myparser")), 1));
    Assert.assertThat(actual, equalTo(expected));
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
  public void patches_global_config_from_complex_key_value() throws Exception {
    File configFile = new File(configDir, "global.json");
    TestUtils.write(configFile, someConfig);
    pushConfigs(GLOBAL, configDir, Optional.of("global"));
    patchConfigs(GLOBAL, Optional.empty(), Optional.of("global"), Optional.of(ADD), Optional.of("/foo"), Optional.of("{ \"bar\" : { \"baz\" : [ \"bazval1\", \"bazval2\" ] } }"));
    byte[] expected = JSONUtils.INSTANCE.toJSONPretty(expectedComplexConfig);
    byte[] actual = JSONUtils.INSTANCE.toJSONPretty(stripLines(dumpConfigs(GLOBAL, Optional.of("global")), 1));
    Assert.assertThat(actual, equalTo(expected));
  }

  @After
  public void tearDown() throws IOException {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }
}
