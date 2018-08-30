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

package org.apache.metron.profiler;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.metron.common.configuration.profiler.ProfileConfig;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single data point within a profile.
 *
 * <p>A profile contains many individual {@link ProfileMeasurement} values captured over a
 * period of time.  These values in aggregate form a time series.
 */
public class ProfileMeasurement implements Serializable {

  /**
   * The name of the profile that this measurement is associated with.
   */
  private String profileName;

  /**
   * The name of the entity being profiled.
   */
  private String entity;

  /**
   * The 'groups' used to sort the Profile data. The groups are the result of
   * executing the Profile's 'groupBy' expression.
   */
  private List<Object> groups;

  /**
   * The period in which the ProfileMeasurement was taken.
   */
  private ProfilePeriod period;

  /**
   * The profile definition that resulted in this measurement.
   */
  private ProfileConfig definition;

  /**
   * The result of evaluating the profile expression.
   */
  private Object profileValue;

  /**
   * The result of evaluating the triage expression(s).
   *
   * A profile can generate one or more values that can be used during the
   * threat triage process.  Each value is given a unique name.
   */
  private Map<String, Object> triageValues;

  public ProfileMeasurement() {
    this.groups = Collections.emptyList();
  }

  public ProfileMeasurement withProfileName(String profileName) {
    this.profileName = profileName;
    return this;
  }

  public ProfileMeasurement withEntity(String entity) {
    this.entity = entity;
    return this;
  }

  public ProfileMeasurement withGroups(List<Object> groups) {
    this.groups = groups;
    return this;
  }

  public ProfileMeasurement withPeriod(long whenMillis, long periodDuration, TimeUnit periodUnits) {
    this.withPeriod(new ProfilePeriod(whenMillis, periodDuration, periodUnits));
    return this;
  }

  public ProfileMeasurement withPeriod(ProfilePeriod period) {
    this.period = period;
    return this;
  }

  public ProfileMeasurement withDefinition(ProfileConfig definition) {
    this.definition = definition;
    return this;
  }

  public ProfileMeasurement withProfileValue(Object profileValue) {
    this.profileValue = profileValue;
    return this;
  }

  public ProfileMeasurement withTriageValues(Map<String, Object> triageValues) {
    this.triageValues = triageValues;
    return this;
  }

  public String getProfileName() {
    return profileName;
  }

  public void setProfileName(String profileName) {
    this.profileName = profileName;
  }

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public List<Object> getGroups() {
    return groups;
  }

  public void setGroups(List<Object> groups) {
    this.groups = groups;
  }

  public ProfilePeriod getPeriod() {
    return period;
  }

  public void setPeriod(ProfilePeriod period) {
    this.period = period;
  }

  public ProfileConfig getDefinition() {
    return definition;
  }

  public void setDefinition(ProfileConfig definition) {
    this.definition = definition;
  }

  public Object getProfileValue() {
    return profileValue;
  }

  public void setProfileValue(Object profileValue) {
    this.profileValue = profileValue;
  }

  public Map<String, Object> getTriageValues() {
    return triageValues;
  }

  public void setTriageValues(Map<String, Object> triageValues) {
    this.triageValues = triageValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileMeasurement that = (ProfileMeasurement) o;
    return new EqualsBuilder()
            .append(profileName, that.profileName)
            .append(entity, that.entity)
            .append(groups, that.groups)
            .append(period, that.period)
            .append(definition, that.definition)
            .append(profileValue, that.profileValue)
            .append(triageValues, that.triageValues)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(profileName)
            .append(entity)
            .append(groups)
            .append(period)
            .append(definition)
            .append(profileValue)
            .append(triageValues)
            .toHashCode();
  }
}
