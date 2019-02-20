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

import com.google.common.base.Function;
import java.io.IOException;
import org.apache.curator.framework.CuratorFramework;

/**
 * Defines the types of configurations available and the associated {@link ConfigurationOperations}.
 */
public enum ConfigurationType implements Function<String, Object>, ConfigurationOperations {

  GLOBAL(new GlobalConfigurationOperations()),
  PARSER(new ParserConfigurationOperations()),
  ENRICHMENT(new EnrichmentConfigurationOperations()),
  INDEXING(new IndexingConfigurationOperations()),
  PROFILER(new ProfilerConfigurationOperations());

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

  /**
   * Deserializes a string according for the config type.
   *
   * @param s The string to be deserialized
   * @return The deserialized string
   * @throws RuntimeException If the string cannot be deserialized
   */
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
  public void writeSensorConfigToZookeeper(String sensorType, byte[] configData,
      CuratorFramework client) throws Exception {
    ops.writeSensorConfigToZookeeper(sensorType, configData, client);
  }

  public String getZookeeperRoot() {
    return ops.getZookeeperRoot();
  }

}
