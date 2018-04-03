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
 * A {@link Clock} that advances based on system time.
 *
 * <p>This {@link Clock} is used to advance time when the Profiler is running
 * on processing time, rather than event time.
 */
public class WallClock implements Clock {

  @Override
  public Optional<Long> currentTimeMillis(JSONObject message) {

    // the message does not matter; use system time
    return Optional.of(System.currentTimeMillis());
  }
}
