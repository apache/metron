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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single data point within a Profile.
 *
 * A Profile is effectively a time series.  To this end a Profile is composed
 * of many ProfileMeasurement values which in aggregate form a time series.
 */
public class ProfileMeasurement {

  /**
   * The name of the profile that this measurement is associated with.
   */
  private String profileName;

  /**
   * The name of the entity being profiled.
   */
  private String entity;

  /**
   * The actual measurement itself.
   */
  private Object value;

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
   * @param profileName The name of the profile.
   * @param entity The name of the entity being profiled.
   * @param whenMillis When the measurement was taken in epoch milliseconds.
   * @param periodDuration The duration of each profile period.
   * @param periodUnits The units of the duration of each profile period.
   */
  public ProfileMeasurement(String profileName, String entity, long whenMillis, long periodDuration, TimeUnit periodUnits) {
    this.profileName = profileName;
    this.entity = entity;
    this.period = new ProfilePeriod(whenMillis, periodDuration, periodUnits);
    this.groups = Collections.emptyList();
  }

  public String getProfileName() {
    return profileName;
  }

  public String getEntity() {
    return entity;
  }

  public Object getValue() {
    return value;
  }

  public ProfilePeriod getPeriod() {
    return period;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public List<Object> getGroups() {
    return groups;
  }

  public void setGroups(List<Object> groups) {
    this.groups = groups;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileMeasurement that = (ProfileMeasurement) o;

    if (profileName != null ? !profileName.equals(that.profileName) : that.profileName != null) return false;
    if (entity != null ? !entity.equals(that.entity) : that.entity != null) return false;
    if (value != null ? !value.equals(that.value) : that.value != null) return false;
    if (groups != null ? !groups.equals(that.groups) : that.groups != null) return false;
    return period != null ? period.equals(that.period) : that.period == null;
  }

  @Override
  public int hashCode() {
    int result = profileName != null ? profileName.hashCode() : 0;
    result = 31 * result + (entity != null ? entity.hashCode() : 0);
    result = 31 * result + (value != null ? value.hashCode() : 0);
    result = 31 * result + (groups != null ? groups.hashCode() : 0);
    result = 31 * result + (period != null ? period.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ProfileMeasurement{" +
            "profileName='" + profileName + '\'' +
            ", entity='" + entity + '\'' +
            ", value=" + value +
            ", groups=" + groups +
            ", period=" + period +
            '}';
  }
}
