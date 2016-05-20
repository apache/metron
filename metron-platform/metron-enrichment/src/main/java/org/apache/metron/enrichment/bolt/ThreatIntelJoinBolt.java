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

import com.google.common.base.Joiner;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
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
    SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
    if(config != null) {
      return config.getThreatIntel().getFieldMap();
    }
    else {
      LOG.error("Unable to retrieve sensor config: " + sourceType);
      return null;
    }
  }

  @Override
  public JSONObject joinMessages(Map<String, JSONObject> streamMessageMap) {
    JSONObject ret = super.joinMessages(streamMessageMap);
    boolean isAlert = ret.containsKey("is_alert");
    if(!isAlert) {
      for (Object key : ret.keySet()) {
        if (key.toString().startsWith("threatintels") && !key.toString().endsWith(".ts")) {
          isAlert = true;
          break;
        }
      }
    }
    if(isAlert) {
      ret.put("is_alert" , "true");
      String sourceType = MessageUtils.getSensorType(ret);
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
      ThreatTriageConfig triageConfig = null;
      if(config != null) {
        triageConfig = config.getThreatIntel().getTriageConfig();
        if(LOG.isDebugEnabled()) {
          LOG.debug(sourceType + ": Found sensor enrichment config.");
        }
      }
      else {
        LOG.debug(sourceType + ": Unable to find threat config.");
      }
      if(triageConfig != null) {
        if(LOG.isDebugEnabled()) {
          LOG.debug(sourceType + ": Found threat triage config: " + triageConfig);
        }

        if(LOG.isDebugEnabled() && (triageConfig.getRiskLevelRules() == null || triageConfig.getRiskLevelRules().isEmpty())) {
          LOG.debug(sourceType + ": Empty rules!");
        }

        ThreatTriageProcessor threatTriageProcessor = new ThreatTriageProcessor(triageConfig);
        Double triageLevel = threatTriageProcessor.apply(ret);
        if(LOG.isDebugEnabled()) {
          String rules = Joiner.on('\n').join(triageConfig.getRiskLevelRules().entrySet());
          LOG.debug("Marked " + sourceType + " as triage level " + triageLevel + " with rules " + rules);
        }
        if(triageLevel != null && triageLevel > 0) {
          ret.put("threat.triage.level", triageLevel);
        }
      }
      else {
        LOG.debug(sourceType + ": Unable to find threat triage config!");
      }

    }

    return ret;
  }
}
