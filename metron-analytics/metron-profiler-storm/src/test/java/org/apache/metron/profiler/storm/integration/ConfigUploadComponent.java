/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.profiler.storm.integration;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.ZKServerComponent;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.apache.metron.common.configuration.ConfigurationsUtils.*;


/**
 * Uploads configuration to Zookeeper.
 */
public class ConfigUploadComponent implements InMemoryComponent {

  private Properties topologyProperties;
  private String globalConfiguration;
  private String profilerConfigurationPath;
  private ProfilerConfig profilerConfig;

  @Override
  public void start() throws UnableToStartException {
    try {
      upload();
    } catch (Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public void update()
      throws UnableToStartException {
    try {
      upload();
    } catch (Exception e) {
      throw new UnableToStartException(e.getMessage(), e);
    }
  }

  /**
   * Uploads configuration to Zookeeper.
   * @throws Exception
   */
  private void upload() throws Exception {
    final String zookeeperUrl = topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY);
    try(CuratorFramework client = getClient(zookeeperUrl)) {
      if(client.getState() != CuratorFrameworkState.STARTED) {
        client.start();
      }
      uploadGlobalConfig(client);
      uploadProfilerConfig(client);
    }
  }

  /**
   * Upload the profiler configuration to Zookeeper.
   * @param client The zookeeper client.
   */
  private void uploadProfilerConfig(CuratorFramework client) throws Exception {
    byte[] configBytes = null;

    if (profilerConfigurationPath != null) {
      configBytes = readProfilerConfigFromFile(profilerConfigurationPath);

    } else if(profilerConfig != null) {
      configBytes = profilerConfig.toJSON().getBytes(StandardCharsets.UTF_8);
    }

    if (ArrayUtils.getLength(configBytes) > 0) {
      writeProfilerConfigToZookeeper(configBytes, client);
    }
  }

  /**
   * Upload the global configuration to Zookeeper.
   * @param client The zookeeper client.
   */
  private void uploadGlobalConfig(CuratorFramework client) throws Exception {
    if (globalConfiguration != null) {
      byte[] globalConfig = readGlobalConfigFromFile(globalConfiguration);
      if (globalConfig.length > 0) {
        writeGlobalConfigToZookeeper(readGlobalConfigFromFile(globalConfiguration), client);
      }
    }
  }

  public ConfigUploadComponent withTopologyProperties(Properties topologyProperties) {
    this.topologyProperties = topologyProperties;
    return this;
  }

  public ConfigUploadComponent withGlobalConfiguration(String path) {
    this.globalConfiguration = path;
    return this;
  }

  public ConfigUploadComponent withProfilerConfigurationPath(String path) {
    this.profilerConfigurationPath = path;
    return this;
  }

  public ConfigUploadComponent withProfilerConfiguration(ProfilerConfig profilerConfig) {
    this.profilerConfig = profilerConfig;
    return this;
  }
}
