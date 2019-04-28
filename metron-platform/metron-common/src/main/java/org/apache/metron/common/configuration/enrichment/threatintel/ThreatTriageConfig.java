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

package org.apache.metron.common.configuration.enrichment.threatintel;

import com.google.common.base.Joiner;
import org.apache.metron.common.aggregator.Aggregators;
import org.apache.metron.stellar.common.StellarPredicateProcessor;
import org.apache.metron.stellar.common.StellarProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThreatTriageConfig {

  private List<RiskLevelRule> riskLevelRules = new ArrayList<>();
  private Aggregators aggregator = Aggregators.MAX;
  private Map<String, Object> aggregationConfig = new HashMap<>();

  public List<RiskLevelRule> getRiskLevelRules() {
    return riskLevelRules;
  }

  /**
   * Given a list of @{link RiskLevelRule}, builds up the necessary context to evaluate them.
   * This includes validation of the Stellar expression contained.
   *
   * @param riskLevelRules The list of {@link RiskLevelRule}s to be evaluated
   */
  public void setRiskLevelRules(List<RiskLevelRule> riskLevelRules) {
    List<RiskLevelRule> rules = new ArrayList<>();
    Set<String> ruleIndex = new HashSet<>();
    StellarPredicateProcessor predicateProcessor = new StellarPredicateProcessor();
    StellarProcessor processor = new StellarProcessor();

    for(RiskLevelRule rule : riskLevelRules) {
      if(rule.getRule() == null || rule.getScoreExpression() == null) {
        throw new IllegalStateException("Risk level rules must contain both a rule and a score.");
      }
      if(ruleIndex.contains(rule.getRule())) {
        continue;
      }
      else {
        ruleIndex.add(rule.getRule());
      }

      // validate the fields which are expected to be valid Stellar expressions
      predicateProcessor.validate(rule.getRule());
      if(rule.getReason() != null) {
        processor.validate(rule.getReason());
      }

      rules.add(rule);
    }
    this.riskLevelRules = rules;
  }

  public Aggregators getAggregator() {
    return aggregator;
  }

  /**
   * Sets an aggregator by name from {@link Aggregators}.
   *
   * @param aggregator The aggregator name to grab
   */
  public void setAggregator(String aggregator) {
    try {
      this.aggregator = Aggregators.valueOf(aggregator);
    }
    catch(IllegalArgumentException iae) {
      throw new IllegalArgumentException("Unable to load aggregator of " + aggregator
                                        + ".  Valid aggregators are " + Joiner.on(',').join(Aggregators.values())
                                        );
    }
  }

  public Map<String, Object> getAggregationConfig() {
    return aggregationConfig;
  }

  public void setAggregationConfig(Map<String, Object> aggregationConfig) {
    this.aggregationConfig = aggregationConfig;
  }

  @Override
  public String toString() {
    return "ThreatTriageConfig{" +
            "riskLevelRules=" + riskLevelRules +
            ", aggregator=" + aggregator +
            ", aggregationConfig=" + aggregationConfig +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ThreatTriageConfig that = (ThreatTriageConfig) o;

    if (riskLevelRules != null ? !riskLevelRules.equals(that.riskLevelRules) : that.riskLevelRules != null)
      return false;
    if (aggregator != that.aggregator) return false;
    return aggregationConfig != null ? aggregationConfig.equals(that.aggregationConfig) : that.aggregationConfig == null;

  }

  @Override
  public int hashCode() {
    int result = riskLevelRules != null ? riskLevelRules.hashCode() : 0;
    result = 31 * result + (aggregator != null ? aggregator.hashCode() : 0);
    result = 31 * result + (aggregationConfig != null ? aggregationConfig.hashCode() : 0);
    return result;
  }
}
