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
package org.apache.metron.common.configuration;

import org.apache.commons.io.FilenameUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.zookeeper.KeeperException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.common.configuration.ConfigurationType.*;

public class ConfigurationsUtils {

  public static CuratorFramework getClient(String zookeeperUrl) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    return CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
  }

  public static void writeGlobalConfigToZookeeper(Map<String, Object> globalConfig, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
     client.start();
      writeGlobalConfigToZookeeper(globalConfig, client);
    }
  }
  public static void writeGlobalConfigToZookeeper(Map<String, Object> globalConfig, CuratorFramework client) throws Exception {
    writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSON(globalConfig), client);
  }

  public static void writeGlobalConfigToZookeeper(byte[] globalConfig, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeGlobalConfigToZookeeper(globalConfig, client);
    }
  }

  public static void writeGlobalConfigToZookeeper(byte[] globalConfig, CuratorFramework client) throws Exception {
    GLOBAL.deserialize(new String(globalConfig));
    writeToZookeeper(GLOBAL.getZookeeperRoot(), globalConfig, client);
  }

  public static void writeProfilerConfigToZookeeper(byte[] config, CuratorFramework client) throws Exception {
    PROFILER.deserialize(new String(config));
    writeToZookeeper(PROFILER.getZookeeperRoot(), config, client);
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, SensorParserConfig sensorParserConfig, String zookeeperUrl) throws Exception {
    writeSensorParserConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSON(sensorParserConfig), zookeeperUrl);
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, byte[] configData, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeSensorParserConfigToZookeeper(sensorType, configData, client);
    }
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    SensorParserConfig c = (SensorParserConfig) PARSER.deserialize(new String(configData));
    c.init();
    writeToZookeeper(PARSER.getZookeeperRoot() + "/" + sensorType, configData, client);
  }

  public static void writeSensorIndexingConfigToZookeeper(String sensorType, Map<String, Object> sensorIndexingConfig, String zookeeperUrl) throws Exception {
    writeSensorIndexingConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSON(sensorIndexingConfig), zookeeperUrl);
  }

  public static void writeSensorIndexingConfigToZookeeper(String sensorType, byte[] configData, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeSensorIndexingConfigToZookeeper(sensorType, configData, client);
    }
  }

  public static void writeSensorIndexingConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    INDEXING.deserialize(new String(configData));
    writeToZookeeper(INDEXING.getZookeeperRoot() + "/" + sensorType, configData, client);
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, SensorEnrichmentConfig sensorEnrichmentConfig, String zookeeperUrl) throws Exception {
    writeSensorEnrichmentConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSON(sensorEnrichmentConfig), zookeeperUrl);
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, byte[] configData, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeSensorEnrichmentConfigToZookeeper(sensorType, configData, client);
    }
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    ENRICHMENT.deserialize(new String(configData));
    writeToZookeeper(ENRICHMENT.getZookeeperRoot() + "/" + sensorType, configData, client);
  }

  public static void writeConfigToZookeeper(String name, Map<String, Object> config, String zookeeperUrl) throws Exception {
    writeConfigToZookeeper(name, JSONUtils.INSTANCE.toJSON(config), zookeeperUrl);
  }

  public static void writeConfigToZookeeper(String name, byte[] config, String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeToZookeeper(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + name, config, client);
    }
  }

  public static void writeToZookeeper(String path, byte[] configData, CuratorFramework client) throws Exception {
    try {
      client.setData().forPath(path, configData);
    } catch (KeeperException.NoNodeException e) {
      client.create().creatingParentsIfNeeded().forPath(path, configData);
    }
  }

  public static void updateConfigsFromZookeeper(Configurations configurations, CuratorFramework client) throws Exception {
    configurations.updateGlobalConfig(readGlobalConfigBytesFromZookeeper(client));
  }

  public static void updateParserConfigsFromZookeeper(ParserConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper(configurations, client);
    List<String> sensorTypes = client.getChildren().forPath(PARSER.getZookeeperRoot());
    for(String sensorType: sensorTypes) {
      configurations.updateSensorParserConfig(sensorType, readSensorParserConfigBytesFromZookeeper(sensorType, client));
    }
  }

  public static void updateSensorIndexingConfigsFromZookeeper(IndexingConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper(configurations, client);
    List<String> sensorTypes = client.getChildren().forPath(INDEXING.getZookeeperRoot());
    for(String sensorType: sensorTypes) {
      configurations.updateSensorIndexingConfig(sensorType, readSensorEnrichmentConfigBytesFromZookeeper(sensorType, client));
    }
  }

  public static void updateEnrichmentConfigsFromZookeeper(EnrichmentConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper(configurations, client);
    List<String> sensorTypes = client.getChildren().forPath(ENRICHMENT.getZookeeperRoot());
    for(String sensorType: sensorTypes) {
      configurations.updateSensorEnrichmentConfig(sensorType, readSensorEnrichmentConfigBytesFromZookeeper(sensorType, client));
    }
  }

  public static SensorEnrichmentConfig readSensorEnrichmentConfigFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(readFromZookeeper(ENRICHMENT.getZookeeperRoot() + "/" + sensorType, client)), SensorEnrichmentConfig.class);
  }

  public static SensorParserConfig readSensorParserConfigFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(readFromZookeeper(PARSER.getZookeeperRoot() + "/" + sensorType, client)), SensorParserConfig.class);
  }

  public static byte[] readGlobalConfigBytesFromZookeeper(CuratorFramework client) throws Exception {
    return readFromZookeeper(GLOBAL.getZookeeperRoot(), client);
  }

  public static byte[] readProfilerConfigBytesFromZookeeper(CuratorFramework client) throws Exception {
    return readFromZookeeper(PROFILER.getZookeeperRoot(), client);
  }

  public static byte[] readSensorIndexingConfigBytesFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return readFromZookeeper(INDEXING.getZookeeperRoot() + "/" + sensorType, client);
  }

  public static byte[] readSensorParserConfigBytesFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return readFromZookeeper(PARSER.getZookeeperRoot() + "/" + sensorType, client);
  }

  public static byte[] readSensorEnrichmentConfigBytesFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return readFromZookeeper(ENRICHMENT.getZookeeperRoot() + "/" + sensorType, client);
  }

  public static byte[] readConfigBytesFromZookeeper(String name, CuratorFramework client) throws Exception {
    return readFromZookeeper(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + name, client);
  }

  public static byte[] readFromZookeeper(String path, CuratorFramework client) throws Exception {
    if(client != null && client.getData() != null && path != null) {
      return client.getData().forPath(path);
    }
    return new byte[]{};
  }

  public static void uploadConfigsToZookeeper(String globalConfigPath,
                                              String parsersConfigPath,
                                              String enrichmentsConfigPath,
                                              String indexingConfigPath,
                                              String profilerConfigPath,
                                              String zookeeperUrl) throws Exception {
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      uploadConfigsToZookeeper(globalConfigPath, parsersConfigPath, enrichmentsConfigPath, indexingConfigPath, profilerConfigPath, client);
    }
  }

  public static void uploadConfigsToZookeeper(String rootFilePath, CuratorFramework client) throws Exception {
    uploadConfigsToZookeeper(rootFilePath, rootFilePath, rootFilePath, rootFilePath, rootFilePath, client);
  }

  public static void uploadConfigsToZookeeper(String globalConfigPath,
                                              String parsersConfigPath,
                                              String enrichmentsConfigPath,
                                              String indexingConfigPath,
                                              String profilerConfigPath,
                                              CuratorFramework client) throws Exception {

    // global
    if (globalConfigPath != null) {
      final byte[] globalConfig = readGlobalConfigFromFile(globalConfigPath);
      if (globalConfig.length > 0) {
        setupStellarStatically(client, Optional.of(new String(globalConfig)));
        ConfigurationsUtils.writeGlobalConfigToZookeeper(readGlobalConfigFromFile(globalConfigPath), client);
      }
    }

    // parsers
    if (parsersConfigPath != null) {
      Map<String, byte[]> sensorParserConfigs = readSensorParserConfigsFromFile(parsersConfigPath);
      for (String sensorType : sensorParserConfigs.keySet()) {
        ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), client);
      }
    }

    // indexing
    if (indexingConfigPath != null) {
      Map<String, byte[]> sensorIndexingConfigs = readSensorIndexingConfigsFromFile(indexingConfigPath);
      for (String sensorType : sensorIndexingConfigs.keySet()) {
        ConfigurationsUtils.writeSensorIndexingConfigToZookeeper(sensorType, sensorIndexingConfigs.get(sensorType), client);
      }
    }

    // enrichments
    if (enrichmentsConfigPath != null) {
      Map<String, byte[]> sensorEnrichmentConfigs = readSensorEnrichmentConfigsFromFile(enrichmentsConfigPath);
      for (String sensorType : sensorEnrichmentConfigs.keySet()) {
        ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), client);
      }
    }

    // profiler
    if (profilerConfigPath != null) {
      byte[] profilerConfig = readProfilerConfigFromFile(profilerConfigPath);
      if (profilerConfig.length > 0) {
        ConfigurationsUtils.writeProfilerConfigToZookeeper(profilerConfig, client);
      }
    }
  }

  public static void setupStellarStatically(CuratorFramework client) throws Exception {
    byte[] ret = null;
    try {
      ret = readGlobalConfigBytesFromZookeeper(client);
    }
    catch(KeeperException.NoNodeException nne) {
      //can't find the node
    }
    if(ret == null || ret.length == 0) {
      setupStellarStatically(client, Optional.empty());
    }
    else {
      setupStellarStatically(client, Optional.of(new String(ret)));
    }
  }

  public static void setupStellarStatically(CuratorFramework client, Optional<String> globalConfig) {
    /*
      In order to validate stellar functions, the function resolver must be initialized.  Otherwise,
      those utilities that require validation cannot validate the stellar expressions necessarily.
    */
    Context.Builder builder = new Context.Builder().with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
            ;
    if(globalConfig.isPresent()) {
      builder = builder.with(Context.Capabilities.GLOBAL_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()))
              .with(Context.Capabilities.STELLAR_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()));
    }
    else {
      builder = builder.with(Context.Capabilities.STELLAR_CONFIG, () -> new HashMap<>());
    }
    Context stellarContext = builder.build();
    StellarFunctions.FUNCTION_RESOLVER().initialize(stellarContext);
  }

  public static byte[] readGlobalConfigFromFile(String rootPath) throws IOException {
    byte[] globalConfig = new byte[0];
    File configPath = new File(rootPath, GLOBAL.getName() + ".json");
    if (configPath.exists()) {
      globalConfig = Files.readAllBytes(configPath.toPath());
    }
    return globalConfig;
  }

  public static Map<String, byte[]> readSensorParserConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, PARSER);
  }

  public static Map<String, byte[]> readSensorEnrichmentConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, ENRICHMENT);
  }

  public static Map<String, byte[]> readSensorIndexingConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, INDEXING);
  }

  /**
   * Read the Profiler configuration from a file.  There is only a single profiler configuration.
   * @param rootPath Path to the Profiler configuration.
   */
  public static byte[] readProfilerConfigFromFile(String rootPath) throws IOException {

    byte[] config = new byte[0];
    File configPath = new File(rootPath, PROFILER.getName() + ".json");
    if (configPath.exists()) {
      config = Files.readAllBytes(configPath.toPath());
    }

    return config;
  }

  public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath, ConfigurationType configType) throws IOException {
    Map<String, byte[]> sensorConfigs = new HashMap<>();
    File configPath = new File(rootPath, configType.getDirectory());
    if (configPath.exists()) {
      File[] children = configPath.listFiles();
      if (children != null) {
        for (File file : children) {
          sensorConfigs.put(FilenameUtils.removeExtension(file.getName()), Files.readAllBytes(file.toPath()));
        }
      }
    }
    return sensorConfigs;
  }


  public interface ConfigurationVisitor{
    void visit(ConfigurationType configurationType, String name, String data);
  }

  public static void visitConfigs(CuratorFramework client, final ConfigurationVisitor callback) throws Exception {
    visitConfigs(client, (type, name, data) -> {
      setupStellarStatically(client, Optional.ofNullable(data));
      callback.visit(type, name, data);
    }, GLOBAL);
    visitConfigs(client, callback, PARSER);
    visitConfigs(client, callback, INDEXING);
    visitConfigs(client, callback, ENRICHMENT);
    visitConfigs(client, callback, PROFILER);
  }

  public static void visitConfigs(CuratorFramework client, ConfigurationVisitor callback, ConfigurationType configType) throws Exception {

    if (client.checkExists().forPath(configType.getZookeeperRoot()) != null) {

      if (configType.equals(GLOBAL)) {
        byte[] globalConfigData = client.getData().forPath(configType.getZookeeperRoot());
        callback.visit(configType, "global", new String(globalConfigData));
      }
      else if(configType.equals(PROFILER)) {
        byte[] profilerConfigData = client.getData().forPath(configType.getZookeeperRoot());
        callback.visit(configType, "profiler", new String(profilerConfigData));
      }
      else if (configType.equals(PARSER) || configType.equals(ENRICHMENT) || configType.equals(INDEXING)) {
        List<String> children = client.getChildren().forPath(configType.getZookeeperRoot());
        for (String child : children) {

          byte[] data = client.getData().forPath(configType.getZookeeperRoot() + "/" + child);
          callback.visit(configType, child, new String(data));
        }
      }
    }
  }

  public static void dumpConfigs(PrintStream out, CuratorFramework client) throws Exception {
    ConfigurationsUtils.visitConfigs(client, (type, name, data) -> {
      type.deserialize(data);
      out.println(type + " Config: " + name + "\n" + data);
    });
  }
}
