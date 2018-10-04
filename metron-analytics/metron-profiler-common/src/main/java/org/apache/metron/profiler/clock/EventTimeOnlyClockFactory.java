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

import java.io.Serializable;

/**
 * Creates a {@link Clock} based on the profiler configuration.  This should
 * be used in cases where only event time is accceptable.
 *
 * <p>If the Profiler is configured to use event time, a {@link EventTimeClock} will
 * be created.  Otherwise, an {@link IllegalStateException} is thrown.
 */
public class EventTimeOnlyClockFactory implements ClockFactory, Serializable {

  /**
   * If the Profiler is configured to use event time, a {@link EventTimeClock} is created.
   * Otherwise, an {@link IllegalArgumentException} is thrown.
   *
   * @param config The profiler configuration.
   * @return The appropriate Clock based on the profiler configuration.
   * @throws IllegalStateException If the profiler configuration is set to system time.
   */
  @Override
  public Clock createClock(ProfilerConfig config) {
    Clock clock;

    boolean isEventTime = config.getTimestampField().isPresent();
    if(isEventTime) {
      String timestampField = config.getTimestampField().get();
      clock = new EventTimeClock(timestampField);

    } else {
      throw new IllegalStateException("Expected profiler to use event time.");
    }

    return clock;
  }
}
