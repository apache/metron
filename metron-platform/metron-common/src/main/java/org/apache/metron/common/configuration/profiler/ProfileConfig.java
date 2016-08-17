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
import java.util.HashMap;
import java.util.Map;

/**
 * The user defined configuration values required to generate a Profile.
 */
public class ProfileConfig implements Serializable {

  /**
   * The name of the profile.
   */
  private String profile;

  /**
   * Stella code that when executed results in the name of the entity being profiled.  A
   * profile is created 'for-each' of these; hence the name.
   */
  private String foreach;

  /**
   * Stella code that when executed determines whether a message should be included in this
   * profile.
   */
  private String onlyif;

  /**
   * Stella code that when executed results in a single measurement that is stored with the Profile.
   */
  private String result;

  /**
   * Defines how the state is initialized before any messages are received.
   */
  private Map<String, String> init = new HashMap<>();

  /**
   * Defines how the state is updated when a new message is received.
   */
  private Map<String, String> update = new HashMap<>();

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public String getForeach() {
    return foreach;
  }

  public void setForeach(String foreach) {
    this.foreach = foreach;
  }

  public String getOnlyif() {
    return onlyif;
  }

  public void setOnlyif(String onlyif) {
    this.onlyif = onlyif;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public Map<String, String> getInit() {
    return init;
  }

  public void setInit(Map<String, String> init) {
    this.init = init;
  }

  public Map<String, String> getUpdate() {
    return update;
  }

  public void setUpdate(Map<String, String> update) {
    this.update = update;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileConfig that = (ProfileConfig) o;
    if (profile != null ? !profile.equals(that.profile) : that.profile != null) return false;
    if (foreach != null ? !foreach.equals(that.foreach) : that.foreach != null) return false;
    if (onlyif != null ? !onlyif.equals(that.onlyif) : that.onlyif != null) return false;
    if (result != null ? !result.equals(that.result) : that.result != null) return false;
    if (init != null ? !init.equals(that.init) : that.init != null) return false;
    return update != null ? update.equals(that.update) : that.update == null;
  }

  @Override
  public int hashCode() {
    int result1 = profile != null ? profile.hashCode() : 0;
    result1 = 31 * result1 + (foreach != null ? foreach.hashCode() : 0);
    result1 = 31 * result1 + (onlyif != null ? onlyif.hashCode() : 0);
    result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
    result1 = 31 * result1 + (init != null ? init.hashCode() : 0);
    result1 = 31 * result1 + (update != null ? update.hashCode() : 0);
    return result1;
  }

  @Override
  public String toString() {
    return "ProfileConfig{" +
            "profile='" + profile + '\'' +
            ", foreach='" + foreach + '\'' +
            ", onlyif='" + onlyif + '\'' +
            ", result='" + result + '\'' +
            ", init=" + init +
            ", update=" + update +
            '}';
  }
}
