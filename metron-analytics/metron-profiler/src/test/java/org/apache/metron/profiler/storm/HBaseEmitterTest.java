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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Values;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
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
 * Tests the HBaseEmitter class.
 */
public class HBaseEmitterTest {

  /**
   * {
   *   "profile": "profile-one",
   *   "foreach": "ip_src_addr",
   *   "init":   { "x": "0" },
   *   "update": { "x": "x + 1" },
   *   "result": "x"
   * }
   */
  @Multiline
  private String profileDefinition;

  private HBaseEmitter emitter;
  private ProfileConfig profile;
  private OutputCollector collector;

  @Before
  public void setup() throws Exception {
    emitter = new HBaseEmitter();
    profile = createDefinition(profileDefinition);
    collector = Mockito.mock(OutputCollector.class);
  }

  /**
   * The handler should emit a message containing the result of executing
   * the 'result/profile' expression.
   */
  @Test
  public void testEmit() throws Exception {

    // create a measurement that has triage values
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile")
            .withEntity("entity")
            .withPeriod(20000, 15, TimeUnit.MINUTES)
            .withDefinition(profile)
            .withProfileValue(22);

    // execute the test
    emitter.emit(measurement, collector);

    // the measurement should be emitted as-is
    ProfileMeasurement actual = expectMeasurement(emitter, collector);
    assertEquals(measurement, actual);
  }

  /**
   * Verifies that the emitter does emit a {@code ProfileMeasurement}.
   *
   * @return The {@code ProfileMeasurement} that was emitted
   */
  private ProfileMeasurement expectMeasurement(HBaseEmitter hbaseEmitter, OutputCollector collector) {

    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(1)).emit(eq(hbaseEmitter.getStreamId()), arg.capture());
    Values values = arg.getValue();
    assertTrue(values.get(0) instanceof ProfileMeasurement);
    return (ProfileMeasurement) values.get(0);
  }

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfileConfig createDefinition(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }
}
