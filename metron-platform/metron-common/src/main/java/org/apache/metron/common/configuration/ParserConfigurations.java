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
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Allows for retrieval and update of parsing configurations.
 */
public class ParserConfigurations extends Configurations {
  public static final Integer DEFAULT_KAFKA_BATCH_SIZE = 15;
  public static final String PARSER_GROUPS_CONF = "parser.groups";

  public SensorParserConfig getSensorParserConfig(String sensorType) {
    return (SensorParserConfig) getConfigurations().get(getKey(sensorType));
  }

  public void updateSensorParserConfig(String sensorType, byte[] data) throws IOException {
    updateSensorParserConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorParserConfig(String sensorType, InputStream io) throws IOException {
    SensorParserConfig sensorParserConfig = JSONUtils.INSTANCE.load(io, SensorParserConfig.class);
    updateSensorParserConfig(sensorType, sensorParserConfig);
  }

  public void updateSensorParserConfig(String sensorType, SensorParserConfig sensorParserConfig) {
    sensorParserConfig.init();
    getConfigurations().put(getKey(sensorType), sensorParserConfig);
  }

  /**
   * Retrieves all the sensor groups from the global config
   * @return Map of sensor groups with the group name as the key
   */
  public Map<String, SensorParserGroup> getSensorParserGroups() {
    Object groups = getGlobalConfig(true).getOrDefault(PARSER_GROUPS_CONF, new ArrayList<>());
    Collection<SensorParserGroup> sensorParserGroups = JSONUtils.INSTANCE.getMapper()
            .convertValue(groups, new TypeReference<Collection<SensorParserGroup>>() {{}});
    return sensorParserGroups.stream()
            .collect(Collectors.toMap(SensorParserGroup::getName, sensorParserGroup -> sensorParserGroup));
  }

  /**
   * Gets the list of sensor types that parsing configurations exist for.
   *
   * @return List of sensor types
   */
  public List<String> getTypes() {
    List<String> ret = new ArrayList<>();
    for(String keyedSensor : getConfigurations().keySet()) {
      if(!keyedSensor.isEmpty() && keyedSensor.startsWith(ConfigurationType.PARSER.getTypeName())) {
        ret.add(keyedSensor.substring(ConfigurationType.PARSER.getTypeName().length() + 1));
      }
    }
    return ret;
  }

  public void delete(String sensorType) {
    getConfigurations().remove(getKey(sensorType));
  }

  public static String getKey(String sensorType) {
    return ConfigurationType.PARSER.getTypeName() + "." + sensorType;
  }
}
