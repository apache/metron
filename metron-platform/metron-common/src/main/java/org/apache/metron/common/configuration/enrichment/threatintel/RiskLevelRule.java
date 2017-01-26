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

public class RiskLevelRule {
  String name;
  String comment;
  String rule;
  Number score;

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

  @Override
  public String toString() {
    return "RiskLevelRule{" +
            "name='" + name + '\'' +
            ", comment='" + comment + '\'' +
            ", rule='" + rule + '\'' +
            ", score=" + score +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RiskLevelRule that = (RiskLevelRule) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
    if (rule != null ? !rule.equals(that.rule) : that.rule != null) return false;
    return score != null ? score.equals(that.score) : that.score == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (comment != null ? comment.hashCode() : 0);
    result = 31 * result + (rule != null ? rule.hashCode() : 0);
    result = 31 * result + (score != null ? score.hashCode() : 0);
    return result;
  }
}
