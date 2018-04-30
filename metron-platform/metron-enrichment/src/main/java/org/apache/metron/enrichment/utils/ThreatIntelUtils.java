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
package org.apache.metron.enrichment.utils;

import com.google.common.base.Joiner;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.common.utils.MessageUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class ThreatIntelUtils {
  public static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String KEY_PREFIX = "threatintels";
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


  public static String getThreatIntelKey(String threatIntelName, String field) {
    return Joiner.on(".").join(new String[]{KEY_PREFIX, threatIntelName, field});
  }

public static JSONObject triage(JSONObject ret, SensorEnrichmentConfig config, FunctionResolver functionResolver, Context stellarContext) {
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
      ThreatTriageConfig triageConfig = null;
      if(config != null) {
        triageConfig = config.getThreatIntel().getTriageConfig();
        if(LOG.isDebugEnabled()) {
          LOG.debug("{}: Found sensor enrichment config.", sourceType);
        }
      }
      else {
        LOG.debug("{}: Unable to find threat config.", sourceType );
      }
      if(triageConfig != null) {
        if(LOG.isDebugEnabled()) {
          LOG.debug("{}: Found threat triage config: {}", sourceType, triageConfig);
        }

        if(LOG.isDebugEnabled() && (triageConfig.getRiskLevelRules() == null || triageConfig.getRiskLevelRules().isEmpty())) {
          LOG.debug("{}: Empty rules!", sourceType);
        }

        // triage the threat
        ThreatTriageProcessor threatTriageProcessor = new ThreatTriageProcessor(config, functionResolver, stellarContext);
        ThreatScore score = threatTriageProcessor.apply(ret);

        if(LOG.isDebugEnabled()) {
          String rules = Joiner.on('\n').join(triageConfig.getRiskLevelRules());
          LOG.debug("Marked {} as triage level {} with rules {}", sourceType, score.getScore(),
              rules);
        }

        // attach the triage threat score to the message
        if(score.getRuleScores().size() > 0) {
          appendThreatScore(score, ret);
        }
      }
      else {
        LOG.debug("{}: Unable to find threat triage config!", sourceType);
      }
    }

    return ret;
  }


  /**
   * Appends the threat score to the telemetry message.
   * @param threatScore The threat triage score
   * @param message The telemetry message being triaged.
   */
  private static void appendThreatScore(ThreatScore threatScore, JSONObject message) {

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
