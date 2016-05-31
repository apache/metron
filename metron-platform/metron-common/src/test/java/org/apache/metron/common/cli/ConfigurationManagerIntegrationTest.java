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

package org.apache.metron.common.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.PosixParser;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.metron.TestConstants;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ConfigurationManagerIntegrationTest {
  private TestingServer testZkServer;
  private CuratorFramework client;
  private String zookeeperUrl;
  private String outDir = "target/configs";
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
    File sensorDir = new File(new File(TestConstants.SAMPLE_CONFIG_PATH), ConfigurationType.ENRICHMENT.getDirectory());
    sensors.addAll(Collections2.transform(
             Arrays.asList(sensorDir.list())
            ,s -> Iterables.getFirst(Splitter.on('.').split(s), "null")
                                         )
                  );
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
    validateConfig("global", ConfigurationType.GLOBAL, new String(Files.readAllBytes(Paths.get(globalConfigFile.toURI()))));
    for(String sensor : sensors) {
      File sensorFile = new File(configDir, ConfigurationType.ENRICHMENT.getDirectory() + "/" + sensor + ".json");
      Assert.assertTrue(sensor + " config does not exist", sensorFile.exists());
      validateConfig(sensor, ConfigurationType.ENRICHMENT, new String(Files.readAllBytes(Paths.get(sensorFile.toURI()))));
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
      Assert.fail("Should have failed to pull configs in a directory structure that already exists.");
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
        Assert.fail("Unable to load config " + name + ": " + data);
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
        if(configurationType == ConfigurationType.GLOBAL) {
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

  @After
  public void tearDown() throws IOException {
    client.close();
    testZkServer.close();
    testZkServer.stop();
  }
}
