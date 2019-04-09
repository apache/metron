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

package org.apache.metron.threatintel.triage;

import com.google.common.base.Function;
import org.apache.metron.common.aggregator.Aggregators;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.RiskLevelRule;
import org.apache.metron.common.configuration.enrichment.threatintel.RuleScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatScore;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.stellar.common.StellarPredicateProcessor;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies the threat triage rules to an alert and produces a threat score that is
 * attached to the alert.
 *
 * The goal of threat triage is to prioritize the alerts that pose the greatest
 * threat and thus need urgent attention.  To perform threat triage, a set of rules
 * are applied to each message.  Each rule has a predicate to determine if the rule
 * applies or not.  The threat score from each applied rule is aggregated into a single
 * threat triage score that can be used to prioritize high risk threats.
 *
 * Tuning the threat triage process involves creating one or more rules, adjusting
 * the score of each rule, and changing the way that each rule's score is aggregated.
 */
public class ThreatTriageProcessor implements Function<Map, ThreatScore> {

  private SensorEnrichmentConfig sensorConfig;
  private ThreatIntelConfig threatIntelConfig;
  private ThreatTriageConfig threatTriageConfig;
  private Context context;
  private FunctionResolver functionResolver;

  public ThreatTriageProcessor( SensorEnrichmentConfig config
                              , FunctionResolver functionResolver
                              , Context context
                              )
  {
    this.threatIntelConfig = config.getThreatIntel();
    this.sensorConfig = config;
    this.threatTriageConfig = config.getThreatIntel().getTriageConfig();
    this.functionResolver = functionResolver;
    this.context = context;
  }

  /**
   * @param message The message being triaged.
   */
  @Nullable
  @Override
  public ThreatScore apply(@Nullable Map message) {

    ThreatScore threatScore = new ThreatScore();
    StellarPredicateProcessor predicateProcessor = new StellarPredicateProcessor();
    StellarProcessor processor = new StellarProcessor();
    VariableResolver variableResolver = new MapVariableResolver(message, sensorConfig.getConfiguration(), threatIntelConfig.getConfig());

    // attempt to apply each rule to the threat
    for(RiskLevelRule rule : threatTriageConfig.getRiskLevelRules()) {
      if(predicateProcessor.parse(rule.getRule(), variableResolver, functionResolver, context)) {

        // add the rule's score to the overall threat score
        String reason = execute(rule.getReason(), processor, variableResolver, String.class);
        Double score = execute(rule.getScoreExpression(), processor, variableResolver, Double.class);
        threatScore.addRuleScore(new RuleScore(rule, reason, score));
      }
    }

    // calculate the aggregate threat score
    List<Number> ruleScores = new ArrayList<>();
    for(RuleScore ruleScore: threatScore.getRuleScores()) {
      ruleScores.add(ruleScore.getScore());
    }
    Aggregators aggregators = threatTriageConfig.getAggregator();
    Double aggregateScore = aggregators.aggregate(ruleScores, threatTriageConfig.getAggregationConfig());
    threatScore.setScore(aggregateScore);

    return threatScore;
  }

  private <T> T execute(String expression, StellarProcessor processor, VariableResolver resolver, Class<T> clazz) {
    Object result = processor.parse(expression, resolver, functionResolver, context);
    return ConversionUtils.convert(result, clazz);
  }

  public List<RiskLevelRule> getRiskLevelRules() {
    return threatTriageConfig.getRiskLevelRules();
  }

  public SensorEnrichmentConfig getSensorConfig() {
    return sensorConfig;
  }

  @Override
  public String toString() {
    return String.format("ThreatTriage{%d rule(s)}", threatTriageConfig.getRiskLevelRules().size());
  }
}
