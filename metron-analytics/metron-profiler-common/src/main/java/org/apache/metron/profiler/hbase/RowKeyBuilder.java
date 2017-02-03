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

package org.apache.metron.profiler.hbase;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds a row key that can be used to read or write ProfileMeasurement data
 * to HBase.
 */
public interface RowKeyBuilder extends Serializable {

  /**
   * Build a row key for a given ProfileMeasurement.
   * 
   * This method is useful when writing ProfileMeasurements to HBase.
   *
   * @param measurement The profile measurement.
   * @return The HBase row key.
   */
  byte[] rowKey(ProfileMeasurement measurement);

  /**
   * Builds a list of row keys necessary to retrieve a profile's measurements over
   * a time horizon.
   *
   * This method is useful when attempting to read ProfileMeasurements stored in HBase.
   *
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param groups The group(s) used to sort the profile data.
   * @param start When the time horizon starts in epoch milliseconds.
   * @param end When the time horizon ends in epoch milliseconds.
   * @return All of the row keys necessary to retrieve the profile measurements.
   */
  List<byte[]> rowKeys(String profile, String entity, List<Object> groups, long start, long end);

  /**
   * Builds a list of row keys necessary to retrieve a profile's measurements over
   * a time horizon.
   *
   * This method is useful when attempting to read ProfileMeasurements stored in HBase.
   *
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param groups The group(s) used to sort the profile data.
   * @param periods The profile measurement periods to compute the rowkeys for
   * @return All of the row keys necessary to retrieve the profile measurements.
   */
  List<byte[]> rowKeys(String profile, String entity, List<Object> groups, Iterable<ProfilePeriod> periods);

}
