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
package org.apache.metron.profiler.clock;

import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventTimeClockTest {

  private final String timestampField = "timestamp";

  public JSONObject createMessage() {
    return new JSONObject();
  }

  /**
   * The event time should be extracted from a field contained within a message.
   */
  @Test
  public void testEventTime() {

    JSONObject message = createMessage();

    // add a field containing a timestamp to the message
    final Long timestamp = System.currentTimeMillis();
    message.put(timestampField, timestamp);

    // what time is it?
    EventTimeClock clock = new EventTimeClock(timestampField);
    Optional<Long> result = clock.currentTimeMillis(message);

    // validate
    assertTrue(result.isPresent());
    assertEquals(timestamp, result.get());
  }

  /**
   * If the timestamp field is a String, it should be converted to Long and used as-is.
   */
  @Test
  public void testEventTimeWithString() {
    JSONObject message = createMessage();

    // the timestamp field is a string
    final Long timestamp = System.currentTimeMillis();
    message.put(timestampField, timestamp.toString());

    // what time is it?
    EventTimeClock clock = new EventTimeClock(timestampField);
    Optional<Long> result = clock.currentTimeMillis(message);

    // validate
    assertTrue(result.isPresent());
    assertEquals(timestamp, result.get());
  }

  /**
   * If the message does not contain the timestamp field, then nothing should be returned.
   */
  @Test
  public void testMissingTimestampField() {

    // no timestamp added to the message
    JSONObject message = createMessage();

    // what time is it?
    EventTimeClock clock = new EventTimeClock(timestampField);
    Optional<Long> result = clock.currentTimeMillis(message);

    // validate
    assertFalse(result.isPresent());
  }

  /**
   * No timestamp should be returned if the value stored in the timestamp field
   * cannot be coerced into a valid timestamp.
   */
  @Test
  public void testInvalidValue() {

    // create a message with an invalid value stored in the timestamp field
    JSONObject message = createMessage();
    message.put(timestampField, "invalid-timestamp-value");

    // what time is it?
    EventTimeClock clock = new EventTimeClock(timestampField);
    Optional<Long> result = clock.currentTimeMillis(message);

    // no value should be returned
    assertFalse(result.isPresent());
  }
}
