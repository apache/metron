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

package org.apache.metron.common.configuration.writer;

import org.apache.metron.common.field.FieldNameConverter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Configures a writer to write messages to an endpoint.
 *
 * <p>Each destination will have its own {@link WriterConfiguration}; for example HDFS, Elasticsearch, and Solr.
 *
 * <p>A writer can be configured independently for each source type.
 */
public interface WriterConfiguration extends Serializable {

  /**
   * Defines the maximum batch size for a given sensor.
   *
   * @param sensorName The name of the sensor.
   * @return The batch size for the sensor.
   */
  int getBatchSize(String sensorName);

  /**
   * Defines the batch timeout for a given sensor.  Even if the maximum
   * batch size has not been reached, the messages will be written when
   * the timeout is reached.
   *
   * @param sensorName The name of the sensor.
   * @return The batch timeout for the sensor.
   */
  int getBatchTimeout(String sensorName);

  /**
   * Returns the batch timeouts for all of the currently configured sensors.
   * @return All of the batch timeouts.
   */
  List<Integer> getAllConfiguredTimeouts();

  /**
   * The name of the index to write to for a given sensor.
   *
   * @param sensorName The name of the sensor.
   * @return The name of the index to write to
   */
  String getIndex(String sensorName);

  /**
   * Returns true, if this writer is enabled for the given sensor.
   *
   * @param sensorName The name of the sensor.
   * @return True, if this writer is enabled.  Otherwise, false.
   */
  boolean isEnabled(String sensorName);

  /**
   * Returns the sensor config for a specific sensor.
   *
   * @param sensorName The name of a sensor.
   * @return a map containing the config
   */
  Map<String, Object> getSensorConfig(String sensorName);

  /**
   * Returns the global configuration.
   * @return The global configuration.
   */
  Map<String, Object> getGlobalConfig();

  /**
   * Returns true, if the current writer configuration is set to all default values.
   *
   * @param sensorName The name of the sensor.
   * @return True, if the writer is using all default values. Otherwise, false.
   */
  boolean isDefault(String sensorName);

  /**
   * Return the {@link FieldNameConverter} to use
   * when writing messages.
   *
   * @param sensorName The name of the sensor;
   * @return The {@link FieldNameConverter}
   */
  String getFieldNameConverter(String sensorName);
}
