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
package org.apache.metron.enrichment.integration.components;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.zookeeper.KeeperException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.apache.metron.common.configuration.ConfigurationsUtils.*;

public class ConfigUploadComponent implements InMemoryComponent {

  private String connectionString;
  private Properties topologyProperties;
  private String globalConfigPath;
  private String parserConfigsPath;
  private String enrichmentConfigsPath;
  private String indexingConfigsPath;
  private String profilerConfigPath;
  private Optional<Consumer<ConfigUploadComponent>> postStartCallback = Optional.empty();
  private Optional<String> globalConfig = Optional.empty();
  private Map<String, SensorParserConfig> parserSensorConfigs = new HashMap<>();

  public ConfigUploadComponent withConnectionString(String connectionString) {
    this.connectionString = connectionString;
    return this;
  }

  public ConfigUploadComponent withTopologyProperties(Properties topologyProperties) {
    this.topologyProperties = topologyProperties;
    return this;
  }

  public ConfigUploadComponent withGlobalConfigsPath(String globalConfigPath) {
    this.globalConfigPath = globalConfigPath;
    return this;
  }

  public ConfigUploadComponent withParserConfigsPath(String parserConfigsPath) {
    this.parserConfigsPath = parserConfigsPath;
    return this;
  }
  public ConfigUploadComponent withEnrichmentConfigsPath(String enrichmentConfigsPath) {
    this.enrichmentConfigsPath = enrichmentConfigsPath;
    return this;
  }

  public ConfigUploadComponent withIndexingConfigsPath(String indexingConfigsPath) {
    this.indexingConfigsPath = indexingConfigsPath;
    return this;
  }
  public ConfigUploadComponent withProfilerConfigsPath(String profilerConfigsPath) {
    this.profilerConfigPath = profilerConfigsPath;
    return this;
  }

  public ConfigUploadComponent withParserSensorConfig(String name, SensorParserConfig config) {
    parserSensorConfigs.put(name, config);
    return this;
  }

  public ConfigUploadComponent withGlobalConfig(String globalConfig) {
    this.globalConfig = Optional.ofNullable(globalConfig);
    return this;
  }

  public ConfigUploadComponent withPostStartCallback(Consumer<ConfigUploadComponent> f) {
        this.postStartCallback = Optional.ofNullable(f);
        return this;
  }

  public Properties getTopologyProperties() {
    return topologyProperties;
  }

  public String getGlobalConfigPath() {
    return globalConfigPath;
  }

  public String getParserConfigsPath() {
    return parserConfigsPath;
  }

  public String getEnrichmentConfigsPath() {
    return enrichmentConfigsPath;
  }

  public String getIndexingConfigsPath() {
    return indexingConfigsPath;
  }

  public String getProfilerConfigPath() {
    return profilerConfigPath;
  }

  public Optional<Consumer<ConfigUploadComponent>> getPostStartCallback() {
    return postStartCallback;
  }

  public Optional<String> getGlobalConfig() {
    return globalConfig;
  }

  public Map<String, SensorParserConfig> getParserSensorConfigs() {
    return parserSensorConfigs;
  }

  @Override
  public void start() throws UnableToStartException {
    update();
  }

  public void update() throws UnableToStartException {
    try {
      final String zookeeperUrl = connectionString == null?topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY):connectionString;

      if(globalConfigPath != null
      || parserConfigsPath != null
      || enrichmentConfigsPath != null
      || indexingConfigsPath != null
      || profilerConfigPath != null
        ) {
        uploadConfigsToZookeeper(globalConfigPath, parserConfigsPath, enrichmentConfigsPath, indexingConfigsPath, profilerConfigPath, zookeeperUrl);
      }

      for(Map.Entry<String, SensorParserConfig> kv : parserSensorConfigs.entrySet()) {
        writeSensorParserConfigToZookeeper(kv.getKey(), kv.getValue(), zookeeperUrl);
      }

      if(globalConfig.isPresent()) {
        writeGlobalConfigToZookeeper(globalConfig.get().getBytes(), zookeeperUrl);
      }
      if(postStartCallback.isPresent()) {
        postStartCallback.get().accept(this);
      }

    } catch (Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }



  public SensorParserConfig getSensorParserConfig(String sensorType) {
    SensorParserConfig sensorParserConfig = new SensorParserConfig();
    CuratorFramework client = getClient(topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY));
    client.start();
    try {
      sensorParserConfig = readSensorParserConfigFromZookeeper(sensorType, client);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      client.close();
    }
    return sensorParserConfig;
  }

  @Override
  public void stop() {

  }
}
