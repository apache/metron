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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The configuration object for the Profiler, which may contain many Profile definitions.
 */
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
  private Optional<String> timestampField = Optional.empty();

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

  public Optional<String> getTimestampField() {
    return timestampField;
  }

  public void setTimestampField(String timestampField) {
    this.timestampField = Optional.of(timestampField);
  }

  public void setTimestampField(Optional<String> timestampField) {
    this.timestampField = timestampField;
  }

  public ProfilerConfig withTimestampField(Optional<String> timestampField) {
    this.timestampField = timestampField;
    return this;
  }

  @Override
  public String toString() {
    return "ProfilerConfig{" +
            "profiles=" + profiles +
            ", timestampField='" + timestampField + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProfilerConfig that = (ProfilerConfig) o;
    if (profiles != null ? !profiles.equals(that.profiles) : that.profiles != null) return false;
    return timestampField != null ? timestampField.equals(that.timestampField) : that.timestampField == null;
  }

  @Override
  public int hashCode() {
    int result = profiles != null ? profiles.hashCode() : 0;
    result = 31 * result + (timestampField != null ? timestampField.hashCode() : 0);
    return result;
  }
}
