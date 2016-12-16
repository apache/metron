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
import java.util.List;
import java.util.stream.Collectors;

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
 * <li>period - The period in which the measurement was taken. The first period starts at the epoch and increases monotonically.
 * </ul>
 */
public class SaltyRowKeyBuilder implements RowKeyBuilder {

  /**
   * A salt can be prepended to the row key to help prevent hot-spotting.  The salt
   * divisor is used to generate the salt.  The salt divisor should be roughly equal
   * to the number of nodes in the Hbase cluster.
   */
  private int saltDivisor;

  public SaltyRowKeyBuilder() {
    this.saltDivisor = 1000;
  }

  public SaltyRowKeyBuilder(int saltDivisor) {
    this.saltDivisor = saltDivisor;
  }

  /**
   * Builds a list of row keys necessary to retrieve a profile's measurements over
   * a time horizon.
   *
   * This method is useful when attempting to read ProfileMeasurements stored in HBase.
   *
   * @param periods All of the profile periods for which row keys are needed.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param groups The group(s) used to sort the profile data.
   * @return All of the row keys necessary to retrieve the profile measurements.
   */
  @Override
  public List<byte[]> rowKeys(List<ProfilePeriod> periods, String profile, String entity, List<Object> groups) {

    return periods.stream()
            .map(period -> rowKey(period, profile, entity, groups))
            .collect(Collectors.toList());
  }

  /**
   * Builds the row key for a given profile measurement.
   * @param m The profile measurement.
   * @return The HBase row key.
   */
  @Override
  public byte[] rowKey(ProfileMeasurement m) {
    return rowKey(m.getPeriod(), m.getProfileName(), m.getEntity(), m.getGroups());
  }

  /**
   * Build the row key.
   * @param period The period in which the measurement was taken.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param groups The groups.
   * @return The HBase row key.
   */
  public byte[] rowKey(ProfilePeriod period, String profile, String entity, List<Object> groups) {

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
    byte[] profileBytes = Bytes.toBytes(profile);
    byte[] entityBytes = Bytes.toBytes(entity);
    return ByteBuffer
            .allocate(profileBytes.length + entityBytes.length)
            .put(profileBytes)
            .put(entityBytes)
            .array();
  }

  /**
   * Builds the 'group' component of the row key.
   * @param groups The groups to include in the row key.
   */
  private static byte[] groupKey(List<Object> groups) {
    StringBuilder builder = new StringBuilder();
    groups.forEach(g -> builder.append(g));
    return Bytes.toBytes(builder.toString());
  }

  /**
   * Builds the 'time' portion of the row key
   * @param period The ProfilePeriod in which the ProfileMeasurement was taken.
   */
  private static byte[] timeKey(ProfilePeriod period) {
    long thePeriod = period.getPeriod();
    return Bytes.toBytes(thePeriod);
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
      // an MD5 is 16 bytes aka 128 bits
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(timeKey(period));
      int salt = Bytes.toShort(hash) % saltDivisor;
      return Bytes.toBytes(salt);

    } catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public SaltyRowKeyBuilder withSaltDivisor(int saltDivisor) {
    this.saltDivisor = saltDivisor;
    return this;
  }
}
