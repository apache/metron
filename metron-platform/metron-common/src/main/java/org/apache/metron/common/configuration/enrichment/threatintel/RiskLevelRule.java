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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * This class represents a rule that is used to triage threats.
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
public class RiskLevelRule {

  /**
   * The name of the rule. This field is optional.
   */
  private String name;

  /**
   * A description of the rule. This field is optional.
   */
  private String comment;

  /**
   * A predicate, in the form of a Stellar expression, that determines whether
   * the rule is applied to an alert or not.  This field is required.
   */
  private String rule;

  /**
   * A Stellar expression that when evaluated results in a numeric score. The expression
   * can refer to fields within the message undergoing triage.
   */
  private String scoreExpression;

  /**
   * Allows a rule author to provide contextual information when a rule is applied
   * to a message.  This can assist a SOC analyst when actioning a threat.
   *
   * <p>This is expected to be a valid Stellar expression and can refer to any of the
   * fields within the message itself.
   */
  private String reason;

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

  @JsonProperty("score")
  public String getScoreExpression() {
    return scoreExpression;
  }

  /**
   * Sets the score expression based on an input object, taking care to properly handle numbers or
   * strings.
   *
   * @param scoreExpression The raw object containing the score expression.
   */
  @JsonProperty("score")
  public void setScoreExpression(Object scoreExpression) {
    if(scoreExpression instanceof Number) {
      // a numeric value was provided
      scoreExpression = Number.class.cast(scoreExpression).toString();

    } else if (scoreExpression instanceof String) {
      // a stellar expression was provided
      scoreExpression = String.class.cast(scoreExpression);

    } else {
      throw new IllegalArgumentException(String.format("Expected 'score' to be number or string, but got '%s'", scoreExpression));
    }

    this.scoreExpression = scoreExpression.toString();
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
    if (!(o instanceof RiskLevelRule)) return false;
    RiskLevelRule that = (RiskLevelRule) o;
    return Objects.equals(name, that.name) &&
            Objects.equals(comment, that.comment) &&
            Objects.equals(rule, that.rule) &&
            Objects.equals(scoreExpression, that.scoreExpression) &&
            Objects.equals(reason, that.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, comment, rule, scoreExpression, reason);
  }

  @Override
  public String toString() {
    return "RiskLevelRule{" +
            "name='" + name + '\'' +
            ", comment='" + comment + '\'' +
            ", rule='" + rule + '\'' +
            ", scoreExpression='" + scoreExpression + '\'' +
            ", reason='" + reason + '\'' +
            '}';
  }
}
