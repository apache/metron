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
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.client.stellar.Util;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver;
import org.apache.metron.threatintel.triage.ThreatTriageProcessor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.metron.common.configuration.ConfigurationType.ENRICHMENT;
import static org.apache.metron.management.EnrichmentConfigFunctions.getConfig;

/**
 * Stellar functions related to Threat Triage.
 */
public class ThreatTriageFunctions {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final String SCORE_KEY = "score";
  protected static final String RULES_KEY = "rules";
  protected static final String AGG_KEY = "aggregator";
  protected static final String RULE_NAME_KEY = "name";
  protected static final String RULE_EXPR_KEY = "rule";
  protected static final String RULE_SCORE_KEY = "score";
  protected static final String RULE_REASON_KEY = "reason";
  protected static final String RULE_COMMENT_KEY = "comment";

  @Stellar(
          namespace = "THREAT_TRIAGE"
          ,name = "INIT"
          ,description = "Create a threat triage engine to execute triage rules."
          ,params = {"config - the threat triage configuration (optional)" }
          ,returns = "A threat triage engine."
  )
  public static class ThreatTriageInit implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      ThreatTriageProcessor processor;
      SensorEnrichmentConfig config = new SensorEnrichmentConfig();

      // the user can provide an initial config
      if(args.size() > 0) {
        String json = Util.getArg(0, String.class, args);
        if (json != null) {
          config = (SensorEnrichmentConfig) ENRICHMENT.deserialize(json);

        } else {
          throw new IllegalArgumentException(format("Invalid configuration: unable to deserialize '%s'", json));
        }
      }

