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
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * <li>period - The period in which the measurement was taken. The first period starts at the epoch and increases monotonically.
 * </ul>
 */
public class SaltyRowKeyBuilder implements RowKeyBuilder {

  /**
   * Defines the byte order when encoding and decoding the row keys.
   */
  private static final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

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
    this(1000, 15, TimeUnit.MINUTES);
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
  public List<byte[]> encode(String profile, String entity, List<Object> groups, long start, long end) {
    // be forgiving of out-of-order start and end times; order is critical to this algorithm
    end = Math.max(start, end);
    start = Math.min(start, end);

    // find the starting period and advance until the end time is reached
    return ProfilePeriod.visitPeriods( start
                                        , end
                                        , periodDurationMillis
                                        , TimeUnit.MILLISECONDS
                                        , Optional.empty()
                                        , period -> encode(profile, entity, period, groups)
                                        );

  }

  /**
   * Builds a list of row keys necessary to retrieve a profile's measurements over
   * a time horizon.
   * <p>
   * This method is useful when attempting to read ProfileMeasurements stored in HBase.
   *
   * @param profile    The name of the profile.
   * @param entity     The name of the entity.
   * @param groups     The group(s) used to sort the profile data.
   * @param periods    The profile measurement periods to compute the rowkeys for
   * @return All of the row keys necessary to retrieve the profile measurements.
   */
  @Override
  public List<byte[]> encode(String profile, String entity, List<Object> groups, Iterable<ProfilePeriod> periods) {
    List<byte[]> rowKeys = new ArrayList<>();
    for(ProfilePeriod period : periods) {
      rowKeys.add(encode(profile, entity, period, groups));
    }
    return rowKeys;
  }

  /**
   * Builds the row key for a given profile measurement.
   * @param m The profile measurement.
   * @return The HBase row key.
   */
  @Override
  public byte[] encode(ProfileMeasurement m) {
    return encode(m.getProfileName(), m.getEntity(), m.getPeriod(), m.getGroups());
  }

  /**
   * Build the row key.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param period The period in which the measurement was taken.
   * @param groups The groups.
   * @return The HBase row key.
   */
  private byte[] encode(String profile, String entity, ProfilePeriod period, List<Object> groups) {
    return encode(profile, entity, period.getPeriod(), period.getDurationMillis(), groups);
  }

  /**
   * Build the row key.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param periodId The period identifier
   * @param groups The groups.
   * @return The HBase row key.
   */
  public byte[] encode(String profile, String entity, long periodId, long periodDurationMillis, List<Object> groups) {
    byte[] salt = encodeSalt(periodId, saltDivisor);
    byte[] profileB = Bytes.toBytes(profile);
    byte[] entityB = Bytes.toBytes(entity);
    byte[] groupB = encodeGroups(groups);

    int capacity = salt.length + profileB.length + entityB.length + groupB.length + (Integer.BYTES * 3) + (Long.BYTES * 2);
    ByteBuffer buffer = ByteBuffer
            .allocate(capacity)
            .order(byteOrder)
            .putInt(salt.length)
            .put(salt)
            .putInt(profileB.length)
            .put(profileB)
            .putInt(entityB.length)
            .put(entityB)
            .put(groupB)
            .putLong(periodId)
            .putLong(periodDurationMillis);

    return buffer.array();
  }

  /**
   * Decodes a row key to build a ProfileMeasurement containing all of the
   * relevant fields that exist within the row key.
   *
   * @param rowKey The row key to decode.
   * @return A ProfileMeasurement.
   */
  @Override
  public ProfileMeasurement decode(byte[] rowKey) {
    ByteBuffer buffer = ByteBuffer
            .wrap(rowKey)
            .order(byteOrder);

    // decode the salt
    int saltLength = buffer.getInt();
    byte[] salt = new byte[saltLength];
    buffer.get(salt);

    // decode the profile name
    int profileLength = buffer.getInt();
    byte[] profileBytes = new byte[profileLength];
    buffer.get(profileBytes);
    String profile = new String(profileBytes);

    // decode the entity
    int entityLength = buffer.getInt();
    byte[] entityBytes = new byte[entityLength];
    buffer.get(entityBytes);
    String entity = new String(entityBytes);

    // decode the groups
    List<Object> groups = new ArrayList<>();
    int numberOfGroups = buffer.getInt();
    for(int i = 0; i < numberOfGroups; i++) {

      int groupLength = buffer.getInt();
      byte[] groupBytes = new byte[groupLength];
      buffer.get(groupBytes);

      String group = new String(groupBytes);
      groups.add(group);
    }

    // decode the period
    long periodId = buffer.getLong();
    long duration = buffer.getLong();
    ProfilePeriod period = ProfilePeriod.buildFromPeriod(periodId, duration, TimeUnit.MILLISECONDS);

    return new ProfileMeasurement()
            .withProfileName(profile)
            .withEntity(entity)
            .withGroups(groups)
            .withProfilePeriod(period);
  }

  /**
   * Builds the 'group' component of the row key.
   * @param groups The groups to include in the row key.
   */
  private byte[] encodeGroups(List<Object> groups) {

    // encode each of the groups and determine their size
    int lengthOfGroups = 0;
    List<byte[]> groupBytes = new ArrayList<>();
    for(Object group : groups) {
      byte[] groupB = Bytes.toBytes(String.valueOf(group));
      groupBytes.add(groupB);
      lengthOfGroups += groupB.length;
    }

    // encode each of the groups
    ByteBuffer buffer = ByteBuffer
            .allocate((Integer.BYTES * (1 + groups.size())) + lengthOfGroups)
            .order(byteOrder)
            .putInt(groups.size());

    for(byte[] groupB : groupBytes) {
      buffer.putInt(groupB.length).put(groupB);
    }

    return buffer.array();
  }

  /**
   * Builds the 'time' portion of the row key
   * @param period The ProfilePeriod in which the ProfileMeasurement was taken.
   */
  private static byte[] encodePeriod(ProfilePeriod period) {
    return encodePeriod(period.getPeriod());
  }

  /**
   * Builds the 'time' portion of the row key
   * @param period the period
   */
  private static byte[] encodePeriod(long period) {
    return Bytes.toBytes(period);
  }

  /**
   * Calculates a salt value that is used as part of the row key.
   *
   * The salt is calculated as 'md5(period) % N' where N is a configurable value that ideally
   * is close to the number of nodes in the Hbase cluster.
   *
   * @param period The period in which a profile measurement is taken.
   */
  public static byte[] encodeSalt(ProfilePeriod period, int saltDivisor) {
    return encodeSalt(period.getPeriod(), saltDivisor);
  }

  /**
   * Calculates a salt value that is used as part of the row key.
   *
   * The salt is calculated as 'md5(period) % N' where N is a configurable value that ideally
   * is close to the number of nodes in the Hbase cluster.
   *
   * @param period The period
   * @param saltDivisor The salt divisor
   */
  public static byte[] encodeSalt(long period, int saltDivisor) {
    try {
      // an MD5 is 16 bytes aka 128 bits
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(encodePeriod(period));
      int salt = Bytes.toShort(hash) % saltDivisor;
      return Bytes.toBytes(salt);

    } catch(NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public SaltyRowKeyBuilder withPeriodDuration(long duration, TimeUnit units) {
    periodDurationMillis = units.toMillis(duration);
    return this;
  }

  public SaltyRowKeyBuilder setSaltDivisor(int saltDivisor) {
    this.saltDivisor = saltDivisor;
    return this;
  }
}
