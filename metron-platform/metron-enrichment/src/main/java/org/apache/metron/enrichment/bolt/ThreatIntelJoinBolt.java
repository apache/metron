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
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.enrichment.adapters.geo.GeoLiteDatabase;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ThreatIntelJoinBolt extends EnrichmentJoinBolt {

  protected static final Logger LOG = LoggerFactory.getLogger(ThreatIntelJoinBolt.class);

  /**
   * The message key under which the overall threat triage score is stored.
   */
  public static final String THREAT_TRIAGE_SCORE_KEY = "threat.triage.score";

  /**
   * The prefix of the message keys that record the threat triage rules that fired.
   */
  public static final String THREAT_TRIAGE_RULES_KEY = "threat.triage.rules";

  /**
   * The portion of the message key used to record the 'name' field of a rule.
   */
  public static final String THREAT_TRIAGE_RULE_NAME = "name";

  /**
   * The portion of the message key used to record the 'comment' field of a rule.
   */
  public static final String THREAT_TRIAGE_RULE_COMMENT = "comment";

  /**
   * The portion of the message key used to record the 'score' field of a rule.
   */
  public static final String THREAT_TRIAGE_RULE_SCORE = "score";

  /**
   * The portion of the message key used to record the 'reason' field of a rule.
   */
  public static final String THREAT_TRIAGE_RULE_REASON = "reason";

  /**
   * The Stellar function resolver.
   */
  private FunctionResolver functionResolver;

  /**
   * The execution context for Stellar.
   */
  private Context stellarContext;

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
    GeoLiteDatabase.INSTANCE.update((String)getConfigurations().getGlobalConfig().get(GeoLiteDatabase.GEO_HDFS_FILE));
    initializeStellar();
  }

  protected void initializeStellar() {
    this.stellarContext = new Context.Builder()
                                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client)
                                .with(Context.Capabilities.GLOBAL_CONFIG, () -> getConfigurations().getGlobalConfig())
                                .with(Context.Capabilities.STELLAR_CONFIG, () -> getConfigurations().getGlobalConfig())
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
  public JSONObject joinMessages(Map<String, Tuple> streamMessageMap, MessageGetStrategy messageGetStrategy) {
    JSONObject ret = super.joinMessages(streamMessageMap, messageGetStrategy);
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

        // triage the threat
        ThreatTriageProcessor threatTriageProcessor = new ThreatTriageProcessor(config, functionResolver, stellarContext);
        ThreatScore score = threatTriageProcessor.apply(ret);

        if(LOG.isDebugEnabled()) {
          String rules = Joiner.on('\n').join(triageConfig.getRiskLevelRules());
          LOG.debug("Marked " + sourceType + " as triage level " + score.getScore() + " with rules " + rules);
        }

        // attach the triage threat score to the message
        if(score.getRuleScores().size() > 0) {
          appendThreatScore(score, ret);
        }
      }
      else {
        LOG.debug(sourceType + ": Unable to find threat triage config!");
      }
    }

    return ret;
  }

  @Override
  public void reloadCallback(String name, ConfigurationType type) {
    super.reloadCallback(name, type);
    if(type == ConfigurationType.GLOBAL) {
      GeoLiteDatabase.INSTANCE.updateIfNecessary(getConfigurations().getGlobalConfig());
    }
  }

  /**
   * Appends the threat score to the telemetry message.
   * @param threatScore The threat triage score
   * @param message The telemetry message being triaged.
   */
  private void appendThreatScore(ThreatScore threatScore, JSONObject message) {

    // append the overall threat score
    message.put(THREAT_TRIAGE_SCORE_KEY, threatScore.getScore());

    // append each of the rules - each rule is 'flat'
    Joiner joiner = Joiner.on(".");
    int i = 0;
    for(RuleScore score: threatScore.getRuleScores()) {
      message.put(joiner.join(THREAT_TRIAGE_RULES_KEY, i, THREAT_TRIAGE_RULE_NAME), score.getRule().getName());
      message.put(joiner.join(THREAT_TRIAGE_RULES_KEY, i, THREAT_TRIAGE_RULE_COMMENT), score.getRule().getComment());
      message.put(joiner.join(THREAT_TRIAGE_RULES_KEY, i, THREAT_TRIAGE_RULE_SCORE), score.getRule().getScore());
      message.put(joiner.join(THREAT_TRIAGE_RULES_KEY, i++, THREAT_TRIAGE_RULE_REASON), score.getReason());
    }
  }
}
