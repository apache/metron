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
package org.apache.metron.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jakewharton.fliptables.FlipTable;
import org.apache.log4j.Logger;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.common.utils.JSONUtils;

import java.util.*;

import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.management.EnrichmentConfigFunctions.getConfig;

public class ThreatTriageFunctions {
  private static final Logger LOG = Logger.getLogger(ConfigurationFunctions.class);

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "PRINT"
          ,description = "Retrieve stellar enrichment transformations."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    }
          ,returns = "The String representation of the threat triage rules"
          )
  public static class GetStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        return "";
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        return "";
      }
      List<RiskLevelRule> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new ArrayList<>();
      }
      String[] headers = new String[] {"Name", "Comment", "Triage Rule", "Score", "Reason"};
      String[][] data = new String[triageRules.size()][5];
      int i = 0;
      for(RiskLevelRule rule : triageRules) {
        double d = rule.getScore().doubleValue();
        String score = d == (long)d ? String.format("%d", (long)d) : String.format("%s", d);
        String name = Optional.ofNullable(rule.getName()).orElse("");
        String comment = Optional.ofNullable(rule.getComment()).orElse("");
        String reason = Optional.ofNullable(rule.getReason()).orElse("");
        data[i++]  = new String[] {name, comment, rule.getRule(), score, reason};
      }
      String ret = FlipTable.of(headers, data);
      if(!triageRules.isEmpty()) {
        ret += "\n\n";

        ret += "Aggregation: " + triageConfig.getAggregator().name();
      }
      return ret;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "ADD"
          ,description = "Add a threat triage rule."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"triageRules - A Map (or list of Maps) representing a triage rule.  It must contain 'rule' and 'score' keys, " +
                      "the stellar expression for the rule and triage score respectively.  " +
                      "It may contain 'name' and 'comment', the name of the rule and comment associated with the rule respectively."
                    }
          ,returns = "The String representation of the threat triage rules"
          )
  public static class AddStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      List<RiskLevelRule> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new ArrayList<>();
      }
      Object newRuleObj = args.get(1);
      List<Map<String, Object>> newRules = new ArrayList<>();
      if(newRuleObj != null && newRuleObj instanceof List) {
        newRules = (List<Map<String, Object>>) newRuleObj;
      }
      else if(newRuleObj != null && newRuleObj instanceof Map) {
        newRules.add((Map<String, Object>) newRuleObj);
      }
      else if(newRuleObj != null) {
        throw new IllegalStateException("triageRule must be either a Map representing a single rule or a List of rules.");
      }
      for(Map<String, Object> newRule : newRules) {
        if(!(newRule == null || !newRule.containsKey("rule") || !newRule.containsKey("score"))) {
          RiskLevelRule ruleToAdd = new RiskLevelRule();
          ruleToAdd.setRule((String) newRule.get("rule"));
          ruleToAdd.setScore(ConversionUtils.convert(newRule.get("score"), Double.class));
          if (newRule.containsKey("name")) {
            ruleToAdd.setName((String) newRule.get("name"));
          }
          if (newRule.containsKey("comment")) {
            ruleToAdd.setComment((String) newRule.get("comment"));
          }
          if (newRule.containsKey("reason")) {
            ruleToAdd.setReason((String) newRule.get("reason"));
          }
          triageRules.add(ruleToAdd);
        }
      }
      triageConfig.setRiskLevelRules(triageRules);
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
        return config;
      }

    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "REMOVE"
          ,description = "Remove stellar threat triage rule(s)."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"rules - A list of stellar rules or rule names to remove"
                    }
          ,returns = "The String representation of the enrichment config"
          )
  public static class RemoveStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);
      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }
      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      List<RiskLevelRule> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new ArrayList<>();
        triageConfig.setRiskLevelRules(triageRules);
      }

      Set<String> toRemove = new HashSet<>(Optional.ofNullable((List<String>) args.get(1)).orElse(new ArrayList<>()));
      for (Iterator<RiskLevelRule> it = triageRules.iterator();it.hasNext();){
        RiskLevelRule rule = it.next();
        boolean remove = toRemove.contains(rule.getRule());
        if(!remove && rule.getName() != null) {
          remove = toRemove.contains(rule.getName());
        }
        if(remove) {
          it.remove();
        }
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
        return config;
      }

    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "SET_AGGREGATOR"
          ,description = "Set the threat triage aggregator."
          ,params = {"sensorConfig - Sensor config to add transformation to."
                    ,"aggregator - Aggregator to use.  One of MIN, MAX, MEAN, SUM, POSITIVE_MEAN"
                    ,"aggregatorConfig - Optional config for aggregator"
                    }
          ,returns = "The String representation of the enrichment config"
          )
  public static class SetAggregator implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String config = (String) args.get(0);

      SensorEnrichmentConfig configObj;
      if(config == null || config.isEmpty()) {
        configObj = new SensorEnrichmentConfig();
      }
      else {
        configObj = (SensorEnrichmentConfig) ENRICHMENT.deserialize(config);
      }

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(configObj, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        configObj.setThreatIntel(tiConfig);
      }
      ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }
      List<RiskLevelRule> triageRules = triageConfig.getRiskLevelRules();
      if(triageRules == null) {
        triageRules = new ArrayList<>();
        triageConfig.setRiskLevelRules(triageRules);
      }
      String aggregator = (String) args.get(1);
      triageConfig.setAggregator(aggregator);
      if(args.size() > 2) {
        Map<String, Object> aggConfig = (Map<String, Object>) args.get(2);
        triageConfig.setAggregationConfig(aggConfig);
      }
      try {
        return JSONUtils.INSTANCE.toJSON(configObj, true);
      } catch (JsonProcessingException e) {
        LOG.error("Unable to convert object to JSON: " + configObj, e);
        return config;
      }

    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
