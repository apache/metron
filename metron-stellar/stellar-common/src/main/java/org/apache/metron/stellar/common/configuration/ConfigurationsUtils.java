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
package org.apache.metron.stellar.common.configuration;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.stellar.common.Constants;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.stellar.common.configuration.ConfigurationType.GLOBAL;

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
    GLOBAL.deserialize(new String(globalConfig, StandardCharsets.UTF_8));
    writeToZookeeper(GLOBAL.getZookeeperRoot(), globalConfig, client);
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

  public static byte[] readGlobalConfigBytesFromZookeeper(CuratorFramework client) throws Exception {
    return readFromZookeeper(GLOBAL.getZookeeperRoot(), client);
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

  public static void setupStellarStatically(CuratorFramework client) throws Exception {
    byte[] ret = null;
    try {
      ret = readGlobalConfigBytesFromZookeeper(client);
    } catch (KeeperException.NoNodeException nne) {
      //can't find the node
    }
    if (ret == null || ret.length == 0) {
      setupStellarStatically(client, Optional.empty());
    } else {
      setupStellarStatically(client, Optional.of(new String(ret, StandardCharsets.UTF_8)));
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

  public interface ConfigurationVisitor {
    void visit(ConfigurationType configurationType, String name, String data);
  }

  public static void visitConfigs(CuratorFramework client, final ConfigurationVisitor callback) throws Exception {
    visitConfigs(client, (type, name, data) -> {
      setupStellarStatically(client, Optional.ofNullable(data));
      callback.visit(type, name, data);
    }, GLOBAL);
  }

  public static void visitConfigs(CuratorFramework client, ConfigurationVisitor callback, ConfigurationType configType) throws Exception {

    if (client.checkExists().forPath(configType.getZookeeperRoot()) != null) {

      if (configType.equals(GLOBAL)) {
        byte[] globalConfigData = client.getData().forPath(configType.getZookeeperRoot());
        callback.visit(configType, "global", new String(globalConfigData, StandardCharsets.UTF_8));
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
