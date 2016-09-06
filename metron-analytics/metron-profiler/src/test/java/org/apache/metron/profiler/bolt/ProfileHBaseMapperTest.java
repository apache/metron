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

import backtype.storm.tuple.Tuple;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
/**
 * Tests the ProfileHBaseMapper class.
 */
public class ProfileHBaseMapperTest {

  Tuple tuple;
  ProfileHBaseMapper mapper;
  ProfileMeasurement measurement;
  DefaultStellarExecutor executor;

  @Before
  public void setup() {
    executor = new DefaultStellarExecutor();

    mapper = new ProfileHBaseMapper();
    mapper.setExecutor(executor);
    mapper.setSaltDivisor(0);

    measurement = new ProfileMeasurement();
    measurement.setProfileName("profile");
    measurement.setEntity("entity");
    measurement.setValue(22);
    measurement.setStart(20000);
    measurement.setEnd(50000);

    // the tuple will contain the original message
    tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq("measurement"))).thenReturn(measurement);
  }

  /**
   * There is a single group in the 'groupBy' expression that simply returns the string "group1".
   */
  @Test
  public void testRowKeyWithOneGroupBy() throws Exception {
    // setup
    measurement.setGroupBy(Arrays.asList("'group1'"));

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .put("group1".getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * The user can define multiple 'groupBy' expressions.
   */
  @Test
  public void testRowKeyWithTwoGroupBy() throws Exception {
    // setup
    measurement.setGroupBy(Arrays.asList("'group1'", "'group2'"));

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .put("group1".getBytes())
            .put("group2".getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * A 'groupBy' expression can return any type; not just Strings.
   */
  @Test
  public void testRowKeyWithOneIntegerGroupBy() throws Exception {
    // setup
    measurement.setGroupBy(Arrays.asList("200"));

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .put(Integer.valueOf(200).toString().getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * A user does not have to define a 'groupBy'.  It is an optional field.
   */
  @Test
  public void testRowKeyWithNoGroupBy() throws Exception {
    // setup
    measurement.setGroupBy(Collections.emptyList());

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * A user does not have to define a 'groupBy'.  It is an optional field.
   */
  @Test
  public void testRowKeyWithNullGroupBy() throws Exception {
    // setup
    measurement.setGroupBy(null);

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * A 'groupBy' expression can refer to the fields within the ProfileMeasurement.  The
   * most important fields likely being the starting and ending timestamp.  With these fields
   * any calendar based group can be defined.  For example, the day of week, week of month, etc
   * can all be calculated from these vaulues.
   */
  @Test
  public void testRowKeyWithGroupByUsingMeasurementField() throws Exception {

    // setup - the group expression refers to the 'end' timestamp contained within the ProfileMeasurement
    measurement.setGroupBy(Arrays.asList("end"));

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .put(Long.valueOf(measurement.getEnd()).toString().getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }

  /**
   * If the saltDivisor > 0, then the row key should be prepended with a salt.  This
   * can be used to prevent hotspotting.
   */
  @Test
  public void testRowKeyWithSalt() throws Exception {
    // setup
    mapper.setSaltDivisor(100);
    measurement.setGroupBy(Collections.emptyList());

    // the expected row key
    ByteBuffer buffer = ByteBuffer
            .allocate(100)
            .put(ProfileHBaseMapper.getSalt(measurement.getStart(), mapper.getSaltDivisor()))
            .put(measurement.getProfileName().getBytes())
            .put(measurement.getEntity().getBytes())
            .putLong(measurement.getStart());
    buffer.flip();
    final byte[] expected = new byte[buffer.limit()];
    buffer.get(expected, 0, buffer.limit());

    // validate
    byte[] actual = mapper.rowKey(tuple);
    Assert.assertTrue(Arrays.equals(expected, actual));
  }
}
