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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The definition of a single Profile.
 */
public class ProfileConfig implements Serializable {

  /**
   * A unique name identifying the profile.  The field is treated as a string.
   */
  private String profile;

  /**
   * A separate profile is maintained for each of these.  This is effectively the
   * entity that the profile is describing.  The field is expected to contain a
   * Stellar expression whose result is the entity name.  For example, if `ip_src_addr`
   * then a separate profile would be maintained for each unique IP source address in
   * the data; 10.0.0.1, 10.0.0.2, etc.
   */
  private String foreach;

  /**
   * An expression that determines if a message should be applied to the profile.  A
   * Stellar expression is expected that when executed returns a boolean.  A message
   * is only applied to a profile if this condition is true. This allows a profile
   * to filter the messages that it receives.
   */
  private String onlyif = "true";

  /**
   * A set of expressions that is executed at the start of a window period.  A map is
   * expected where the key is the variable name and the value is a Stellar expression.
   * The map can contain 0 or more variables/expressions. At the start of each window
   * period the expression is executed once and stored in a variable with the given
   * name.
   */
  private Map<String, String> init = new HashMap<>();

  /**
   * A set of expressions that is executed when a message is applied to the profile.
   * A map is expected where the key is the variable name and the value is a Stellar
   * expression.  The map can include 0 or more variables/expressions.
   */
  private Map<String, String> update = new HashMap<>();

  /**
   * A list of Stellar expressions that is executed in order and used to group the
   * resulting profile data.
   */
  private List<String> groupBy = new ArrayList<>();

  /**
   * Stellar expression(s) that are executed when the window period expires.  The
   * expression(s) are expected to in some way summarize the messages that were applied
   * to the profile over the window period.
   */
  private ProfileResult result;

  /**
   * How long the data created by this Profile will be retained.  After this period of time the
   * profile data will be purged and no longer accessible.
   */
  private Long expires;

  public ProfileConfig() {
  }

  /**
   * A profile definition requires at the very least the profile name, the foreach, and result
   * expressions.
   * @param profile The name of the profile.
   * @param foreach The foreach expression of the profile.
   * @param result The result expression of the profile.
   */
  public ProfileConfig(
          @JsonProperty(value = "profile", required = true) String profile,
          @JsonProperty(value = "foreach", required = true) String foreach,
          @JsonProperty(value = "result",  required = true) ProfileResult result) {

    this.profile = profile;
    this.foreach = foreach;
    this.result = result;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public ProfileConfig withProfile(String profile) {
    this.profile = profile;
    return this;
  }

  public String getForeach() {
    return foreach;
  }

  public void setForeach(String foreach) {
    this.foreach = foreach;
  }

  public ProfileConfig withForeach(String foreach) {
    this.foreach = foreach;
    return this;
  }

  public String getOnlyif() {
    return onlyif;
  }

  public void setOnlyif(String onlyif) {
    this.onlyif = onlyif;
  }

  public ProfileConfig withOnlyif(String onlyif) {
    this.onlyif = onlyif;
    return this;
  }

  public Map<String, String> getInit() {
    return init;
  }

  public void setInit(Map<String, String> init) {
    this.init = init;
  }

  public ProfileConfig withInit(Map<String, String> init) {
    this.init.putAll(init);
    return this;
  }

  public ProfileConfig withInit(String var, String expression) {
    this.init.put(var, expression);
    return this;
  }

  public Map<String, String> getUpdate() {
    return update;
  }

  public void setUpdate(Map<String, String> update) {
    this.update = update;
  }

  public ProfileConfig withUpdate(Map<String, String> update) {
    this.update.putAll(update);
    return this;
  }

  public ProfileConfig withUpdate(String var, String expression) {
    this.update.put(var, expression);
    return this;
  }

  public List<String> getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(List<String> groupBy) {
    this.groupBy = groupBy;
  }

  public ProfileConfig withGroupBy(List<String> groupBy) {
    this.groupBy = groupBy;
    return this;
  }

  public ProfileResult getResult() {
    return result;
  }

  public void setResult(ProfileResult result) {
    this.result = result;
  }

  public ProfileConfig withResult(String profileExpression) {
    this.result = new ProfileResult(profileExpression);
    return this;
  }

  public Long getExpires() {
    return expires;
  }

  public void setExpires(Long expiresDays) {
    this.expires = expiresDays;
  }

  public ProfileConfig withExpires(Long expiresDays) {
    this.expires = TimeUnit.DAYS.toMillis(expiresDays);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProfileConfig that = (ProfileConfig) o;
    return new EqualsBuilder()
            .append(profile, that.profile)
            .append(foreach, that.foreach)
            .append(onlyif, that.onlyif)
            .append(init, that.init)
            .append(update, that.update)
            .append(groupBy, that.groupBy)
            .append(result, that.result)
            .append(expires, that.expires)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(profile)
            .append(foreach)
            .append(onlyif)
            .append(init)
            .append(update)
            .append(groupBy)
            .append(result)
            .append(expires)
            .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("profile", profile)
            .append("foreach", foreach)
            .append("onlyif", onlyif)
            .append("init", init)
            .append("update", update)
            .append("groupBy", groupBy)
            .append("result", result)
            .append("expires", expires)
            .toString();
  }

  /**
   * Deserialize a {@link ProfileConfig}.
   *
   * @param bytes Raw bytes containing a UTF-8 JSON String.
   * @return The Profile definition.
   * @throws IOException If unable to deserialize the bytes into a {@link ProfileConfig}
   */
  public static ProfileConfig fromBytes(byte[] bytes) throws IOException {
    return JSONUtils.INSTANCE.load(new String(bytes), ProfileConfig.class);
  }

  /**
   * Deserialize a {@link ProfileConfig}.
   *
   * @param json A String containing JSON.
   * @return The Profile definition.
   * @throws IOException If unable to deserialize the string into a {@link ProfileConfig}
   */
  public static ProfileConfig fromJSON(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }

  /**
   * Serialize the profile definition to a JSON string.
   *
   * @return The Profiler configuration serialized as a JSON string.
   * @throws JsonProcessingException If there's an error converting this to Json
   */
  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }
}
