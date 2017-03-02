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
 * This class represents a rule that is used to triage threats.
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
public class RiskLevelRule {

  /**
   * The name of the rule. This field is optional.
   */
  String name;

  /**
   * A description of the rule. This field is optional.
   */
  String comment;

  /**
   * A predicate, in the form of a Stellar expression, that determines whether
   * the rule is applied to an alert or not.  This field is required.
   */
  String rule;

  /**
   * A numeric value that represents the score that is applied to the alert. This
   * field is required.
   */
  Number score;

  /**
   * Allows a rule author to provide contextual information when a rule is applied
   * to a message.  This can assist a SOC analyst when actioning a threat.
   *
   * This is expected to be a valid Stellar expression and can refer to any of the
   * fields within the message itself.
   */
  String reason;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getRule() {
    return rule;
  }

  public void setRule(String rule) {
    this.rule = rule;
  }

  public Number getScore() {
    return score;
  }

  public void setScore(Number score) {
    this.score = score;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RiskLevelRule that = (RiskLevelRule) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
    if (rule != null ? !rule.equals(that.rule) : that.rule != null) return false;
    if (score != null ? !score.equals(that.score) : that.score != null) return false;
    return reason != null ? reason.equals(that.reason) : that.reason == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (comment != null ? comment.hashCode() : 0);
    result = 31 * result + (rule != null ? rule.hashCode() : 0);
    result = 31 * result + (score != null ? score.hashCode() : 0);
    result = 31 * result + (reason != null ? reason.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "RiskLevelRule{" +
            "name='" + name + '\'' +
            ", comment='" + comment + '\'' +
            ", rule='" + rule + '\'' +
            ", score=" + score +
            ", reason='" + reason + '\'' +
            '}';
  }
}
