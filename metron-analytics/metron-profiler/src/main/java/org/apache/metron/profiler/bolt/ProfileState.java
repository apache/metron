/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.profiler.bolt;

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.StellarExecutor;

/**
 * The state that must be maintained for each [profile, entity] pair when building a Profile.
 */
public class ProfileState {

  /**
   * A ProfileMeasurement is created and emitted each window period.  A Profile
   * itself is composed of many ProfileMeasurements.
   */
  private ProfileMeasurement measurement;

  /**
   * The definition of the Profile that the bolt is building.
   */
  private ProfileConfig definition;

  /**
   * Executes Stellar code and maintains state across multiple invocations.
   */
  private StellarExecutor executor;

  public ProfileMeasurement getMeasurement() {
    return measurement;
  }

  public void setMeasurement(ProfileMeasurement measurement) {
    this.measurement = measurement;
  }

  public ProfileConfig getDefinition() {
    return definition;
  }

  public void setDefinition(ProfileConfig definition) {
    this.definition = definition;
  }

  public StellarExecutor getExecutor() {
    return executor;
  }

  public void setExecutor(StellarExecutor executor) {
    this.executor = executor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileState that = (ProfileState) o;

    if (measurement != null ? !measurement.equals(that.measurement) : that.measurement != null) return false;
    if (definition != null ? !definition.equals(that.definition) : that.definition != null) return false;
    return executor != null ? executor.equals(that.executor) : that.executor == null;

  }

  @Override
  public int hashCode() {
    int result = measurement != null ? measurement.hashCode() : 0;
    result = 31 * result + (definition != null ? definition.hashCode() : 0);
    result = 31 * result + (executor != null ? executor.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ProfileState{" +
            "measurement=" + measurement +
            ", definition=" + definition +
            ", executor=" + executor +
            '}';
  }
}
