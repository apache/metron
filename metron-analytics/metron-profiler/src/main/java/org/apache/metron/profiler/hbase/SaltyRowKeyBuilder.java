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

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A RowKeyBuilder that uses a salt to prevent hot-spotting.
 *
 * Responsible for building the row keys used to store profile data in HBase.  The row key is composed of the following
 * fields in the given order.
 * <ul>
 * <li>salt - A salt that helps prevent hot-spotting.
 * <li>profile - The name of the profile.
 * <li>entity - The name of the entity being profiled.
 * <li>group(s) - The group(s) used to sort the data in HBase. For example, a group may distinguish between weekends and weekdays.
 * <li>year - The year based on UTC.
 * <li>day of year - The current day within the year based on UTC; [1, 366]
 * <li>hour - The hour within the day based on UTC; [0, 23]
 * </ul>period - The period within the hour.  The number of periods per hour can be defined by the user; defaults to 4.
 */
public class SaltyRowKeyBuilder implements RowKeyBuilder {

  /**
   * A salt can be prepended to the row key to help prevent hot-spotting.  The salt
   * divisor is used to generate the salt.  The salt divisor should be roughly equal
   * to the number of nodes in the Hbase cluster.
   */
  private int saltDivisor;

  /**
   * The duration of each profile period in milliseconds.
   */
  private long periodDurationMillis;

  public SaltyRowKeyBuilder() {
    this.saltDivisor = 1000;
    this.periodDurationMillis = TimeUnit.MINUTES.toMillis(15);
  }

  public SaltyRowKeyBuilder(int saltDivisor, long duration, TimeUnit units) {
    this.saltDivisor = saltDivisor;
    this.periodDurationMillis = units.toMillis(duration);
  }

  /**
   * Builds a list of row keys necessary to retrieve profile measurements over
   * a time horizon.
   *
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param groups The group(s) used to sort the profile data.
   * @param start When the time horizon starts in epoch milliseconds.
   * @param end When the time horizon ends in epoch milliseconds.
   * @return All of the row keys necessary to retrieve the profile measurements.
   */
  @Override
  public List<byte[]> rowKeys(String profile, String entity, List<Object> groups, long start, long end) {
    List<byte[]> rowKeys = new ArrayList<>();

    // be forgiving of out-of-order start and end times; order is critical to this algorithm
    end = Math.max(start, end);
    start = Math.min(start, end);

    // find the starting period and advance until the end time is reached
    ProfilePeriod period = new ProfilePeriod(start, periodDurationMillis, TimeUnit.MILLISECONDS);
    while(period.getStartTimeMillis() <= end) {

      byte[] k = rowKey(profile, entity, period, groups);
      rowKeys.add(k);

      // advance to the next period
      period = period.next();
    }

    return rowKeys;
  }

  /**
   * Builds the row key for a given profile measurement.
   * @param m The profile measurement.
   * @param groups The groups used to sort the profile data.
   * @return The HBase row key.
   */
  @Override
  public byte[] rowKey(ProfileMeasurement m, List<Object> groups) {
    return rowKey(m.getProfileName(), m.getEntity(), m.getPeriod(), groups);
  }

  public void withPeriodDuration(long duration, TimeUnit units) {
    periodDurationMillis = units.toMillis(duration);
  }

  public void setSaltDivisor(int saltDivisor) {
    this.saltDivisor = saltDivisor;
  }

  /**
   * Build the row key.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param period The period in which the measurement was taken.
   * @param groups The groups.
   * @return The HBase row key.
   */
  public byte[] rowKey(String profile, String entity, ProfilePeriod period, List<Object> groups) {

    // row key = salt + prefix + group(s) + time
    byte[] salt = getSalt(period, saltDivisor);
    byte[] prefixKey = prefixKey(profile, entity);
    byte[] groupKey = groupKey(groups);
    byte[] timeKey = timeKey(period);

    int capacity = salt.length + prefixKey.length + groupKey.length + timeKey.length;
    return ByteBuffer
            .allocate(capacity)
            .put(salt)
            .put(prefixKey)
            .put(groupKey)
            .put(timeKey)
            .array();
  }

  /**
   * Builds the 'prefix' component of the row key.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   */
  private static byte[] prefixKey(String profile, String entity) {
    return ByteBuffer
            .allocate(profile.length() + entity.length())
            .put(profile.getBytes())
            .put(entity.getBytes())
            .array();
  }

  /**
   * Builds the 'group' component of the row key.
   * @param groups The groups to include in the row key.
   */
  private static byte[] groupKey(List<Object> groups) {

    StringBuilder builder = new StringBuilder();
    groups.forEach(g -> builder.append(g));
    String groupStr = builder.toString();

    return ByteBuffer
            .allocate(groupStr.length())
            .put(groupStr.getBytes())
            .array();
  }

  /**
   * Builds the 'time' portion of the row key
   * @param period The ProfilePeriod in which the ProfileMeasurement was taken.
   */
  private static byte[] timeKey(ProfilePeriod period) {
    return ByteBuffer
            .allocate(Long.BYTES)
            .putLong(period.getPeriod())
            .array();
  }

  /**
   * Calculates a salt value that is used as part of the row key.
   *
   * The salt is calculated as 'md5(timestamp) % N' where N is a configurable value that ideally
   * is close to the number of nodes in the Hbase cluster.
   *
   * @param period The period in which a profile measurement is taken.
   */
  public static byte[] getSalt(ProfilePeriod period, int saltDivisor) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(timeKey(period));
      int salt = Bytes.toInt(hash) % saltDivisor;
      return ByteBuffer
              .allocate(Integer.BYTES)
              .putInt(salt)
              .array();

    } catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

}
