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
package org.apache.metron.enrichment.bolt;

import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.enrichment.utils.ThreatIntelUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreatIntelSplitterBolt extends EnrichmentSplitterBolt {

  public ThreatIntelSplitterBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  protected Map<String, List<String>> getFieldMap(String sensorType) {
    if (sensorType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sensorType);
      if (config != null) {
        return config.getThreatIntel().getFieldMap();
      } else {
        LOG.error("Unable to retrieve sensor config: " + sensorType);
      }
    } else {
      LOG.error("Trying to retrieve a field map with sensor type of null");
    }
    return new HashMap<>();
  }

  @Override
  protected String getKeyName(String type, String field) {
    return ThreatIntelUtils.getThreatIntelKey(type, field);
  }
}
