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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Tests the KafkaDestinationHandler.
 */
public class KafkaDestinationHandlerTest {

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

  private KafkaDestinationHandler handler;
  private ProfileConfig profile;
  private ProfileMeasurement measurement;
  private OutputCollector collector;

  @Before
  public void setup() throws Exception {
    handler = new KafkaDestinationHandler();

    profile = createDefinition(profileDefinition);

    measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withTriageValues(Collections.singletonMap("triage-key", "triage-value"))
            .withDefinition(profile);

    collector = Mockito.mock(OutputCollector.class);
  }

  /**
   * The handler must serialize the ProfileMeasurement into a JSONObject.
   */
  @Test
  public void testJSONSerialization() throws Exception {

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
    assertEquals(measurement.getPeriod().getStartTimeMillis(), actual.get("periodStartTime"));
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
