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

import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EnrichmentConfigurations extends Configurations {

  public SensorEnrichmentConfig getSensorEnrichmentConfig(String sensorType) {
    return (SensorEnrichmentConfig) getConfigurations().get(getKey(sensorType));
  }

  public void updateSensorEnrichmentConfig(String sensorType, byte[] data) throws IOException {
    updateSensorEnrichmentConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorEnrichmentConfig(String sensorType, InputStream io) throws IOException {
    SensorEnrichmentConfig sensorEnrichmentConfig = JSONUtils.INSTANCE.load(io, SensorEnrichmentConfig.class);
    updateSensorEnrichmentConfig(sensorType, sensorEnrichmentConfig);
  }

  public void updateSensorEnrichmentConfig(String sensorType, SensorEnrichmentConfig sensorEnrichmentConfig) {
    getConfigurations().put(getKey(sensorType), sensorEnrichmentConfig);
  }

  public void delete(String sensorType) {
    getConfigurations().remove(getKey(sensorType));
  }

  public List<String> getTypes() {
    List<String> ret = new ArrayList<>();
    for(String keyedSensor : getConfigurations().keySet()) {
      if(!keyedSensor.isEmpty() && keyedSensor.startsWith(ConfigurationType.ENRICHMENT.getTypeName())) {
        ret.add(keyedSensor.substring(ConfigurationType.ENRICHMENT.getTypeName().length() + 1));
      }
    }
    return ret;
  }

  public static String getKey(String sensorType) {
    return ConfigurationType.ENRICHMENT.getTypeName() + "." + sensorType;
  }
}
