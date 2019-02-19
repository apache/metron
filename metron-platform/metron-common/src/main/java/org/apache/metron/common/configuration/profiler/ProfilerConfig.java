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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.metron.common.utils.JSONUtils;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The configuration object for the Profiler, which may contain many Profile definitions.
 */
@JsonSerialize(include=Inclusion.NON_NULL)
public class ProfilerConfig implements Serializable {

  /**
   * One or more profile definitions.
   */
  private List<ProfileConfig> profiles = new ArrayList<>();

  /**
   * The name of a field containing the timestamp that is used to
   * generate profiles.
   *
   * <p>By default, the processing time of the Profiler is used rather
   * than event time; a value contained within the message itself.
   *
   * <p>The field must contain a timestamp in epoch milliseconds.
   *
   * <p>If a message does NOT contain this field, it will be dropped
   * and not included in any profiles.
   */
  private String timestampField = null;

  public List<ProfileConfig> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<ProfileConfig> profiles) {
    this.profiles = profiles;
  }

  public ProfilerConfig withProfile(ProfileConfig profileConfig) {
    this.profiles.add(profileConfig);
    return this;
  }

  @JsonGetter("timestampField")
  public String getTimestampFieldForJson() {
    return timestampField;
  }

  public Optional<String> getTimestampField() {
    return Optional.ofNullable(timestampField);
  }

  @JsonSetter("timestampField")
  public void setTimestampField(String timestampField) {
    this.timestampField = timestampField;
  }

  public void setTimestampField(Optional<String> timestampField) {
    this.timestampField = timestampField.orElse(null);
  }

  public ProfilerConfig withTimestampField(Optional<String> timestampField) {
    this.timestampField = timestampField.orElse(null);
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("profiles", profiles)
            .append("timestampField", timestampField)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProfilerConfig that = (ProfilerConfig) o;
    return new EqualsBuilder()
            .append(profiles, that.profiles)
            .append(timestampField, that.timestampField)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(profiles)
            .append(timestampField)
            .toHashCode();
  }

  /**
   * Deserialize a {@link ProfilerConfig}.
   *
   * @param bytes Raw bytes containing a UTF-8 JSON String.
   * @return The Profiler configuration.
   * @throws IOException If there's an error deserializing the raw bytes
   */
  public static ProfilerConfig fromBytes(byte[] bytes) throws IOException {
    return JSONUtils.INSTANCE.load(new String(bytes), ProfilerConfig.class);
  }

  /**
   * Deserialize a {@link ProfilerConfig}.
   *
   * @param json A String containing JSON.
   * @return The Profiler configuration.
   * @throws IOException If there's an error deserializing the string
   */
  public static ProfilerConfig fromJSON(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfilerConfig.class);
  }

  /**
   * Serialize a {@link ProfilerConfig} to a JSON string.
   *
   * @return The Profiler configuration serialized as a JSON string.
   * @throws JsonProcessingException If an error occurs serializing this as a Json string
   */
  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }
}
