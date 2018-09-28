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

package org.apache.metron.profiler.storm;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.statistics.OnlineStatisticsProvider;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the KafkaDestinationHandler.
 */
public class KafkaEmitterTest {

  /**
   * {
   *   "profile": "profile-one-destination",
   *   "foreach": "ip_src_addr",
   *   "init":   { "x": "0" },
   *   "update": { "x": "x + 1" },
   *   "result": {
   *      "profile": "x",
   *      "triage": {
   *        "value": "x"
   *       }
   *    }
   * }
   */
  @Multiline
  private String profileDefinitionWithTriage;

  private KafkaEmitter kafkaEmitter;
  private ProfileConfig profile;
  private OutputCollector collector;

  @Before
  public void setup() throws Exception {
    kafkaEmitter = new KafkaEmitter();
    profile = createDefinition(profileDefinitionWithTriage);
    collector = Mockito.mock(OutputCollector.class);
  }

  /**
   * The handler should emit a message when a result/triage expression(s) has been defined.
   */
  @Test
  public void testEmit() throws Exception {

    // create a measurement that has triage values
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withDefinition(profile)
            .withTriageValues(Collections.singletonMap("triage-key", "triage-value"));

    // execute the test
    kafkaEmitter.emit(measurement, collector);

    // a message should be emitted
    verify(collector, times(1)).emit(eq(kafkaEmitter.getStreamId()), any());
  }

  /**
   * The handler should NOT emit a message when there is NO result/triage value(s).
   */
  @Test
  public void testDoNotEmit() throws Exception {

    // create a measurement with NO triage values
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withDefinition(profile);

    // execute the test
    kafkaEmitter.emit(measurement, collector);

    // a message should NOT be emitted
    verify(collector, times(0)).emit(eq(kafkaEmitter.getStreamId()), any());
  }

  /**
   * Validate that the message generated for Kafka should include the triage value.
   */
  @Test
  public void testTriageValueInMessage() throws Exception {

    // create a measurement that has triage values
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withDefinition(profile)
            .withProfileName(profile.getProfile())
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", "triage-value"));

    // execute the test
    kafkaEmitter.emit(measurement, collector);
    JSONObject actual = expectJsonObject(kafkaEmitter, collector);

    // validate the core parts of the message
    assertEquals(measurement.getProfileName(),                    actual.get("profile"));
    assertEquals(measurement.getEntity(),                         actual.get("entity"));
    assertEquals(measurement.getPeriod().getPeriod(),             actual.get("period"));
    assertEquals(measurement.getPeriod().getStartTimeMillis(),    actual.get("period.start"));
    assertEquals(measurement.getPeriod().getEndTimeMillis(),      actual.get("period.end"));
    assertEquals("profiler",                                      actual.get("source.type"));
    assertNotNull(actual.get("timestamp"));

    // validate that the triage value has been added
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
  }

  /**
   * Validate that the message generated for Kafka can include multiple triage values.
   */
  @Test
  public void testMultipleTriageValueInMessage() throws Exception {

    // multiple triage values have been defined
    Map<String, Object> triageValues = ImmutableMap.of(
            "x", 2,
            "y", "4",
            "z", 6.0);

    // create a measurement that has multiple triage values
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withDefinition(profile)
            .withProfileName(profile.getProfile())
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(triageValues);

    // execute the test
    kafkaEmitter.emit(measurement, collector);
    JSONObject actual = expectJsonObject(kafkaEmitter, collector);

    // validate that ALL of the triage values have been added
    assertEquals(measurement.getTriageValues().get("x"), actual.get("x"));
    assertEquals(measurement.getTriageValues().get("y"), actual.get("y"));
    assertEquals(measurement.getTriageValues().get("z"), actual.get("z"));
  }

  /**
   * Values destined for Kafka can only be serialized into text, which limits the types of values
   * that can result from a triage expression.  Only primitive types and Strings are allowed.
   */
  @Test
  public void testInvalidType() throws Exception {

    // create one invalid expression and one valid expression
    Map<String, Object> triageValues = ImmutableMap.of(
            "invalid", new OnlineStatisticsProvider(),
            "valid", 4);

    // create the measurement with a Map as a triage value; this is not allowed
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withDefinition(profile)
            .withProfileName(profile.getProfile())
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(triageValues);

    // execute the test
    kafkaEmitter.emit(measurement, collector);
    JSONObject actual = expectJsonObject(kafkaEmitter, collector);

    // validate the core parts of the message still exist
    assertEquals(measurement.getProfileName(),                    actual.get("profile"));
    assertEquals(measurement.getEntity(),                         actual.get("entity"));
    assertEquals(measurement.getPeriod().getPeriod(),             actual.get("period"));
    assertEquals(measurement.getPeriod().getStartTimeMillis(),    actual.get("period.start"));
    assertEquals(measurement.getPeriod().getEndTimeMillis(),      actual.get("period.end"));
    assertEquals("profiler",                                      actual.get("source.type"));

    // the invalid expression should be skipped and not included in the message
    assertFalse(actual.containsKey("invalid"));

    // but the valid expression should still be there
    assertEquals(triageValues.get("valid"), actual.get("valid"));
  }

  /**
   * Values destined for Kafka can only be serialized into text, which limits the types of values
   * that can result from a triage expression.  Only primitive types and Strings are allowed.
   */
  @Test
  public void testIntegerIsValidType() throws Exception {

    // create a measurement with a triage value that is an integer
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withDefinition(profile)
            .withProfileName(profile.getProfile())
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", 123));

    // execute the test
    kafkaEmitter.emit(measurement, collector);
    JSONObject actual = expectJsonObject(kafkaEmitter, collector);

    // the triage expression is valid
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
  }

  /**
   * Values destined for Kafka can only be serialized into text, which limits the types of values
   * that can result from a triage expression.  Only primitive types and Strings are allowed.
   */
  @Test
  public void testStringIsValidType() throws Exception {

    // create a measurement with a triage value that is a string
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withDefinition(profile)
            .withProfileName(profile.getProfile())
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", "value"));

    // execute the test
    kafkaEmitter.emit(measurement, collector);
    JSONObject actual = expectJsonObject(kafkaEmitter, collector);

    // the triage expression is valid
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
  }

  /**
   * Verifies that the KafkaEmitter does emit a JSONObject.
   * @return The JSONObject that was emitted
   */
  private JSONObject expectJsonObject(KafkaEmitter kafkaEmitter, OutputCollector collector) {

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(kafkaEmitter.getStreamId()), arg.capture());
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);
    return (JSONObject) values.get(0);
  }

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfileConfig createDefinition(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }
}
