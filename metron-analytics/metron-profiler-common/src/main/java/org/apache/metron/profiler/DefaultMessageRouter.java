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
import org.apache.metron.profiler.clock.Clock;
import org.apache.metron.profiler.clock.ClockFactory;
import org.apache.metron.profiler.clock.DefaultClockFactory;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Routes incoming telemetry messages.
 *
 * A single telemetry message may need to take multiple routes.  This is the case
 * when a message is needed by more than one profile.
 */
public class DefaultMessageRouter implements MessageRouter, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Executes Stellar code.
   */
  private StellarStatefulExecutor executor;

  /**
   * Responsible for creating the {@link Clock}.
   */
  private ClockFactory clockFactory;

  public DefaultMessageRouter(Context context) {
    this.executor = new DefaultStellarStatefulExecutor();
    StellarFunctions.initialize(context);
    executor.setContext(context);
    clockFactory = new DefaultClockFactory();
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

    // attempt to route the message to each of the profiles
    for (ProfileConfig profile: config.getProfiles()) {
      Clock clock = clockFactory.createClock(config);
      Optional<MessageRoute> route = routeToProfile(message, profile, clock);
      route.ifPresent(routes::add);
    }

    return routes;
  }

  /**
   * Creates a route if a message is needed by a profile.
   * @param message The message that needs routed.
   * @param profile The profile that may need the message.
   * @return A MessageRoute if the message is needed by the profile.
   */
  private Optional<MessageRoute> routeToProfile(JSONObject message, ProfileConfig profile, Clock clock) {
    Optional<MessageRoute> route = Optional.empty();

    // allow the profile to access the fields defined within the message
    @SuppressWarnings("unchecked")
    final Map<String, Object> state = (Map<String, Object>) message;
    try {
      // is this message needed by this profile?
      if (executor.execute(profile.getOnlyif(), state, Boolean.class)) {

        // what time is is? could be either system or event time
        Optional<Long> timestamp = clock.currentTimeMillis(message);
        if(timestamp.isPresent()) {

          // what is the name of the entity in this message?
          String entity = executor.execute(profile.getForeach(), state, String.class);
          route = Optional.of(new MessageRoute(profile, entity, message, timestamp.get()));
        }
      }

    } catch(Throwable e) {
      // log an error and move on. ignore bad profiles.
      String msg = format("error while executing profile; profile='%s', error='%s'", profile.getProfile(), e.getMessage());
      LOG.error(msg, e);
    }

    return route;
  }

  public void setExecutor(StellarStatefulExecutor executor) {
    this.executor = executor;
  }

  public void setClockFactory(ClockFactory clockFactory) {
    this.clockFactory = clockFactory;
  }
}
