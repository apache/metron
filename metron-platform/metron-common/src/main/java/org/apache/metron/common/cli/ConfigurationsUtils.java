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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.*;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.zookeeper.KeeperException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationsUtils {

  public static CuratorFramework getClient(String zookeeperUrl) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    return CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
  }

  public static void writeGlobalConfigToZookeeper(Map<String, Object> globalConfig, String zookeeperUrl) throws Exception {
    writeGlobalConfigToZookeeper(JSONUtils.INSTANCE.toJSON(globalConfig), zookeeperUrl);
  }

  public static void writeGlobalConfigToZookeeper(byte[] globalConfig, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    try {
      writeGlobalConfigToZookeeper(globalConfig, client);
    }
    finally {
      client.close();
    }
  }

  public static void writeGlobalConfigToZookeeper(byte[] globalConfig, CuratorFramework client) throws Exception {
    writeToZookeeper(Constants.ZOOKEEPER_GLOBAL_ROOT, globalConfig, client);
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, SensorParserConfig sensorParserConfig, String zookeeperUrl) throws Exception {
    writeSensorParserConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSON(sensorParserConfig), zookeeperUrl);
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, byte[] configData, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    try {
      writeSensorParserConfigToZookeeper(sensorType, configData, client);
    }
    finally {
      client.close();
    }
  }

  public static void writeSensorParserConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    writeToZookeeper(Constants.ZOOKEEPER_PARSER_ROOT + "/" + sensorType, configData, client);
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, SensorEnrichmentConfig sensorEnrichmentConfig, String zookeeperUrl) throws Exception {
    writeSensorEnrichmentConfigToZookeeper(sensorType, JSONUtils.INSTANCE.toJSON(sensorEnrichmentConfig), zookeeperUrl);
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, byte[] configData, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    try {
      writeSensorEnrichmentConfigToZookeeper(sensorType, configData, client);
    }
    finally {
      client.close();
    }
  }

  public static void writeSensorEnrichmentConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    writeToZookeeper(Constants.ZOOKEEPER_ENRICHMENT_ROOT + "/" + sensorType, configData, client);
  }

  public static void writeConfigToZookeeper(String name, Map<String, Object> config, String zookeeperUrl) throws Exception {
    writeConfigToZookeeper(name, JSONUtils.INSTANCE.toJSON(config), zookeeperUrl);
  }

  public static void writeConfigToZookeeper(String name, byte[] config, String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    try {
      writeToZookeeper(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + name, config, client);
    }
    finally {
      client.close();
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
    List<String> sensorTypes = client.getChildren().forPath(Constants.ZOOKEEPER_PARSER_ROOT);
    for(String sensorType: sensorTypes) {
      configurations.updateSensorParserConfig(sensorType, readSensorParserConfigBytesFromZookeeper(sensorType, client));
    }
  }

  public static void updateEnrichmentConfigsFromZookeeper(EnrichmentConfigurations configurations, CuratorFramework client) throws Exception {
    updateConfigsFromZookeeper(configurations, client);
    List<String> sensorTypes = client.getChildren().forPath(Constants.ZOOKEEPER_ENRICHMENT_ROOT);
    for(String sensorType: sensorTypes) {
      configurations.updateSensorEnrichmentConfig(sensorType, readSensorEnrichmentConfigBytesFromZookeeper(sensorType, client));
    }
  }

  public static SensorEnrichmentConfig readSensorEnrichmentConfigFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(readFromZookeeper(Constants.ZOOKEEPER_ENRICHMENT_ROOT + "/" + sensorType, client)), SensorEnrichmentConfig.class);
  }

  public static SensorParserConfig readSensorParserConfigFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return JSONUtils.INSTANCE.load(new ByteArrayInputStream(readFromZookeeper(Constants.ZOOKEEPER_PARSER_ROOT + "/" + sensorType, client)), SensorParserConfig.class);
  }

  public static byte[] readGlobalConfigBytesFromZookeeper(CuratorFramework client) throws Exception {
    return readFromZookeeper(Constants.ZOOKEEPER_GLOBAL_ROOT, client);
  }

  public static byte[] readSensorParserConfigBytesFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return readFromZookeeper(Constants.ZOOKEEPER_PARSER_ROOT + "/" + sensorType, client);
  }

  public static byte[] readSensorEnrichmentConfigBytesFromZookeeper(String sensorType, CuratorFramework client) throws Exception {
    return readFromZookeeper(Constants.ZOOKEEPER_ENRICHMENT_ROOT + "/" + sensorType, client);
  }

  public static byte[] readConfigBytesFromZookeeper(String name, CuratorFramework client) throws Exception {
    return readFromZookeeper(Constants.ZOOKEEPER_TOPOLOGY_ROOT + "/" + name, client);
  }

  public static byte[] readFromZookeeper(String path, CuratorFramework client) throws Exception {
    return client.getData().forPath(path);
  }

  public static void uploadConfigsToZookeeper(String rootFilePath, String zookeeperUrl) throws Exception {
    uploadConfigsToZookeeper(rootFilePath, rootFilePath, rootFilePath, zookeeperUrl);
  }

  public static void uploadConfigsToZookeeper(String globalConfigPath, String parsersConfigPath, String enrichmentsConfigPath, String zookeeperUrl) throws Exception {
    if (globalConfigPath != null) {
      byte[] globalConfig = readGlobalConfigFromFile(globalConfigPath);
      if (globalConfig.length > 0) {
        ConfigurationsUtils.writeGlobalConfigToZookeeper(readGlobalConfigFromFile(globalConfigPath), zookeeperUrl);
      }
    }
    if (parsersConfigPath != null) {
      Map<String, byte[]> sensorParserConfigs = readSensorParserConfigsFromFile(parsersConfigPath);
      for (String sensorType : sensorParserConfigs.keySet()) {
        ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, sensorParserConfigs.get(sensorType), zookeeperUrl);
      }
    }
    if (enrichmentsConfigPath != null) {
      Map<String, byte[]> sensorEnrichmentConfigs = readSensorEnrichmentConfigsFromFile(enrichmentsConfigPath);
      for (String sensorType : sensorEnrichmentConfigs.keySet()) {
        ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, sensorEnrichmentConfigs.get(sensorType), zookeeperUrl);
      }
    }
  }

  public static byte[] readGlobalConfigFromFile(String rootPath) throws IOException {
    byte[] globalConfig = new byte[0];
    File configPath = new File(rootPath, Constants.GLOBAL_CONFIG_NAME + ".json");
    if (configPath.exists()) {
      globalConfig = Files.readAllBytes(configPath.toPath());
    }
    return globalConfig;
  }

  public static Map<String, byte[]> readSensorParserConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, ConfigType.PARSER);
  }

  public static Map<String, byte[]> readSensorEnrichmentConfigsFromFile(String rootPath) throws IOException {
    return readSensorConfigsFromFile(rootPath, ConfigType.ENRICHMENT);
  }

  public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath, ConfigType configType) throws IOException {
    Map<String, byte[]> sensorConfigs = new HashMap<>();
    File configPath = new File(rootPath, configType.getName());
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


  public static void dumpConfigs(String zookeeperUrl) throws Exception {
    CuratorFramework client = getClient(zookeeperUrl);
    client.start();
    //Output global configs
    {
      try {
        System.out.println("Global config at path " + Constants.ZOOKEEPER_GLOBAL_ROOT);
        byte[] globalConfigData = client.getData().forPath(Constants.ZOOKEEPER_GLOBAL_ROOT);
        System.out.println(new String(globalConfigData));
      } catch (KeeperException.NoNodeException e) {
        System.out.println("Global config is not configured");
        System.out.println();
      }
    }
    //Output sensor parser specific configs
    {
      try {
        List<String> children = client.getChildren().forPath(Constants.ZOOKEEPER_PARSER_ROOT);
        for (String child : children) {
          String path = Constants.ZOOKEEPER_PARSER_ROOT + "/" + child;
          byte[] data = client.getData().forPath(path);
          System.out.println("Parser config for sensor " + child + " at path " + path);
          System.out.println(new String(data));
          System.out.println();
        }
      } catch (KeeperException.NoNodeException e) {
        System.out.println("No parsers configured");
        System.out.println();
      }
    }
    //Output sensor enrichment specific configs
    {
      try {
        List<String> children = client.getChildren().forPath(Constants.ZOOKEEPER_ENRICHMENT_ROOT);
        for (String child : children) {
          String path = Constants.ZOOKEEPER_ENRICHMENT_ROOT + "/" + child;
          byte[] data = client.getData().forPath(path);
          System.out.println("Enrichment config for sensor " + child + " at path " + path);
          System.out.println(new String(data));
          System.out.println();
        }
      } catch (KeeperException.NoNodeException e) {
        System.out.println("No enrichments configured");
        System.out.println();
      }
    }
    client.close();
  }

  public static void main(String[] args) {

    Options options = new Options();
    {
      Option o = new Option("h", "help", false, "This screen");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("p", "config_files", true, "Path to the source config files.  Must be named like \"$source\".json");
      o.setArgName("DIR_NAME");
      o.setRequired(false);
      options.addOption(o);
    }
    {
      Option o = new Option("z", "zk", true, "Zookeeper Quroum URL (zk1:2181,zk2:2181,...");
      o.setArgName("ZK_QUORUM");
      o.setRequired(true);
      options.addOption(o);
    }

    try {
      CommandLineParser parser = new PosixParser();
      CommandLine cmd = null;
      try {
        cmd = parser.parse(options, args);
      } catch (ParseException pe) {
        pe.printStackTrace();
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("ConfigurationsUtils", null, options, null, true);
        System.exit(-1);
      }
      if (cmd.hasOption("h")) {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("ConfigurationsUtils", null, options, null, true);
        System.exit(0);
      }

      String zkQuorum = cmd.getOptionValue("z");
      if (cmd.hasOption("p")) {
        String sourcePath = cmd.getOptionValue("p");
        uploadConfigsToZookeeper(sourcePath, zkQuorum);
      }

      ConfigurationsUtils.dumpConfigs(zkQuorum);

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

  }
}
