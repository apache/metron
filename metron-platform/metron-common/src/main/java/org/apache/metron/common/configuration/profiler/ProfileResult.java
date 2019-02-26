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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Defines the 'result' field of a Profile definition.
 */
public class ProfileResult implements Serializable {

  /**
   * A Stellar expression that is executed to produce
   * a measurement that is persisted in the profile store.
   */
  @JsonProperty("profile")
  private ProfileResultExpressions profileExpressions;

  /**
   * A set of named Stellar expressions that are executed
   * to produce a measurement that can be used for threat
   * triage.
   */
  @JsonProperty("triage")
  private ProfileTriageExpressions triageExpressions;

  public ProfileResult() {
    // no-arg constructor required for kryo serialization in storm
  }

  @JsonCreator
  public ProfileResult(
          @JsonProperty(value = "profile", required = true) ProfileResultExpressions profileExpressions,
          @JsonProperty(value = "triage") ProfileTriageExpressions triageExpressions) {
    this.profileExpressions = profileExpressions;
    this.triageExpressions = triageExpressions != null ? triageExpressions : new ProfileTriageExpressions();
  }

  /**
   * Allows a single result expression to be interpreted as a 'profile' expression.
   *
   * <p>The profile definition
   *    <pre>{@code {..., "result": "2 + 2" }}</pre>
   * is equivalent to
   *    <pre>{@code {..., "result": { "profile": "2 + 2" }}}</pre>
   *
   * @param expression The result expression.
   */
  public ProfileResult(String expression) {
    this.profileExpressions = new ProfileResultExpressions(expression);
    this.triageExpressions = new ProfileTriageExpressions();
  }

  public ProfileResultExpressions getProfileExpressions() {
    return profileExpressions;
  }

  public void setProfileExpressions(ProfileResultExpressions profileExpressions) {
    this.profileExpressions = profileExpressions;
  }

  public ProfileTriageExpressions getTriageExpressions() {
    return triageExpressions;
  }

  public void setTriageExpressions(ProfileTriageExpressions triageExpressions) {
    this.triageExpressions = triageExpressions;
  }

  @Override
  public String toString() {
    return "ProfileResult{" +
            "profileExpressions=" + profileExpressions +
            ", triageExpressions=" + triageExpressions +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileResult that = (ProfileResult) o;
    if (profileExpressions != null ? !profileExpressions.equals(that.profileExpressions) : that.profileExpressions != null)
      return false;
    return triageExpressions != null ? triageExpressions.equals(that.triageExpressions) : that.triageExpressions == null;
  }

  @Override
  public int hashCode() {
    int result = profileExpressions != null ? profileExpressions.hashCode() : 0;
    result = 31 * result + (triageExpressions != null ? triageExpressions.hashCode() : 0);
    return result;
  }
}
