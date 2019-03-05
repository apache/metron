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
package org.apache.metron.common.configuration.profiler;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of Stellar expressions that are executed to produce a single
 * measurement that can be interrogated by the threat triage process.
 *
 * <p>The result of evaluating each expression are made available, keyed
 * by the given name, to the threat triage process.
 */
public class ProfileTriageExpressions implements Serializable {

  /**
   * A set of named Stellar expressions.  The name of the expression
   * serves as the key and the value is the expression itself.
   *
   * <p>Evaluating the expression(s) must result in a basic data type
   * or map of basic data types that can be serialized.
   */
  @JsonIgnore
  private Map<String, String> expressions;

  @JsonCreator
  public ProfileTriageExpressions(Map<String, String> expressions) {
    this.expressions = expressions;
  }

  @JsonCreator
  public ProfileTriageExpressions() {
    this.expressions = new HashMap<>();
  }

  /**
   * Returns the expression associated with a given name.
   * @param name The name of the expression.
   * @return A Stellar expression.
   */
  public String getExpression(String name) {
    return expressions.get(name);
  }

  @JsonAnyGetter
  public Map<String, String> getExpressions() {
    return expressions;
  }

  @JsonAnySetter
  public void setExpressions(Map<String, String> expressions) {
    this.expressions = expressions;
  }

  @Override
  public String toString() {
    return "ProfileTriageExpressions{" +
            "expressions=" + expressions +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileTriageExpressions that = (ProfileTriageExpressions) o;

    return getExpressions() != null ? getExpressions().equals(that.getExpressions()) : that.getExpressions() == null;

  }

  @Override
  public int hashCode() {
    return getExpressions() != null ? getExpressions().hashCode() : 0;
  }
}
