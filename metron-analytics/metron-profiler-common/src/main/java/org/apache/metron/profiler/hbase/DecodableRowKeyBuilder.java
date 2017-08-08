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
import org.apache.hadoop.util.hash.Hash;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.ProfilerClientConfig.PROFILER_SALT_DIVISOR;

/**
 * Responsible for building the row keys used to store profile data in HBase.
 *
 * This builder generates decodable row keys.  A decodable row key is one that can be interrogated to extract
 * the constituent components of that row key.  Given a previously generated row key this builder
 * can extract the profile name, entity name, group name(s), period duration, and period.
 *
 * The row key is composed of the following fields.
 * <ul>
 * <li>magic number - Helps to validate the row key.</li>
 * <li>version - The version number of the row key.</li>
 * <li>salt - A salt that helps prevent hot-spotting.
 * <li>profile - The name of the profile.
 * <li>entity - The name of the entity being profiled.
 * <li>group(s) - The group(s) used to sort the data in HBase. For example, a group may distinguish between weekends and weekdays.
 * <li>period - The period in which the measurement was taken. The first period starts at the epoch and increases monotonically.
 * </ul>
 */
public class DecodableRowKeyBuilder implements RowKeyBuilder {

  /**
   * Defines the byte order when encoding and decoding the row keys.
   *
   * Making this configurable is likely not necessary and is left as a practice exercise for the reader. :)
   */
  private static final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

  /**
   * Defines some level of sane max field length to avoid any shenanigans with oddly encoded row keys.
   */
  private static final int MAX_FIELD_LENGTH = 1000;

  /**
   * Defines the maximum number of groups allowed.
   */
  private static final int MAX_NUMBER_OF_GROUPS = 10;

  /**
   * The seed for the Murmur hash function that is used to generate the salt value.
   *
   * The seed can be any value, but whatever the value is, it must always be the same
   * so that row key generation is deterministic.  Defining this value here avoids
   * a potential future problem should the default seed change in the underlying
   * implementation library.
   */
  private static final int MURMUR_HASH_SEED = 8658992;

  /**
   * A magic number embedded in each row key to help validate the row key and byte ordering when decoding.
   */
  protected static final short MAGIC_NUMBER = 77;

  /**
   * The version number of the row keys supported by this builder.
   */
  protected static final byte VERSION = (byte) 1;

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

  public DecodableRowKeyBuilder() {
    this(PROFILER_SALT_DIVISOR.getDefault(Integer.class),
            PROFILER_PERIOD.getDefault(Long.class),
            TimeUnit.valueOf(PROFILER_PERIOD_UNITS.getDefault(String.class)));
  }

  public DecodableRowKeyBuilder(int saltDivisor, long duration, TimeUnit units) {
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
            , period -> encode(profile, entity, groups, period)
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
      rowKeys.add(encode(profile, entity, groups, period));
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
    return encode(m.getProfileName(), m.getEntity(), m.getGroups(), m.getPeriod());
  }

  /**
   * Build the row key.
   * @param profile The name of the profile.
   * @param entity The name of the entity.
   * @param period The period in which the measurement was taken.
   * @param groups The groups.
   * @return The HBase row key.
   */
  public byte[] encode(String profile, String entity, List<Object> groups, ProfilePeriod period) {

    if(profile == null)
      throw new IllegalArgumentException("Unable to encode; missing profile name.");
    if(entity == null)
      throw new IllegalArgumentException("Unable to encode; missing entity name.");
    if(period == null)
      throw new IllegalArgumentException("Unable to encode; missing profile period.");

    byte[] salt = encodeSalt(period, saltDivisor);

    byte[] profileB = Bytes.toBytes(profile);
    validateField(profileB, "profile");

    byte[] entityB = Bytes.toBytes(entity);
    validateField(entityB, "entity");

    byte[] groupB = encodeGroups(groups);
    validateField(groupB, "group");

    int capacity = Short.BYTES + 1 + salt.length + profileB.length + entityB.length + groupB.length + (Integer.BYTES * 3) + (Long.BYTES * 2);
    ByteBuffer buffer = ByteBuffer
            .allocate(capacity)
            .order(byteOrder)
            .putShort(MAGIC_NUMBER)
            .put(VERSION)
            .putInt(salt.length)
            .put(salt)
            .putInt(profileB.length)
            .put(profileB)
            .putInt(entityB.length)
            .put(entityB)
            .put(groupB)
            .putLong(period.getPeriod())
            .putLong(periodDurationMillis);

    return buffer.array();
  }

