/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.profiler.clock;

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the {@link EventTimeOnlyClockFactory}.
 */
public class EventTimeOnlyClockFactoryTest {

  private EventTimeOnlyClockFactory clockFactory;

  @BeforeEach
  public void setup() {
    clockFactory = new EventTimeOnlyClockFactory();
  }

  @Test
  public void testCreateEventTimeClock() {
    // configure the profiler to use event time
    ProfilerConfig config = new ProfilerConfig();
    config.setTimestampField(Optional.of("timestamp"));

    // the factory should return a clock that handles 'event time'
    Clock clock = clockFactory.createClock(config);
    assertTrue(clock instanceof EventTimeClock);
  }

  @Test
  public void testCreateProcessingTimeClock() {
    // the profiler uses processing time by default
    ProfilerConfig config = new ProfilerConfig();
    assertThrows(IllegalStateException.class, () -> clockFactory.createClock(config));
  }
}
