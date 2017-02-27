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

/**
 * This class represents the score resulting from applying a RiskLevelRule
 * to a message.
 *
 * The goal of threat triage is to prioritize the alerts that pose the greatest
 * threat and thus need urgent attention.  To perform threat triage, a set of rules
 * are applied to each message.  Each rule has a predicate to determine if the rule
 * applies or not.  The threat score from each applied rule is aggregated into a single
 * threat triage score that can be used to prioritize high risk threats.
 */
public class RuleScore {

  /**
   * The rule that when applied to a message resulted in this score.
   */
  RiskLevelRule rule;

  /**
   * Allows a rule author to provide contextual information when a rule is applied
   * to a message.  This can assist a SOC analyst when actioning a threat.
   *
   * This is the result of executing the 'reason' Stellar expression from the
   * associated RiskLevelRule.
   */
  private String reason;

  /**
   * @param rule The threat triage rule that when applied resulted in this score.
   * @param reason The result of executing the rule's 'reason' expression.  Provides context to why a rule was applied.
   */
  public RuleScore(RiskLevelRule rule, String reason) {
    this.rule = rule;
    this.reason = reason;
  }

  public String getReason() {
    return reason;
  }

  public RiskLevelRule getRule() {
    return rule;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RuleScore that = (RuleScore) o;

    if (rule != null ? !rule.equals(that.rule) : that.rule != null) return false;
    return reason != null ? reason.equals(that.reason) : that.reason == null;
  }

  @Override
  public int hashCode() {
    int result = rule != null ? rule.hashCode() : 0;
    result = 31 * result + (reason != null ? reason.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "RuleScore{" +
            "rule=" + rule +
            ", reason='" + reason + '\'' +
            '}';
  }
}
