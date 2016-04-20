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

import org.apache.metron.common.configuration.SensorEnrichmentConfig;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ThreatIntelJoinBolt extends EnrichmentJoinBolt {

  protected static final Logger LOG = LoggerFactory
          .getLogger(ThreatIntelJoinBolt.class);

  public ThreatIntelJoinBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  public Map<String, List<String>> getFieldMap(String sourceType) {
    SensorEnrichmentConfig config = configurations.getSensorEnrichmentConfig(sourceType);
    if(config != null) {
      return config.getThreatIntelFieldMap();
    }
    else {
      LOG.error("Unable to retrieve sensor config: " + sourceType);
      return null;
    }
  }

  @Override
  public JSONObject joinMessages(Map<String, JSONObject> streamMessageMap) {
    JSONObject ret = super.joinMessages(streamMessageMap);
    for(Object key : ret.keySet()) {
      if(key.toString().startsWith("threatintels") && !key.toString().endsWith(".ts")) {
        ret.put("is_alert" , "true");
        break;
      }
    }
    return ret;
  }
}
