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
package org.apache.metron.common.configuration.profiler;

import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.utils.JSONUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Used to manage configurations for the Profiler.
 */
public class ProfilerConfigurations extends Configurations {
  public static final Integer DEFAULT_KAFKA_BATCH_SIZE = 15;
  public static final String BATCH_SIZE_CONF = "profiler.writer.batchSize";
  public static final String BATCH_TIMEOUT_CONF = "profiler.writer.batchTimeout";

  public ProfilerConfig getProfilerConfig() {
    return (ProfilerConfig) getConfigurations().get(getKey());
  }

  public void updateProfilerConfig(byte[] data) throws IOException {
    updateProfilerConfig(new ByteArrayInputStream(data));
  }

  public void updateProfilerConfig(InputStream io) throws IOException {
    ProfilerConfig config = JSONUtils.INSTANCE.load(io, ProfilerConfig.class);
    updateProfilerConfig(config);
  }

  public void updateProfilerConfig(ProfilerConfig config) {
    getConfigurations().put(getKey(), config);
  }

  public static String getKey() {
    return ConfigurationType.PROFILER.getTypeName();
  }

  public void delete() {
    configurations.remove(getKey());
  }

  /**
   * Pulled from global config.
   * Note: profiler writes out to 1 kafka topic, so it is not pulling this config by sensor.
   *
   * @return batch size for writing to kafka
   * @see org.apache.metron.common.configuration.profiler.ProfilerConfigurations#BATCH_SIZE_CONF
   */
  public int getBatchSize() {
    return getAs(BATCH_SIZE_CONF, getGlobalConfig(true), DEFAULT_KAFKA_BATCH_SIZE, Integer.class);
  }

  /**
   * Pulled from global config
   * Note: profiler writes out to 1 kafka topic, so it is not pulling this config by sensor.
   *
   * @return batch timeout for writing to kafka
   * @see org.apache.metron.common.configuration.profiler.ProfilerConfigurations#BATCH_TIMEOUT_CONF
   */
  public int getBatchTimeout() {
    return getAs(BATCH_TIMEOUT_CONF, getGlobalConfig(true), 0, Integer.class);
  }

}
