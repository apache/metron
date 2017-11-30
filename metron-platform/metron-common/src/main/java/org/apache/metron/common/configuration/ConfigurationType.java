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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.Map;

public enum ConfigurationType implements Function<String, Object>, ConfigurationOperations {

  GLOBAL(new ConfigurationOperations() {
    @Override
    public String getTypeName() {
      return "global";
    }

    @Override
    public String getDirectory() {
      return ".";
    }

    @Override
    public Object deserialize(String s) throws IOException {
     return JSONUtils.INSTANCE.load(s, new TypeReference<Map<String, Object>>() { });

    }

    @Override
    public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) {
      throw new UnsupportedOperationException("Global configs are not per-sensor");
    }
  })
  ,

  PARSER(new ConfigurationOperations() {
    @Override
    public String getTypeName() {
      return "parsers";
    }

    @Override
    public Object deserialize(String s) throws IOException {
      return JSONUtils.INSTANCE.load(s, SensorParserConfig.class);
    }

    @Override
    public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
      ConfigurationsUtils.writeSensorParserConfigToZookeeper(sensorType, configData, client);
    }
  }
  ),

  ENRICHMENT(new ConfigurationOperations() {
    @Override
    public String getTypeName() {
      return "enrichments";
    }

    @Override
    public Object deserialize(String s) throws IOException {
      return JSONUtils.INSTANCE.load(s, SensorEnrichmentConfig.class);
    }

    @Override
    public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
      ConfigurationsUtils.writeSensorEnrichmentConfigToZookeeper(sensorType, configData, client);
    }
  }
          ),
  INDEXING(new ConfigurationOperations() {
    @Override
    public String getTypeName() {
      return "indexing";
    }

    @Override
    public Object deserialize(String s) throws IOException {
      return JSONUtils.INSTANCE.load(s, new TypeReference<Map<String, Object>>() { });
    }

    @Override
    public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
      ConfigurationsUtils.writeSensorIndexingConfigToZookeeper(sensorType, configData, client);
    }
  }
          ),
  PROFILER(new ConfigurationOperations() {

    @Override
    public String getTypeName() {
      return "profiler";
    }

    @Override
    public String getDirectory() {
      return ".";
    }

    @Override
    public Object deserialize(String s) throws IOException {
      return JSONUtils.INSTANCE.load(s, ProfilerConfig.class);
    }

    @Override
    public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
      throw new UnsupportedOperationException("Profiler configs are not per-sensor");
    }
  });

  ConfigurationOperations ops;

  ConfigurationType(ConfigurationOperations ops) {
    this.ops = ops;
  }

  public String getTypeName() {
    return ops.getTypeName();
  }

  public String getDirectory() {
    return ops.getDirectory();
  }

  public Object deserialize(String s) {
    try {
      return ops.deserialize(s);
    } catch (IOException e) {
      throw new RuntimeException("Unable to load " + s, e);
    }
  }

  @Override
  public Object apply(String s) {
    return deserialize(s);
  }

  @Override
  public void writeSensorConfigToZookeeper(String sensorType, byte[] configData, CuratorFramework client) throws Exception {
    ops.writeSensorConfigToZookeeper(sensorType, configData, client);
  }
  public String getZookeeperRoot() {
    return ops.getZookeeperRoot();
  }

}
