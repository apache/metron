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
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParserWriterConfiguration implements WriterConfiguration {
  private ParserConfigurations config;
  public ParserWriterConfiguration(ParserConfigurations config) {
    this.config = config;
  }
  @Override
  public int getBatchSize(String sensorName) {
    if(config != null
            && config.getSensorParserConfig(sensorName) != null
            && config.getSensorParserConfig(sensorName).getParserConfig() != null
            ) {
      Object batchObj = config.getSensorParserConfig(sensorName).getParserConfig().get(IndexingConfigurations.BATCH_SIZE_CONF);
      return batchObj == null ? 1 : ConversionUtils.convert(batchObj, Integer.class);
    }
    return 1;
  }

  @Override
  public int getBatchTimeout(String sensorName) {
    if(config != null
            && config.getSensorParserConfig(sensorName) != null
            && config.getSensorParserConfig(sensorName).getParserConfig() != null
            ) {
      Object batchObj = config.getSensorParserConfig(sensorName).getParserConfig().get(IndexingConfigurations.BATCH_TIMEOUT_CONF);
      return batchObj == null ? 0 : ConversionUtils.convert(batchObj, Integer.class);
    }
    return 0;
  }

  @Override
  public List<Integer> getAllConfiguredTimeouts() {
    // TODO - stub implementation pending METRON-750
    return new ArrayList<Integer>();
  }

  @Override
  public String getIndex(String sensorName) {
    if(config != null && config.getSensorParserConfig(sensorName) != null
    && config.getSensorParserConfig(sensorName).getParserConfig() != null
      ) {
      Object indexObj = config.getSensorParserConfig(sensorName).getParserConfig().get(IndexingConfigurations.INDEX_CONF);
      if(indexObj != null) {
        return indexObj.toString();
      }
      return null;
    }
    return sensorName;
  }

  @Override
  public boolean isEnabled(String sensorName) {
    if(config != null
    && config.getSensorParserConfig(sensorName) != null
    && config.getSensorParserConfig(sensorName).getParserConfig() != null
      ) {
      Object enabledObj = config.getSensorParserConfig(sensorName).getParserConfig().get(IndexingConfigurations.ENABLED_CONF);
      return enabledObj == null ? true : ConversionUtils.convert(enabledObj, Boolean.class);
    }
    return true;
  }

  @Override
  public Map<String, Object> getSensorConfig(String sensorName) {
    return config.getSensorParserConfig(sensorName).getParserConfig();
  }

  @Override
  public Map<String, Object> getGlobalConfig() {
    return config.getGlobalConfig();
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
