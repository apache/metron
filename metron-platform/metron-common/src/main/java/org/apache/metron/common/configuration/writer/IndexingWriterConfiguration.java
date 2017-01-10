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

import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.utils.ConversionUtils;

import java.util.Map;
import java.util.Optional;

public class IndexingWriterConfiguration implements WriterConfiguration{
  private Optional<IndexingConfigurations> config;
  public static final String BATCH_SIZE_CONF = "batchSize";
  public static final String INDEX_CONF = "index";

  public IndexingWriterConfiguration(IndexingConfigurations config) {
    this.config = Optional.ofNullable(config);
  }

  private <T> T getAs(String key, Map<String, Object> map, T defaultValue, Class<T> clazz) {
    return map == null?defaultValue:ConversionUtils.convert(map.getOrDefault(key, defaultValue), clazz);
  }

  @Override
  public int getBatchSize(String sensorName) {
    return getAs( BATCH_SIZE_CONF
                , config.orElse(new IndexingConfigurations()).getSensorIndexingConfig(sensorName)
                , 1
                , Integer.class
                );
  }

  @Override
  public String getIndex(String sensorName) {
    return getAs( INDEX_CONF
                , config.orElse(new IndexingConfigurations()).getSensorIndexingConfig(sensorName)
                , sensorName
                , String.class
                );
  }

  @Override
  public Map<String, Object> getSensorConfig(String sensorName) {
    return config.orElse(new IndexingConfigurations()).getSensorIndexingConfig(sensorName);
  }

  @Override
  public Map<String, Object> getGlobalConfig() {
    return config.orElse(new IndexingConfigurations()).getGlobalConfig();
  }
}
