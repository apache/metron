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

/**
 * The user defined configuration values required for the Profiler.
 */
public class ProfilerConfig implements Serializable {

  /**
   * The input topic from which messages will be read.
   */
  private String inputTopic;

  /**
   * One or more profile definitions.
   */
  private List<ProfileConfig> profiles = new ArrayList<>();

  public String getInputTopic() {
    return inputTopic;
  }

  public void setInputTopic(String inputTopic) {
    this.inputTopic = inputTopic;
  }

  public List<ProfileConfig> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<ProfileConfig> profiles) {
    this.profiles = profiles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfilerConfig that = (ProfilerConfig) o;

    if (inputTopic != null ? !inputTopic.equals(that.inputTopic) : that.inputTopic != null) return false;
    return profiles != null ? profiles.equals(that.profiles) : that.profiles == null;

  }

  @Override
  public int hashCode() {
    int result = inputTopic != null ? inputTopic.hashCode() : 0;
    result = 31 * result + (profiles != null ? profiles.hashCode() : 0);
    return result;
  }
}
