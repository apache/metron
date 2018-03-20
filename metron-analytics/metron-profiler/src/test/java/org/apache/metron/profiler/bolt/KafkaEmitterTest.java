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
   *   "result": "x"
   * }
   */
  @Multiline
  private String profileDefinition;

  private KafkaEmitter handler;
  private ProfileConfig profile;
  private OutputCollector collector;

  @Before
  public void setup() throws Exception {
    handler = new KafkaEmitter();
    profile = createDefinition(profileDefinition);
    collector = Mockito.mock(OutputCollector.class);
  }

  /**
   * The handler must serialize the ProfileMeasurement into a JSONObject.
   */
  @Test
  public void testSerialization() throws Exception {

    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", "triage-value"))
            .withDefinition(profile);
    handler.emit(measurement, collector);

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(handler.getStreamId()), arg.capture());

    // expect a JSONObject
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);

    // validate the json
    JSONObject actual = (JSONObject) values.get(0);
    assertEquals(measurement.getDefinition().getProfile(), actual.get("profile"));
    assertEquals(measurement.getEntity(), actual.get("entity"));
    assertEquals(measurement.getPeriod().getPeriod(), actual.get("period"));
    assertEquals(measurement.getPeriod().getStartTimeMillis(), actual.get("period.start"));
    assertEquals(measurement.getPeriod().getEndTimeMillis(), actual.get("period.end"));
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
    assertNotNull(actual.get("timestamp"));
    assertEquals("profiler", actual.get("source.type"));
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

    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(triageValues)
            .withDefinition(profile);
    handler.emit(measurement, collector);

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(handler.getStreamId()), arg.capture());
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);

    // only the triage expression value itself should have been skipped, all others should be there
    JSONObject actual = (JSONObject) values.get(0);
    assertEquals(measurement.getDefinition().getProfile(), actual.get("profile"));
    assertEquals(measurement.getEntity(), actual.get("entity"));
    assertEquals(measurement.getPeriod().getPeriod(), actual.get("period"));
    assertEquals(measurement.getPeriod().getStartTimeMillis(), actual.get("period.start"));
    assertEquals(measurement.getPeriod().getEndTimeMillis(), actual.get("period.end"));
    assertNotNull(actual.get("timestamp"));
    assertEquals("profiler", actual.get("source.type"));

    // the invalid expression should be skipped due to invalid type
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
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", 123))
            .withDefinition(profile);
    handler.emit(measurement, collector);

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(handler.getStreamId()), arg.capture());
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);
    JSONObject actual = (JSONObject) values.get(0);

    // the triage expression is valid
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
  }

  /**
   * Values destined for Kafka can only be serialized into text, which limits the types of values
   * that can result from a triage expression.  Only primitive types and Strings are allowed.
   */
  @Test
  public void testStringIsValidType() throws Exception {
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", "value"))
            .withDefinition(profile);
    handler.emit(measurement, collector);

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(handler.getStreamId()), arg.capture());
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof JSONObject);
    JSONObject actual = (JSONObject) values.get(0);

    // the triage expression is valid
    assertEquals(measurement.getTriageValues().get("triage-key"), actual.get("triage-key"));
  }

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfileConfig createDefinition(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }
}
