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

import org.apache.storm.task.TopologyContext;
import com.google.common.base.Joiner;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ThreatIntelJoinBolt extends EnrichmentJoinBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ThreatIntelJoinBolt.class);
  private FunctionResolver functionResolver;
  private org.apache.metron.common.dsl.Context stellarContext;

  public ThreatIntelJoinBolt(String zookeeperUrl) {
    super(zookeeperUrl);
  }

  @Override
  protected Map<String, ConfigHandler> getFieldToHandlerMap(String sensorType) {
    if(sensorType != null) {
      SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sensorType);
      if (config != null) {
        return config.getThreatIntel().getEnrichmentConfigs();
      } else {
        LOG.info("Unable to retrieve a sensor enrichment config of " + sensorType);
      }
    } else {
      LOG.error("Trying to retrieve a field map with sensor type of null");
    }
    return new HashMap<>();
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext) {
    super.prepare(map, topologyContext);
    initializeStellar();
  }

  protected void initializeStellar() {
    this.stellarContext = new Context.Builder()
                                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                                .build();
    StellarFunctions.initialize(stellarContext);
    this.functionResolver = StellarFunctions.FUNCTION_RESOLVER();
  }

  @Override
  public Map<String, Object> getFieldMap(String sourceType) {
    SensorEnrichmentConfig config = getConfigurations().getSensorEnrichmentConfig(sourceType);
    if(config != null) {
      return config.getThreatIntel().getFieldMap();
    }
    else {
      LOG.info("Unable to retrieve sensor config: " + sourceType);
      return null;
    }
  }

  @Override
  public JSONObject joinMessages(Map<String, JSONObject> streamMessageMap) {
    JSONObject ret = super.joinMessages(streamMessageMap);
    LOG.trace("Received joined messages: {}", ret);
    boolean isAlert = ret.containsKey("is_alert");
    if(!isAlert) {
      for (Object key : ret.keySet()) {
        if (key.toString().startsWith("threatintels") && !key.toString().endsWith(".ts")) {
          isAlert = true;
          break;
        }
      }
    }
    else {
      Object isAlertObj = ret.get("is_alert");
      isAlert = ConversionUtils.convert(isAlertObj, Boolean.class);
      if(!isAlert) {
        ret.remove("is_alert");
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

        ThreatTriageProcessor threatTriageProcessor = new ThreatTriageProcessor(config, functionResolver, stellarContext);
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
