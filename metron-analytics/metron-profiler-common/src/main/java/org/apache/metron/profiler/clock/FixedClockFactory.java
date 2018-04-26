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

import org.apache.metron.common.configuration.profiler.ProfilerConfig;

/**
 * A {@link ClockFactory} that always returns a {@link FixedClock}.
 *
 * <p>A {@link FixedClock} always returns the same time and is only useful for testing.
 */
public class FixedClockFactory implements ClockFactory {

  private long timestamp;

  /**
   * @param timestamp The timestamp that all {@link Clock} objects created by this factory will report.
   */
  public FixedClockFactory(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public Clock createClock(ProfilerConfig config) {
    return new FixedClock(timestamp);
  }
}
