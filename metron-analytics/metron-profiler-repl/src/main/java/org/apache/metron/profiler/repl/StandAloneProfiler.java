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

package org.apache.metron.profiler.repl;

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.DefaultMessageDistributor;
import org.apache.metron.profiler.DefaultMessageRouter;
import org.apache.metron.profiler.MessageDistributor;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.MessageRouter;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.clock.ClockFactory;
import org.apache.metron.profiler.clock.DefaultClockFactory;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * A stand alone version of the Profiler that does not require a distributed
 * execution environment like Apache Storm.
 *
 * <p>This class is used to create and manage profiles within the REPL environment.
 */
public class StandAloneProfiler {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The Stellar execution context.
   */
  private Context context;

  /**
   * The configuration for the Profiler.
   */
  private ProfilerConfig config;

  /**
   * The message router.
   */
  private MessageRouter router;

  /**
   * The message distributor.
   */
  private MessageDistributor distributor;

  /**
   * The factory that creates Clock objects.
   */
  private ClockFactory clockFactory;

  /**
   * Counts the number of messages that have been applied.
   */
  private int messageCount;

  /**
   * Counts the number of routes.
   *
   * If a message is not needed by any profiles, then there are 0 routes.
   * If a message is needed by 1 profile then there is 1 route.
   * If a message is needed by 2 profiles then there are 2 routes.
   */
  private int routeCount;

  /**
   * Create a new Profiler.
   *
   * @param config The Profiler configuration.
   * @param periodDurationMillis The period duration in milliseconds.
   * @param profileTimeToLiveMillis The time-to-live of a profile in milliseconds.
   * @param maxNumberOfRoutes The max number of unique routes to maintain.  After this is exceeded, lesser
   *                          used routes will be evicted from the internal cache.
   * @param context The Stellar execution context.
   */
  public StandAloneProfiler(ProfilerConfig config,
                            long periodDurationMillis,
                            long profileTimeToLiveMillis,
                            long maxNumberOfRoutes,
                            Context context) {
    this.context = context;
    this.config = config;
    this.router = new DefaultMessageRouter(context);
    this.distributor = new DefaultMessageDistributor(periodDurationMillis, profileTimeToLiveMillis, maxNumberOfRoutes);
    this.clockFactory = new DefaultClockFactory();
    this.messageCount = 0;
    this.routeCount = 0;
  }

  /**
   * Apply a message to a set of profiles.
   * @param message The message to apply.
   */
  public void apply(JSONObject message) {
    // route the message to the correct profile builders
    List<MessageRoute> routes = router.route(message, config, context);
    for (MessageRoute route : routes) {
      distributor.distribute(route, context);
    }

    routeCount += routes.size();
    messageCount += 1;
  }

  /**
   * Flush the set of profiles.
   * @return A ProfileMeasurement for each (Profile, Entity) pair.
   */
  public List<ProfileMeasurement> flush() {
    return distributor.flush();
  }

  /**
   * Returns the Profiler configuration.
   * @return The Profiler configuration.
   */
  public ProfilerConfig getConfig() {
    return config;
  }

  /**
   * Returns the number of defined profiles.
   * @return The number of defined profiles.
   */
  public int getProfileCount() {
    return (config == null) ? 0: config.getProfiles().size();
  }

  /**
   * Returns the number of messages that have been applied.
   * @return The number of messages that have been applied.
   */
  public int getMessageCount() {
    return messageCount;
  }

  /**
   * Returns the number of routes.
   * @return The number of routes.
   * @see MessageRoute
   */
  public int getRouteCount() {
    return routeCount;
  }

  @Override
  public String toString() {
    return "Profiler{" +
            getProfileCount() + " profile(s), " +
            getMessageCount() + " messages(s), " +
            getRouteCount() + " route(s)" +
            '}';
  }
}