  /**
   * Validates a field.  A runtime exception is thrown if a field is not valid.
   * @param field The field value.
   * @param fieldName The name of the field.
   */
  private void validateField(byte[] field, String fieldName) {
    validateLength(field.length, fieldName);
  }

  /**
   * Validates the length of a field.  A runtime exception is thrown if a field is not valid.
   * @param fieldLength The length of the field.
   * @param fieldName The name of the field.
   */
  private void validateLength(int fieldLength, String fieldName) {
    if(fieldLength <= 0 || fieldLength > MAX_FIELD_LENGTH) {
      throw new IllegalArgumentException(String.format("'%s' too long; max '%d', got '%d'", fieldName, MAX_FIELD_LENGTH, fieldLength));
    }
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

    try {
      // validate the magic number
      short magicNumber = buffer.getShort();
      if(magicNumber != MAGIC_NUMBER) {
        throw new IllegalArgumentException(String.format("Invalid magic number; expected '%s', got '%s'", MAGIC_NUMBER, magicNumber));
      }

      // validate the row key version
      byte version = buffer.get();
      if(version != VERSION) {
        throw new IllegalArgumentException(String.format("Invalid version; expected '%s', got '%s'", VERSION, version));
      }

      // validate the salt length
      int saltLength = buffer.getInt();
      validateLength(saltLength, "salt");

      // decode the salt
      byte[] salt = new byte[saltLength];
      buffer.get(salt);

      // validate the profile length
      int profileLength = buffer.getInt();
      validateLength(profileLength, "profile");

      // decode the profile name
      byte[] profileBytes = new byte[profileLength];
      buffer.get(profileBytes);
      String profile = new String(profileBytes);

      // validate the entity length
      int entityLength = buffer.getInt();
      validateLength(entityLength, "entity");

      // decode the entity
      byte[] entityBytes = new byte[entityLength];
      buffer.get(entityBytes);
      String entity = new String(entityBytes);

      // validate the number of groups
      int numberOfGroups = buffer.getInt();
      if(numberOfGroups > MAX_NUMBER_OF_GROUPS) {
        throw new IllegalArgumentException(String.format("too many groups; max '%d', got '%d'", MAX_NUMBER_OF_GROUPS, numberOfGroups));
      }

      // decode the groups
      List<Object> groups = new ArrayList<>();
      for (int i = 0; i < numberOfGroups; i++) {

        // validate the group length
        int groupLength = buffer.getInt();
        validateLength(groupLength, "group-" + i);

        // decode the group
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

    } catch(BufferUnderflowException e) {
      throw new IllegalArgumentException("Unable to decode the row key", e);
    }

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
   * Calculates a salt value that is used as part of the row key.
   *
   * The salt is calculated as 'hash(period) % N' where N is a configurable value that ideally
   * is close to the number of nodes in the HBase cluster.
   *
   * @param period The period in which a profile measurement is taken.
   */
  public static byte[] encodeSalt(ProfilePeriod period, int saltDivisor) {
    byte[] encodedPeriod = Bytes.toBytes(period.getPeriod());
    int salt = Hash.getInstance(Hash.MURMUR_HASH).hash(encodedPeriod, MURMUR_HASH_SEED) % saltDivisor;
    return Bytes.toBytes(salt);
  }

  @Override
  public void configure(Map<String, Object> properties) {

    // configure the period - uses default if not defined
    long duration = PROFILER_PERIOD.get(properties, Long.class);
    String units = PROFILER_PERIOD_UNITS.get(properties, String.class);
    setPeriodDuration(duration, TimeUnit.valueOf(units));

    // configure the salt divisor - uses default if not defined
    int saltDivisor = PROFILER_SALT_DIVISOR.get(properties, Integer.class);
    setSaltDivisor(saltDivisor);
  }

  public void setPeriodDuration(long duration, TimeUnit units) {
    periodDurationMillis = units.toMillis(duration);
  }

  public void setSaltDivisor(int saltDivisor) {
    this.saltDivisor = saltDivisor;
  }
}
