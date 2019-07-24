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

import org.apache.metron.common.configuration.IndexingConfigurations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IndexingWriterConfiguration implements WriterConfiguration{
  private Optional<IndexingConfigurations> config;
  private String writerName;

  public IndexingWriterConfiguration(String writerName, IndexingConfigurations config) {
    this.config = Optional.ofNullable(config);
    this.writerName = writerName;
  }

  @Override
  public int getBatchSize(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getBatchSize(sensorName, writerName);
  }

  @Override
  public int getBatchTimeout(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getBatchTimeout(sensorName, writerName);
  }

  @Override
  public List<Integer> getAllConfiguredTimeouts() {
      return config.orElse(new IndexingConfigurations()).getAllConfiguredTimeouts(writerName);
  }

  @Override
  public String getIndex(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getIndex(sensorName, writerName);
  }

  @Override
  public boolean isEnabled(String sensorName) {
    return config.orElse(new IndexingConfigurations()).isEnabled(sensorName, writerName);
  }

  @Override
  public Map<String, Object> getSensorConfig(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getSensorIndexingConfig(sensorName, writerName);
  }

  @Override
  public Map<String, Object> getGlobalConfig() {
    return config.orElse(new IndexingConfigurations()).getGlobalConfig();
  }

  @Override
  public boolean isDefault(String sensorName) {
    return config.orElse(new IndexingConfigurations()).isDefault(sensorName, writerName);
  }

  @Override
  public String getFieldNameConverter(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getFieldNameConverter(sensorName, writerName);
  }

  @Override
  public boolean isSetDocumentId(String sensorName) {
    return config.orElse(new IndexingConfigurations()).isSetDocumentId(sensorName, writerName);
  }
}
