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

package org.apache.metron.profiler;

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Routes incoming telemetry messages.
 *
 * A single telemetry message may need to take multiple routes.  This is the case
 * when a message is needed by more than one profile.
 */
public interface MessageRouter {

  /**
   * Route a telemetry message.  Finds all routes for a given telemetry message.
   *
   * @param message The telemetry message that needs routed.
   * @param config The configuration for the Profiler.
   * @param context The Stellar execution context.
   * @return A list of all the routes for the message.
   */
  List<MessageRoute> route(JSONObject message, ProfilerConfig config, Context context);
}
