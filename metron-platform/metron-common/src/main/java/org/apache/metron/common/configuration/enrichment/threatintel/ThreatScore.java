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

import java.util.ArrayList;
import java.util.List;

/**
 * The overall threat score which is arrived at by aggregating the individual
 * scores from each applied rule.
 *
 * <p>The goal of threat triage is to prioritize the alerts that pose the greatest
 * threat and thus need urgent attention.  To perform threat triage, a set of rules
 * are applied to each message.  Each rule has a predicate to determine if the rule
 * applies or not.  The threat score from each applied rule is aggregated into a single
 * threat triage score that can be used to prioritize high risk threats.
 *
 * <p>Tuning the threat triage process involves creating one or more rules, adjusting
 * the score of each rule, and changing the way that each rule's score is aggregated.
 */
public class ThreatScore {

  /**
   * The numeric threat score resulting from aggregating
   * all of the individual scores from each applied rule.
   */
  private Double score;

  /**
   * The individual rule scores produced by applying each rule to the message.
   */
  private List<RuleScore> ruleScores;

  public ThreatScore() {
    this.ruleScores = new ArrayList<>();
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public List<RuleScore> getRuleScores() {
    return ruleScores;
  }

  public void addRuleScore(RuleScore score) {
    this.ruleScores.add(score);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ThreatScore that = (ThreatScore) o;

    if (score != null ? !score.equals(that.score) : that.score != null) return false;
    return ruleScores != null ? ruleScores.equals(that.ruleScores) : that.ruleScores == null;
  }

  @Override
  public int hashCode() {
    int result = score != null ? score.hashCode() : 0;
    result = 31 * result + (ruleScores != null ? ruleScores.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ThreatScore{" +
            "score=" + score +
            ", ruleScores=" + ruleScores +
            '}';
  }
}
