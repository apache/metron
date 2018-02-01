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

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.common.configuration.ConfigurationType.GLOBAL;
import static org.apache.metron.common.configuration.ConfigurationType.INDEXING;
import static org.apache.metron.common.configuration.ConfigurationType.PARSER;
import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.io.FilenameUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationsUtils {
  protected static final Logger LOG =  LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSONPretty(globalConfig), client);
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
    writeSensorParserConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSONPretty(sensorParserConfig), zookeeperUrl);
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
    writeSensorIndexingConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSONPretty(sensorIndexingConfig), zookeeperUrl);
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
    writeSensorEnrichmentConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSONPretty(sensorEnrichmentConfig), zookeeperUrl);
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
    writeConfigToZookeeper(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + name, JSONUtils.INSTANCE.toJSONPretty(config), zookeeperUrl);
  }

  public static void writeConfigToZookeeper(ConfigurationType configType, byte[] configData,
      String zookeeperUrl) throws Exception {
    writeConfigToZookeeper(configType, Optional.empty(), configData, zookeeperUrl);
  }

  public static void writeConfigToZookeeper(ConfigurationType configType,
      Optional<String> configName, byte[] configData, String zookeeperUrl) throws Exception {
    writeConfigToZookeeper(getConfigZKPath(configType, configName), configData, zookeeperUrl);
  }

  public static void writeConfigToZookeeper(ConfigurationType configType,Optional<String> configName,
      byte[] configData, CuratorFramework client) throws Exception {
    writeToZookeeper(getConfigZKPath(configType, configName), configData, client);
  }

  private static String getConfigZKPath(ConfigurationType configType, Optional<String> configName) {
    String pathSuffix = configName.isPresent() && configType != GLOBAL ? "/" + configName.get() : "";
    return configType.getZookeeperRoot() + pathSuffix;
  }

  /**
   * Writes config to path in Zookeeper, /metron/topology/$CONFIG_TYPE/$CONFIG_NAME
   */
  public static void writeConfigToZookeeper(String configPath, byte[] config, String zookeeperUrl)
      throws Exception {
    try (CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      writeToZookeeper(configPath, config, client);
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


  private interface Callback {
    void apply(String sensorType) throws Exception;
  }

  private static void updateConfigsFromZookeeper( Configurations configurations
                                                , ConfigurationType type
                                                , Callback callback
                                                , CuratorFramework client
                                                )  throws Exception
  {
    Exception globalUpdateException = null;
    try {
      updateConfigsFromZookeeper(configurations, client);
    }
    catch(Exception e) {
      LOG.warn("Unable to update global config when updating indexing configs: " + e.getMessage(), e);
      globalUpdateException = e;
    }
    List<String> sensorTypes = client.getChildren().forPath(type.getZookeeperRoot());
    for(String sensorType: sensorTypes) {
      callback.apply(sensorType);
    }
    if(globalUpdateException != null) {
      throw globalUpdateException;
    }
  }

  public static void updateParserConfigsFromZookeeper(ParserConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper( configurations
                              , PARSER
                              , sensorType -> configurations.updateSensorParserConfig(sensorType, readSensorParserConfigBytesFromZookeeper(sensorType, client))
                              , client
                              );
  }

  public static void updateSensorIndexingConfigsFromZookeeper(IndexingConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper( configurations
                              , INDEXING
                              , sensorType -> configurations.updateSensorIndexingConfig(sensorType, readSensorIndexingConfigBytesFromZookeeper(sensorType, client))
                              , client
                              );
  }

  public static void updateEnrichmentConfigsFromZookeeper(EnrichmentConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper( configurations
                              , ENRICHMENT
                              , sensorType -> configurations.updateSensorEnrichmentConfig(sensorType, readSensorEnrichmentConfigBytesFromZookeeper(sensorType, client))
                              , client
                              );
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

  public static byte[] readConfigBytesFromZookeeper(ConfigurationType configType,
      String zookeeperUrl) throws Exception {
    return readConfigBytesFromZookeeper(configType, Optional.empty(), zookeeperUrl);
  }

  public static byte[] readConfigBytesFromZookeeper(ConfigurationType configType, Optional<String> configName,
      CuratorFramework client) throws Exception {
    return readFromZookeeper(getConfigZKPath(configType, configName), client);
  }

  public static byte[] readConfigBytesFromZookeeper(ConfigurationType configType, Optional<String> configName,
      String zookeeperUrl) throws Exception {
    return readFromZookeeper(getConfigZKPath(configType, configName), zookeeperUrl);
  }

  public static byte[] readFromZookeeper(String path, String zookeeperUrl) throws Exception {
    try (CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      return readFromZookeeper(path, client);
    }
  }

  public static byte[] readFromZookeeper(String path, CuratorFramework client) throws Exception {
    if (client != null && client.getData() != null && path != null) {
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
    try (CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      uploadConfigsToZookeeper(globalConfigPath, parsersConfigPath, enrichmentsConfigPath, indexingConfigPath, profilerConfigPath, client);
    }
  }

  public static void uploadConfigsToZookeeper(String rootFilePath, CuratorFramework client) throws Exception {
    uploadConfigsToZookeeper(rootFilePath, rootFilePath, rootFilePath, rootFilePath, rootFilePath, client);
  }

  /**
   * Uploads config to Zookeeper based on the specified rootPath and configuration type. The local
   * file and zookeeper paths are dynamically calculated based on the rootPath and config type.
   * When grabbing files from the local FS, the rootPath is used. When reading/writing to Zookeeper,
   * the path returned by
   * {@link org.apache.metron.common.configuration.ConfigurationType#getZookeeperRoot()} is used.
   * For example, when grabbing GLOBAL config from the local FS, the path is based on 'rootPath/.'
   * whereas PARSER would be based on 'rootPath/parsers'.
   *
   * @param rootFilePath base configuration path on the local FS
   * @param client zk client
   * @param type config type to upload configs for
   */
  public static void uploadConfigsToZookeeper(String rootFilePath, CuratorFramework client,
      ConfigurationType type) throws Exception {
    uploadConfigsToZookeeper(rootFilePath, client, type, Optional.empty());
  }

  /**
   * Does the same as
   * {@link org.apache.metron.common.configuration.ConfigurationsUtils#uploadConfigsToZookeeper(
   * java.lang.String, org.apache.curator.framework.CuratorFramework,
   * org.apache.metron.common.configuration.ConfigurationType)}
   * with the addition of being able to specify a specific config name for the given configuration
   * type. e.g. config type=PARSER, config name=bro
   *
   * @param rootFilePath base configuration path on the local FS
   * @param client zk client
   * @param type config type to upload configs for
   * @param configName specific config under the specified config type
   */
  public static void uploadConfigsToZookeeper(
          String rootFilePath,
          CuratorFramework client,
          ConfigurationType type,
          Optional<String> configName) throws Exception {

    switch (type) {

      case GLOBAL:
        final byte[] globalConfig = readGlobalConfigFromFile(rootFilePath);
        if (globalConfig.length > 0) {
          setupStellarStatically(client, Optional.of(new String(globalConfig)));
          writeGlobalConfigToZookeeper(globalConfig, client);
        }
        break;

      case PARSER: //pass through intentional
      case ENRICHMENT: //pass through intentional
      case INDEXING: //pass through intentional
      {
        Map<String, byte[]> configs = readSensorConfigsFromFile(rootFilePath, type, configName);
        for (String sensorType : configs.keySet()) {
          byte[] configData = configs.get(sensorType);
          type.writeSensorConfigToZookeeper(sensorType, configData, client);
        }
        break;
      }

      case PROFILER: {
        byte[] configData = readProfilerConfigFromFile(rootFilePath);
        if (configData.length > 0) {
          ConfigurationsUtils.writeProfilerConfigToZookeeper(configData, client);
        }
        break;
      }

      default:
        throw new IllegalArgumentException("Configuration type not found: " + type);
    }
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
    Context.Builder builder = new Context.Builder()
            .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client);

    if(globalConfig.isPresent()) {
      builder = builder
              .with(Context.Capabilities.GLOBAL_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()))
              .with(Context.Capabilities.STELLAR_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()));
    } else {
      builder = builder
              .with(Context.Capabilities.STELLAR_CONFIG, () -> new HashMap<>());
    }
    Context stellarContext = builder.build();
    StellarFunctions.FUNCTION_RESOLVER().initialize(stellarContext);
  }

  public static byte[] readGlobalConfigFromFile(String rootPath) throws IOException {
    byte[] globalConfig = new byte[0];
    File configPath = new File(rootPath, GLOBAL.getTypeName() + ".json");
    if (configPath.exists()) {
      globalConfig = Files.readAllBytes(configPath.toPath());
    }
    return globalConfig;
  }

  public static Map<String, byte[]> readSensorParserConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, PARSER, Optional.empty());
  }

  public static Map<String, byte[]> readSensorEnrichmentConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, ENRICHMENT, Optional.empty());
  }

  public static Map<String, byte[]> readSensorIndexingConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, INDEXING, Optional.empty());
  }

  /**
   * Read the Profiler configuration from a file.  There is only a single profiler configuration.
   * @param rootPath Path to the Profiler configuration.
   */
  public static byte[] readProfilerConfigFromFile(String rootPath) throws IOException {

    byte[] config = new byte[0];
    File configPath = new File(rootPath, PROFILER.getTypeName() + ".json");
    if (configPath.exists()) {
      config = Files.readAllBytes(configPath.toPath());
    }

    return config;
  }

  public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath, ConfigurationType configType) throws IOException {
    return readSensorConfigsFromFile(rootPath, configType, Optional.empty());
  }

  /**
   * Will read configs from local disk at the specified rootPath. Will read all configs for a given
   * configuration type. If an optional specific config name is also provided, it will only read
   * configs for that configuration type and name combo. e.g. PARSER, bro
   * @param rootPath root FS location to read configs from
   * @param configType e.g. GLOBAL, PARSER, ENRICHMENT, etc.
   * @param configName a specific config, for instance a sensor name like bro, yaf, snort, etc.
   * @return map of file names to the contents of that file as a byte array
   * @throws IOException
   */
  public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath,
      ConfigurationType configType, Optional<String> configName) throws IOException {
    Map<String, byte[]> sensorConfigs = new HashMap<>();
    File configPath = new File(rootPath, configType.getDirectory());
    if (configPath.exists() && configPath.isDirectory()) {
      File[] children = configPath.listFiles();
      if (!configName.isPresent()) {
        for (File file : children) {
          sensorConfigs.put(FilenameUtils.removeExtension(file.getName()),
              Files.readAllBytes(file.toPath()));
        }
      } else {
        for (File file : children) {
          if (FilenameUtils.removeExtension(file.getName()).equals(configName.get())) {
            sensorConfigs.put(FilenameUtils.removeExtension(file.getName()),
                Files.readAllBytes(file.toPath()));
          }
        }
        if (sensorConfigs.isEmpty()) {
          throw new RuntimeException("Unable to find configuration for " + configName.get());
        }
      }
    }
    return sensorConfigs;
  }

  /**
   * Reads Json data for the specified config type from zookeeper,
   * applies the patch from patchData, and writes it back to Zookeeper in a pretty print format.
   * Patching JSON flattens existing formatting, so this will keep configs readable.
   * Starts up curatorclient based on zookeeperUrl.
   *
   * @param configurationType GLOBAL, PARSER, etc.
   * @param patchData a JSON patch in the format specified by RFC 6902
   * @param zookeeperUrl configs are here
   */
  public static void applyConfigPatchToZookeeper(ConfigurationType configurationType,
      byte[] patchData, String zookeeperUrl) throws Exception {
    applyConfigPatchToZookeeper(configurationType, Optional.empty(), patchData, zookeeperUrl);
  }

  /**
   * Reads Json data for the specified config type and config name (if applicable) from zookeeper,
   * applies the patch from patchData, and writes it back to Zookeeper in a pretty print format.
   * Patching JSON flattens existing formatting, so this will keep configs readable.
   * Starts up curatorclient based on zookeeperUrl.
   *
   * @param configurationType GLOBAL, PARSER, etc.
   * @param configName e.g. bro, yaf, snort
   * @param patchData a JSON patch in the format specified by RFC 6902
   * @param zookeeperUrl configs are here
   */
  public static void applyConfigPatchToZookeeper(ConfigurationType configurationType,
      Optional<String> configName, byte[] patchData, String zookeeperUrl) throws Exception {
    try (CuratorFramework client = getClient(zookeeperUrl)) {
      client.start();
      applyConfigPatchToZookeeper(configurationType, configName, patchData, client);
    }
  }

  /**
   * Reads Json data for the specified config type and config name (if applicable) from zookeeper,
   * applies the patch from patchData, and writes it back to Zookeeper in a pretty print format.
   * Patching JSON flattens existing formatting, so this will keep configs readable. The
   * curatorclient should be started already.
   *
   * @param configurationType GLOBAL, PARSER, etc.
   * @param configName e.g. bro, yaf, snort
   * @param patchData a JSON patch in the format specified by RFC 6902
   * @param client access to zookeeeper
   */
  public static void applyConfigPatchToZookeeper(
          ConfigurationType configurationType,
          Optional<String> configName,
          byte[] patchData, CuratorFramework client) throws Exception {

    byte[] configData = readConfigBytesFromZookeeper(configurationType, configName, client);
    byte[] prettyPatchedConfig = JSONUtils.INSTANCE.applyPatch(patchData, configData);

    // ensure the patch produces a valid result; otherwise exception thrown during deserialization
    String prettyPatchedConfigStr = new String(prettyPatchedConfig);
    configurationType.deserialize(prettyPatchedConfigStr);

    writeConfigToZookeeper(configurationType, configName, prettyPatchedConfig, client);
  }

  public interface ConfigurationVisitor{
    void visit(ConfigurationType configurationType, String name, String data);
  }

  public static void visitConfigs(CuratorFramework client, final ConfigurationVisitor callback) throws Exception {
    visitConfigs(client, (type, name, data) -> {
      setupStellarStatically(client, Optional.ofNullable(data));
      callback.visit(type, name, data);
    }, GLOBAL, Optional.empty());
    visitConfigs(client, callback, PARSER, Optional.empty());
    visitConfigs(client, callback, INDEXING, Optional.empty());
    visitConfigs(client, callback, ENRICHMENT, Optional.empty());
    visitConfigs(client, callback, PROFILER, Optional.empty());
  }

  public static void visitConfigs(CuratorFramework client, ConfigurationVisitor callback, ConfigurationType configType, Optional<String> configName) throws Exception {

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
        if (configName.isPresent()) {
          byte[] data = readConfigBytesFromZookeeper(configType, configName,  client);
          callback.visit(configType, configName.get(), new String(data));
        } else {
          List<String> children = client.getChildren().forPath(configType.getZookeeperRoot());
          for (String child : children) {
            byte[] data = client.getData().forPath(configType.getZookeeperRoot() + "/" + child);
            callback.visit(configType, child, new String(data));
          }
        }
      }
    }
  }

  /**
   * Writes all config content to the provided print stream.
   *
   * @param out stream to use as output
   * @param client zk client
   * @throws Exception
   */
  public static void dumpConfigs(PrintStream out, CuratorFramework client) throws Exception {
    ConfigurationsUtils.visitConfigs(client, (type, name, data) -> {
      type.deserialize(data);
      out.println(type + " Config: " + name + System.lineSeparator() + data);
    });
  }

  /**
   * Writes config content for a specific config type to the provided print stream. Optionally
   * provide a config name in addition to the config type and it will only print the json for a
   * specific config, e.g. bro, yaf, snort, etc.
   *
   * @param out stream to use as output
   * @param client zk client
   * @param configType GLOBAL, PARSER, ENRICHMENT, etc.
   * @param configName Typically a sensor name like bro, snort, yaf, etc.
   * @throws Exception
   */
  public static void dumpConfigs(PrintStream out, CuratorFramework client,
      ConfigurationType configType, Optional<String> configName) throws Exception {
    ConfigurationsUtils.visitConfigs(client, (type, name, data) -> {
      setupStellarStatically(client, Optional.ofNullable(data));
      type.deserialize(data);
      out.println(type + " Config: " + name + System.lineSeparator() + data);
    }, configType, configName);
  }
}

