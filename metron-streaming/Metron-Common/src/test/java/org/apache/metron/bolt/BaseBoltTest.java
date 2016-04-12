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
package org.apache.metron.bolt;

import org.apache.curator.test.TestingServer;
import org.apache.metron.Constants;
import org.apache.metron.utils.ConfigurationsUtils;
import org.junit.Before;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class BaseBoltTest {

  public String sampleConfigRoot = "../Metron-Testing/src/main/resources/sample/config/";
  protected String zookeeperUrl;
  protected Set<String> allConfigurationTypes = new HashSet<>();

  @Before
  public void setupConfiguration() throws Exception {
    TestingServer testZkServer = new TestingServer(true);
    this.zookeeperUrl = testZkServer.getConnectString();
    byte[] globalConfig = ConfigurationsUtils.readGlobalConfigFromFile(sampleConfigRoot);
    ConfigurationsUtils.writeGlobalConfigToZookeeper(globalConfig, zookeeperUrl);
    allConfigurationTypes.add(Constants.GLOBAL_CONFIG_NAME);
    Map<String, byte[]> sensorEnrichmentConfigs = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(sampleConfigRoot);
    for (String sensorType : sensorEnrichmentConfigs.keySet()) {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
      allConfigurationTypes.add(sensorType);
    }
  }
}
