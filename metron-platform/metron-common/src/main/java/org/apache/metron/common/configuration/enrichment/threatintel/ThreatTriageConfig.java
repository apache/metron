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
import org.apache.metron.common.aggregator.Aggregator;
import org.apache.metron.common.aggregator.Aggregators;
import org.apache.metron.common.query.PredicateProcessor;

import java.util.HashMap;
import java.util.Map;

public class ThreatTriageConfig {
  private Map<String, Number> riskLevelRules = new HashMap<>();
  private Aggregator aggregator = Aggregators.MAX;
  private Map<String, Object> aggregationConfig = new HashMap<>();

  public Map<String, Number> getRiskLevelRules() {
    return riskLevelRules;
  }

  public void setRiskLevelRules(Map<String, Number> riskLevelRules) {
    this.riskLevelRules = riskLevelRules;
    PredicateProcessor processor = new PredicateProcessor();
    for(String rule : riskLevelRules.keySet()) {
      processor.validate(rule);
    }
  }

  public Aggregator getAggregator() {
    return aggregator;
  }

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

    if (getRiskLevelRules() != null ? !getRiskLevelRules().equals(that.getRiskLevelRules()) : that.getRiskLevelRules() != null)
      return false;
    if (getAggregator() != null ? !getAggregator().equals(that.getAggregator()) : that.getAggregator() != null)
      return false;
    return getAggregationConfig() != null ? getAggregationConfig().equals(that.getAggregationConfig()) : that.getAggregationConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getRiskLevelRules() != null ? getRiskLevelRules().hashCode() : 0;
    result = 31 * result + (getAggregator() != null ? getAggregator().hashCode() : 0);
    result = 31 * result + (getAggregationConfig() != null ? getAggregationConfig().hashCode() : 0);
    return result;
  }

}
