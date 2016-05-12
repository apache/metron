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

import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ParserConfigurations extends Configurations {

  public SensorParserConfig getSensorParserConfig(String sensorType) {
    return (SensorParserConfig) configurations.get(getKey(sensorType));
  }

  public void updateSensorParserConfig(String sensorType, byte[] data) throws IOException {
    updateSensorParserConfig(sensorType, new ByteArrayInputStream(data));
  }

  public void updateSensorParserConfig(String sensorType, InputStream io) throws IOException {
    SensorParserConfig sensorParserConfig = JSONUtils.INSTANCE.load(io, SensorParserConfig.class);
    updateSensorParserConfig(sensorType, sensorParserConfig);
  }

  public void updateSensorParserConfig(String sensorType, SensorParserConfig sensorParserConfig) {
    configurations.put(getKey(sensorType), sensorParserConfig);
  }

  private String getKey(String sensorType) {
    return ConfigurationType.PARSER.getName() + "." + sensorType;
  }
}
