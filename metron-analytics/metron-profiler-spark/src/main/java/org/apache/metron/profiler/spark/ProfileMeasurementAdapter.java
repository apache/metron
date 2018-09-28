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
package org.apache.metron.profiler.spark;

import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An adapter for the {@link ProfileMeasurement} class so that the data
 * can be serialized as required by Spark.
 *
 * <p>The `Encoders.bean(Class<T>)` encoder does not handle serialization of type `Object` well. This
 * adapter encodes the profile's result as byte[] rather than an Object to work around this.
 */
public class ProfileMeasurementAdapter implements Serializable {

  /**
   * The name of the profile that this measurement is associated with.
   */
  private String profileName;

  /**
   * The name of the entity being profiled.
   */
  private String entity;

  /**
   * A monotonically increasing number identifying the period.  The first period is 0
   * and began at the epoch.
   */
  private Long periodId;

  /**
   * The duration of each period in milliseconds.
   */
  private Long durationMillis;

  /**
   * The result of evaluating the profile expression.
   *
   * The `Encoders.bean(Class<T>)` encoder does not handle serialization of type `Object`. This
   * adapter encodes the profile's result as `byte[]` rather than an `Object` to work around this.
   */
  private byte[] profileValue;

  public ProfileMeasurementAdapter() {
    // default constructor required for serialization in Spark
  }

  public ProfileMeasurementAdapter(ProfileMeasurement measurement) {
    this.profileName = measurement.getProfileName();
    this.entity = measurement.getEntity();
    this.periodId = measurement.getPeriod().getPeriod();
    this.durationMillis = measurement.getPeriod().getDurationMillis();
    this.profileValue = SerDeUtils.toBytes(measurement.getProfileValue());
  }

  public ProfileMeasurement toProfileMeasurement() {
    ProfilePeriod period = ProfilePeriod.fromPeriodId(periodId, durationMillis, TimeUnit.MILLISECONDS);
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName(profileName)
            .withEntity(entity)
            .withPeriod(period)
            .withProfileValue(SerDeUtils.fromBytes(profileValue, Object.class));
    return measurement;
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

  public Long getPeriodId() {
    return periodId;
  }

  public void setPeriodId(Long periodId) {
    this.periodId = periodId;
  }

  public Long getDurationMillis() {
    return durationMillis;
  }

  public void setDurationMillis(Long durationMillis) {
    this.durationMillis = durationMillis;
  }

  public byte[] getProfileValue() {
    return profileValue;
  }

  public void setProfileValue(byte[] profileValue) {
    this.profileValue = profileValue;
  }

  public void setProfileValue(Object profileValue) {
    this.profileValue = SerDeUtils.toBytes(profileValue);
  }
}
