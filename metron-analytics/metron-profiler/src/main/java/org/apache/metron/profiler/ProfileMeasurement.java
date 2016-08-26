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
   * When the measurement window was started in milliseconds since the epoch.
   */
  private long start;

  /**
   * When the measurement window closed in milliseconds since the epoch.
   */
  private long end;

  /**
   * The actual measurement itself.
   */
  private Object value;

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

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileMeasurement that = (ProfileMeasurement) o;

    if (start != that.start) return false;
    if (end != that.end) return false;
    if (profileName != null ? !profileName.equals(that.profileName) : that.profileName != null) return false;
    if (entity != null ? !entity.equals(that.entity) : that.entity != null) return false;
    return value != null ? value.equals(that.value) : that.value == null;

  }

  @Override
  public int hashCode() {
    int result = profileName != null ? profileName.hashCode() : 0;
    result = 31 * result + (entity != null ? entity.hashCode() : 0);
    result = 31 * result + (int) (start ^ (start >>> 32));
    result = 31 * result + (int) (end ^ (end >>> 32));
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ProfileMeasurement{" +
            "profileName='" + profileName + '\'' +
            ", entity='" + entity + '\'' +
            ", start=" + start +
            ", end=" + end +
            ", value=" + value +
            '}';
  }
}
