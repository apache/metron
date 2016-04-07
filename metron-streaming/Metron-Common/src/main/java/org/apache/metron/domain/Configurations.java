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
package org.apache.metron.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.log4j.Logger;
import org.apache.metron.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Configurations implements Serializable {

  private static final Logger LOG = Logger.getLogger(Configurations.class);

  public static final String GLOBAL_CONFIG_NAME = "global";

  private ConcurrentMap<String, Object> configurations = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public Map<String, Object> getGlobalConfig() {
    return (Map<String, Object>) configurations.get(GLOBAL_CONFIG_NAME);
  }

  public void updateGlobalConfig(byte[] data) throws IOException {
    updateGlobalConfig(new ByteArrayInputStream(data));
  }

  public void updateGlobalConfig(InputStream io) throws IOException {
    Map<String, Object> globalConfig = JSONUtils.INSTANCE.load(io, new TypeReference<Map<String, Object>>() {});
    updateGlobalConfig(globalConfig);
  }

  public void updateGlobalConfig(Map<String, Object> globalConfig) {
    configurations.put(GLOBAL_CONFIG_NAME, globalConfig);
  }

  public SensorEnrichmentConfig getSensorEnrichmentConfig(String sensorType) {
    return (SensorEnrichmentConfig) configurations.get(sensorType);
  }

  public void updateSensorEnrichmentConfig(String sensorType, byte[] data) throws IOException {
    updateSensorEnrichmentConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorEnrichmentConfig(String sensorType, InputStream io) throws IOException {
    SensorEnrichmentConfig sensorEnrichmentConfig = JSONUtils.INSTANCE.load(io, SensorEnrichmentConfig.class);
    updateSensorEnrichmentConfig(sensorType, sensorEnrichmentConfig);
  }

  public void updateSensorEnrichmentConfig(String sensorType, SensorEnrichmentConfig sensorEnrichmentConfig) {
    configurations.put(sensorType, sensorEnrichmentConfig);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getConfig(String name) {
    return (Map<String, Object>) configurations.get(name);
  }

  public void updateConfig(String name, byte[] data) {
    try {
      Map<String, Object> config = JSONUtils.INSTANCE.load(new ByteArrayInputStream(data), new TypeReference<Map<String, Object>>(){});
      updateConfig(name, config);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void updateConfig(String name, Map<String, Object> config) {
    configurations.put(name, config);
  }
}
