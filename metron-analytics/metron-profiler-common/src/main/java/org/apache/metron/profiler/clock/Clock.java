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
 * A {@link Clock} manages the progression of time in the Profiler.
 *
 * <p>The Profiler can operate on either processing time or event time.  This
 * abstraction deals with the differences between the two.
 */
public interface Clock {

  /**
   * Returns the current time in epoch milliseconds.
   *
   * @param message The telemetry message.
   * @return An optional value containing the current time in epoch milliseconds, if
   *         the current time is known.  Otherwise, empty.
   */
  Optional<Long> currentTimeMillis(JSONObject message);
}
