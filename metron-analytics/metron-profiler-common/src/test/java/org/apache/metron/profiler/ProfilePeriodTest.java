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

import org.apache.metron.common.utils.SerDeUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ProfilePeriod class.
 */
public class ProfilePeriodTest {

  /**
   * Thu, Aug 25 2016 13:27:10 GMT
   */
  private long AUG2016 = 1472131630748L;

  @Test
  public void testFirstPeriodAtEpoch() {
    long duration = 1;
    TimeUnit units = TimeUnit.HOURS;

    ProfilePeriod period = new ProfilePeriod(0, duration, units);
    assertEquals(0, period.getPeriod());
    assertEquals(0, period.getStartTimeMillis());
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testOneMinutePeriods() {
    long duration = 1;
    TimeUnit units = TimeUnit.MINUTES;

    ProfilePeriod period = new ProfilePeriod(AUG2016, duration, units);
    assertEquals(24535527, period.getPeriod());
    assertEquals(1472131620000L, period.getStartTimeMillis());  // Thu, 25 Aug 2016 13:27:00 GMT
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testFifteenMinutePeriods() {
    long duration = 15;
    TimeUnit units = TimeUnit.MINUTES;

    ProfilePeriod period = new ProfilePeriod(AUG2016, duration, units);
    assertEquals(1635701, period.getPeriod());
    assertEquals(1472130900000L, period.getStartTimeMillis());  // Thu, 25 Aug 2016 13:15:00 GMT
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testOneHourPeriods() {
    long duration = 1;
    TimeUnit units = TimeUnit.HOURS;

    ProfilePeriod period = new ProfilePeriod(AUG2016, duration, units);
    assertEquals(408925, period.getPeriod());
    assertEquals(1472130000000L, period.getStartTimeMillis());  // Thu, 25 Aug 2016 13:00:00 GMT
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testTwoHourPeriods() {
    long duration = 2;
    TimeUnit units = TimeUnit.HOURS;

    ProfilePeriod period = new ProfilePeriod(AUG2016, duration, units);
    assertEquals(204462, period.getPeriod());
    assertEquals(1472126400000L, period.getStartTimeMillis());  //  Thu, 25 Aug 2016 12:00:00 GMT
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testEightHourPeriods() {
    long duration = 8;
    TimeUnit units = TimeUnit.HOURS;

    ProfilePeriod period = new ProfilePeriod(AUG2016, duration, units);
    assertEquals(51115, period.getPeriod());
    assertEquals(1472112000000L, period.getStartTimeMillis());  // Thu, 25 Aug 2016 08:00:00 GMT
    assertEquals(units.toMillis(duration), period.getDurationMillis());
  }

  @Test
  public void testNextWithFifteenMinutePeriods() {
    long duration = 15;
    TimeUnit units = TimeUnit.MINUTES;

    ProfilePeriod previous = new ProfilePeriod(AUG2016, duration, units);
    IntStream.range(0, 100).forEach(i -> {

      ProfilePeriod next = previous.next();
      assertEquals(previous.getPeriod() + 1, next.getPeriod());
      assertEquals(previous.getStartTimeMillis() + previous.getDurationMillis(), next.getStartTimeMillis());
      assertEquals(previous.getDurationMillis(), next.getDurationMillis());
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPeriodDurationOfZero() {
    long duration = 0;
    TimeUnit units = TimeUnit.HOURS;
    new ProfilePeriod(0, duration, units);
  }

  /**
   * Ensure that the ProfilePeriod can undergo Kryo serialization which
   * occurs when the Profiler is running in Storm.
   */
  @Test
  public void testKryoSerialization() throws Exception {

    ProfilePeriod expected = new ProfilePeriod(AUG2016, 1, TimeUnit.HOURS);

    // round-trip java serialization
    byte[] raw = SerDeUtils.toBytes(expected);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);

    assertEquals(expected, actual);
  }

  /**
   * Ensure that the ProfilePeriod can undergo Java serialization, should a user
   * prefer that over Kryo serialization, which can occur when the Profiler is running
   * in Storm.
   */
  @Test
  public void testJavaSerialization() throws Exception {

    ProfilePeriod expected = new ProfilePeriod(AUG2016, 1, TimeUnit.HOURS);

    // serialize using java
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(expected);

    // the serialized bits
    byte[] raw = bytes.toByteArray();
    assertTrue(raw.length > 0);

    // deserialize using java
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
    Object actual = in.readObject();

    // ensure that the round-trip was successful
    assertEquals(expected, actual);
  }
}
