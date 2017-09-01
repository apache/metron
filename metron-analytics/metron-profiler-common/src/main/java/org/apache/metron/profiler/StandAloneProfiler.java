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
import java.util.concurrent.ExecutionException;

/**
 * A stand alone version of the Profiler that does not require a
 * distributed execution environment like Apache Storm.
 */
public class StandAloneProfiler {

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

  public StandAloneProfiler(ProfilerConfig config, long periodDurationMillis, Context context) {
    this.context = context;
    this.config = config;
    this.router = new DefaultMessageRouter(context);
    // the period TTL does not matter in this context
    this.distributor = new DefaultMessageDistributor(periodDurationMillis, Long.MAX_VALUE);
    this.messageCount = 0;
    this.routeCount = 0;
  }

  /**
   * Apply a message to a set of profiles.
   * @param message The message to apply.
   * @throws ExecutionException
   */
  public void apply(JSONObject message) throws ExecutionException {

    List<MessageRoute> routes = router.route(message, config, context);
    for(MessageRoute route : routes) {
      distributor.distribute(message, route, context);
    }

    routeCount += routes.size();
    messageCount += 1;
  }

  @Override
  public String toString() {
    return "Profiler{" +
            getProfileCount() + " profile(s), " +
            getMessageCount() + " messages(s), " +
            getRouteCount() + " route(s)" +
            '}';
  }

  /**
   * Flush the set of profiles.
   * @return A ProfileMeasurement for each (Profile, Entity) pair.
   */
  public List<ProfileMeasurement> flush() {
    return distributor.flush();
  }

  public ProfilerConfig getConfig() {
    return config;
  }

  public int getProfileCount() {
    return (config == null) ? 0: config.getProfiles().size();
  }

  public int getMessageCount() {
    return messageCount;
  }

  public int getRouteCount() {
    return routeCount;
  }
}
