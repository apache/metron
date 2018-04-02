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

import java.util.Optional;

/**
 * A {@link Clock} that always reports the same time.
 *
 * <p>This is only useful for testing.
 */
public class FixedClock implements Clock {

  /**
   * The time in milliseconds since the epoch.
   */
  private long epochMillis;

  /**
   * Create a {@link Clock}.  The time defaults to the epoch.
   */
  public FixedClock() {
    this(0);
  }

  /**
   * Create a {@link Clock}.
   * @param epochMillis The time in milliseconds since the epoch.
   */
  public FixedClock(long epochMillis) {
    this.setTime(epochMillis);
  }

  /**
   * Set the current time.
   * @param epochMillis The time in milliseconds since the epoch.
   */
  public void setTime(long epochMillis) {
    this.epochMillis = epochMillis;
  }

  /**
   * @return The time in milliseconds since the epoch.
   */
  @Override
  public Optional<Long> currentTimeMillis(JSONObject message) {
    return Optional.of(this.epochMillis);
  }
}
