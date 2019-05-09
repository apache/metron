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

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;

/**
 * Writer configuration for the profiler topology. Batch size and batch timeout are a couple
 * primary values of interest that are used for configuring the profiler writer.
 */
public class ProfilerWriterConfiguration implements WriterConfiguration {
  private Optional<ProfilerConfigurations> config;

  public ProfilerWriterConfiguration(ProfilerConfigurations config) {
    this.config = Optional.ofNullable(config);
  }

  /**
   * Batch size for writing.
   * @param sensorName n/a
   * @return batch size in # messages
   */
  @Override
  public int getBatchSize(String sensorName) {
    return config.orElse(new ProfilerConfigurations()).getBatchSize();
  }

  /**
   * Timeout for this writer.
   * @param sensorName n/a
   * @return timeout in ms
   */
  @Override
  public int getBatchTimeout(String sensorName) {
    return config.orElse(new ProfilerConfigurations()).getBatchTimeout();
  }

  /**
   * Timeout for this writer.
   * @return single item list with this writer's timeout
   */
  @Override
  public List<Integer> getAllConfiguredTimeouts() {
    return asList(getBatchTimeout(null));
  }

  /**
   * n/a for profiler.
   * @param sensorName n/a
   * @return null
   */
  @Override
  public String getIndex(String sensorName) {
    return null;
  }

  /**
   * Always enabled in profiler.
   * @param sensorName n/a
   * @return true
   */
  @Override
  public boolean isEnabled(String sensorName) {
    return true;
  }

  @Override
  public Map<String, Object> getSensorConfig(String sensorName) {
    throw new UnsupportedOperationException("Profiler does not have sensor configs");
  }

  @Override
  public Map<String, Object> getGlobalConfig() {
    return config.orElse(new ProfilerConfigurations()).getGlobalConfig();
  }

  @Override
  public boolean isDefault(String sensorName) {
    return false;
  }

  @Override
  public String getFieldNameConverter(String sensorName) {
    // not applicable
    return null;
  }

}
