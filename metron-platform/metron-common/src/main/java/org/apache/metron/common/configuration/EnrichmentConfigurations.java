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

/**
 * Allows for retrieval and update of enrichment configurations. Some fields are pulled from
 * global config and provided here for convenience.
 */
public class EnrichmentConfigurations extends Configurations {
  public static final Integer DEFAULT_KAFKA_BATCH_SIZE = 15;
  public static final String BATCH_SIZE_CONF = "enrichment.writer.batchSize";
  public static final String BATCH_TIMEOUT_CONF = "enrichment.writer.batchTimeout";

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

  /**
   * Pulled from global config.
   * Note: enrichment writes out to 1 kafka topic, so it is not pulling this config by sensor.
   *
   * @return batch size for writing to kafka
   * @see org.apache.metron.common.configuration.EnrichmentConfigurations#BATCH_SIZE_CONF
   */
  public int getBatchSize() {
    return getAs(BATCH_SIZE_CONF, getGlobalConfig(true), DEFAULT_KAFKA_BATCH_SIZE, Integer.class);
  }

  /**
   * Pulled from global config
   * Note: enrichment writes out to 1 kafka topic, so it is not pulling this config by sensor.
   *
   * @return batch timeout for writing to kafka
   * @see org.apache.metron.common.configuration.EnrichmentConfigurations#BATCH_TIMEOUT_CONF
   */
  public int getBatchTimeout() {
    return getAs(BATCH_TIMEOUT_CONF, getGlobalConfig(true), 0, Integer.class);
  }

  /**
   * Gets the sensor names that have associated enrichments.
   *
   * @return List of sensor names
   */
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