      processor = new ThreatTriageProcessor(config, new ClasspathFunctionResolver(), context);
      return processor;
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
          namespace = "THREAT_TRIAGE"
          ,name = "SCORE"
          ,description = "Scores a message using a set of triage rules."
          ,params = {
                  "message - a string containing the message to score.",
                  "engine - threat triage engine returned by THREAT_TRIAGE_INIT."}
          ,returns = "A threat triage engine."
  )
  public static class ThreatTriageScore implements StellarFunction {

    private JSONParser parser;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // the user must provide the message as a string
      String arg0 = Util.getArg(0, String.class, args);
      if(arg0 == null) {
        throw new IllegalArgumentException(format("expected string, got null"));
      }

      // parse the message
      JSONObject message;
      try {
        message = (JSONObject) parser.parse(arg0);
      } catch(org.json.simple.parser.ParseException e) {
        throw new IllegalArgumentException("invalid message", e);
      }

      // the user must provide the threat triage processor
      ThreatTriageProcessor processor = Util.getArg(1, ThreatTriageProcessor.class, args);
      if(processor == null) {
        throw new IllegalArgumentException(format("expected threat triage engine; got null"));
      }

      ThreatScore score = processor.apply(message);
      return transform(score, processor.getSensorConfig());
    }

    /**
     * Transforms a ThreatScore into a Map that can be more easily manipulated
     * in the REPL.
     * @param score The ThreatScore to transform.
     * @return The transformed ThreatScore.
     */
    private Map<String, Object> transform(ThreatScore score, SensorEnrichmentConfig config) {
      List<Map<String, Object>> scores = new ArrayList<>();
      for(RuleScore ruleScore: score.getRuleScores()) {

        // transform the score from each rule
        Map<String, Object> map = new HashMap<>();
        if(ruleScore.getRule().getName() != null) {
          map.put(RULE_NAME_KEY, ruleScore.getRule().getName());
        }
        if(ruleScore.getRule().getRule() != null) {
          map.put(RULE_EXPR_KEY, ruleScore.getRule().getRule());
        }
        if(ruleScore.getRule().getScore() != null) {
          map.put(RULE_SCORE_KEY, ruleScore.getRule().getScore());
        }
        if(ruleScore.getReason() != null) {
          map.put(RULE_REASON_KEY, ruleScore.getReason());
        }
        if(ruleScore.getRule().getComment() != null) {
          map.put(RULE_COMMENT_KEY, ruleScore.getRule().getComment());
        }
        scores.add(map);
      }

      // contains the total score and details on the score from each rule
      Map<String, Object> result = new HashMap<>();
      result.put(SCORE_KEY, score.getScore());
      result.put(RULES_KEY, scores);
      result.put(AGG_KEY, config.getThreatIntel().getTriageConfig().getAggregator().toString());
      return result;
    }

    @Override
    public void initialize(Context context) {
      parser = new JSONParser();
    }

    @Override
    public boolean isInitialized() {
      return parser != null;
    }
  }

  @Stellar(
          namespace = "THREAT_TRIAGE"
          ,name = "CONFIG"
          ,description = "Export the configuration used by a threat triage engine."
          ,params = { "engine - threat triage engine returned by THREAT_TRIAGE_INIT." }
          ,returns = "The configuration used by the threat triage engine."
  )
  public static class ThreatTriageConfig implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // the user must provide the threat triage processor
      ThreatTriageProcessor processor = Util.getArg(0, ThreatTriageProcessor.class, args);
      if(processor == null) {
        throw new IllegalArgumentException(format("expected threat triage engine; got null"));
      }

      // serialize the configuration to JSON
      SensorEnrichmentConfig config = processor.getSensorConfig();
      return toJSON(config);
    }

    @Override
    public void initialize(Context context) {
      // do nothing
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(
           namespace = "THREAT_TRIAGE"
          ,name = "PRINT"
          ,description = "Retrieve stellar enrichment transformations."
          ,params = { "config (or engine) - JSON configuration as a string (or Threat Triage engine returned by THREAT_TRIAGE_INIT)" }
          ,returns = "The String representation of the threat triage rules"
          )
  public static class GetStellarTransformation implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      SensorEnrichmentConfig config = getSensorEnrichmentConfig(args, 0);

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(config, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        return "";
      }
      org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        return "";
      }

      // print each rule
      List<RiskLevelRule> triageRules = ListUtils.emptyIfNull(triageConfig.getRiskLevelRules());
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

      // print the aggregation
      if(!triageRules.isEmpty()) {
        ret += "Aggregation: " + triageConfig.getAggregator().name();
      }
      return ret;
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
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
      SensorEnrichmentConfig config = getSensorEnrichmentConfig(args, 0);

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(config, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        config.setThreatIntel(tiConfig);
      }

      org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig();
        tiConfig.setTriageConfig(triageConfig);
      }

      // build the new rules
      List<RiskLevelRule> newRules = new ArrayList<>();
      for(Map<String, Object> newRule : getNewRuleDefinitions(args)) {

        if(newRule != null && newRule.containsKey("rule") && newRule.containsKey("score")) {

          // create the rule
          RiskLevelRule ruleToAdd = new RiskLevelRule();
          ruleToAdd.setRule((String) newRule.get(RULE_EXPR_KEY));
          ruleToAdd.setScore(ConversionUtils.convert(newRule.get(RULE_SCORE_KEY), Double.class));

          // add optional rule fields
          if (newRule.containsKey(RULE_NAME_KEY)) {
            ruleToAdd.setName((String) newRule.get(RULE_NAME_KEY));
          }
          if (newRule.containsKey(RULE_COMMENT_KEY)) {
            ruleToAdd.setComment((String) newRule.get(RULE_COMMENT_KEY));
          }
          if (newRule.containsKey(RULE_REASON_KEY)) {
            ruleToAdd.setReason((String) newRule.get(RULE_REASON_KEY));
          }
          newRules.add(ruleToAdd);
        }
      }

      // combine the new and existing rules
      List<RiskLevelRule> allRules = ListUtils.union(triageConfig.getRiskLevelRules(), newRules);
      triageConfig.setRiskLevelRules(allRules);

      return toJSON(config);
    }

    private List<Map<String, Object>> getNewRuleDefinitions(List<Object> args) {
      List<Map<String, Object>> newRules = new ArrayList<>();
      Object arg1 = Util.getArg(1, Object.class, args);
      if(arg1 instanceof Map) {
        newRules.add((Map<String, Object>) arg1);

      } else if(arg1 instanceof List) {
        newRules.addAll((List<Map<String, Object>>) arg1);

      } else {
        throw new IllegalArgumentException(String.format("triage rule expected to be map or list, got %s",
                ClassUtils.getShortClassName(arg1, "null")));
      } return newRules;
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
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
      SensorEnrichmentConfig config = getSensorEnrichmentConfig(args, 0);

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(config, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        config.setThreatIntel(tiConfig);
      }
      org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig();
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

      return toJSON(config);
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
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
      SensorEnrichmentConfig config = getSensorEnrichmentConfig(args, 0);

      ThreatIntelConfig tiConfig = (ThreatIntelConfig) getConfig(config, EnrichmentConfigFunctions.Type.THREAT_INTEL);
      if(tiConfig == null) {
        tiConfig = new ThreatIntelConfig();
        config.setThreatIntel(tiConfig);
      }
      org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig triageConfig = tiConfig.getTriageConfig();
      if(triageConfig == null) {
        triageConfig = new org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig();
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

      return toJSON(config);
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  /**
   *
   * Serializes the Enrichment configuration to JSON.
   * @param enrichmentConfig The Enrichment configuration to serialize to JSON.
   * @return The Enrichment configuration as JSON.
   */
  private static String toJSON(SensorEnrichmentConfig enrichmentConfig) {
    try {
      return JSONUtils.INSTANCE.toJSON(enrichmentConfig, true);

    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize enrichment config to JSON", e);
    }
  }

  /**
   * Retrieves the sensor enrichment configuration from the function arguments.  The manner
   * of retrieving the configuration can differ based on what the user passes in.
   * @param args The function arguments.
   * @param position The position from which the configuration will be extracted.
   * @return The sensor enrichment configuration.
   */
  private static SensorEnrichmentConfig getSensorEnrichmentConfig(List<Object> args, int position) {
    Object arg0 = Util.getArg(position, Object.class, args);
    SensorEnrichmentConfig config = new SensorEnrichmentConfig();
    if(arg0 instanceof String) {

      // deserialize the configuration from json
      String json = Util.getArg(0, String.class, args);
      if(json != null) {
        config = (SensorEnrichmentConfig) ENRICHMENT.deserialize(json);
      }
    } else if(arg0 instanceof ThreatTriageProcessor) {

      // extract the configuration from the engine
      ThreatTriageProcessor engine = Util.getArg(0, ThreatTriageProcessor.class, args);
      config = engine.getSensorConfig();

    } else {

      // unexpected type
      throw new IllegalArgumentException(String.format("Unexpected type: got '%s'", ClassUtils.getShortClassName(arg0, "null")));
    }

    return config;
  }
}
