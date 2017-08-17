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

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Routes incoming telemetry messages.
 *
 * A single telemetry message may need to take multiple routes.  This is the case
 * when a message is needed by more than one profile.
 */
public class DefaultMessageRouter implements MessageRouter {

  /**
   * Executes Stellar code.
   */
  private StellarStatefulExecutor executor;

  public DefaultMessageRouter(Context context) {
    this.executor = new DefaultStellarStatefulExecutor();
    StellarFunctions.initialize(context);
    executor.setContext(context);
  }

  /**
   * Route a telemetry message.  Finds all routes for a given telemetry message.
   *
   * @param message The telemetry message that needs routed.
   * @param config The configuration for the Profiler.
   * @param context The Stellar execution context.
   * @return A list of all the routes for the message.
   */
  @Override
  public List<MessageRoute> route(JSONObject message, ProfilerConfig config, Context context) {
    List<MessageRoute> routes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    final Map<String, Object> state = (Map<String, Object>) message;

    // attempt to route the message to each of the profiles
    for (ProfileConfig profile: config.getProfiles()) {

      // is this message needed by this profile?
      if (executor.execute(profile.getOnlyif(), state, Boolean.class)) {

        // what is the name of the entity in this message?
        String entity = executor.execute(profile.getForeach(), state, String.class);
        routes.add(new MessageRoute(profile, entity));
      }
    }

    return routes;
  }
}
