/*
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
 */

package org.apache.metron.profiler.client.stellar;

import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.client.HBaseProfilerClientFactory;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.profiler.client.ProfilerClientFactory;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_DEFAULT_VALUE;
import static org.apache.metron.profiler.client.stellar.Util.getArg;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * A Stellar function that can retrieve profile measurements.
 *
 *  PROFILE_VERBOSE
 *
 * Differs from PROFILE_GET by returning a map containing the profile name, entity, period id, period start,
 * period end for each profile measurement.
 *
 * Retrieve all values for 'entity1' from 'profile1' over the past 4 hours.
 *
 *   <code>PROFILE_VERBOSE('profile1', 'entity1', PROFILE_WINDOW(4, "HOURS")</code>
 *
 * Retrieve all values for 'entity1' from 'profile1' that occurred on 'weekdays' over the past month.
 *
 *   <code>PROFILE_VERBOSE('profile1', 'entity1', PROFILE_WINDOW(1, "MONTH"), ['weekdays'])</code>
 */
@Stellar(
        namespace="PROFILE",
        name="VERBOSE",
        description="Retrieves a series of measurements from a stored profile. Returns a map containing the profile " +
                "name, entity, period id, period start, period end for each profile measurement. Provides a more " +
                "verbose view of each measurement than PROFILE_GET. See also PROFILE_GET, PROFILE_FIXED, PROFILE_WINDOW.",
        params={
                "profile - The name of the profile.",
                "entity - The name of the entity.",
                "periods - The list of profile periods to fetch. Use PROFILE_WINDOW or PROFILE_FIXED.",
                "groups - Optional, The groups to retrieve. Must correspond to the 'groupBy' " +
                        "list used during profile creation. Defaults to an empty list, meaning no groups. "
        },
        returns="A map for each profile measurement containing the profile name, entity, period, and value."
)
public class VerboseProfile implements StellarFunction {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final String PROFILE_KEY = "profile";
  protected static final String ENTITY_KEY = "entity";
  protected static final String PERIOD_KEY = "period";
  protected static final String PERIOD_START_KEY = "period.start";
  protected static final String PERIOD_END_KEY = "period.end";
  protected static final String VALUE_KEY = "value";
  protected static final String GROUPS_KEY = "groups";
  private static final int PROFILE_ARG_INDEX = 0;
  private static final int ENTITY_ARG_INDEX = 1;
  private static final int PERIOD_ARG_INDEX = 2;
  private static final int GROUPS_ARG_INDEX = 3;

  public static class Builder {
    private ProfilerClientFactory profilerClientFactory = new HBaseProfilerClientFactory();

    public VerboseProfile.Builder withProfilerClientFactory(ProfilerClientFactory profilerClientFactory) {
      this.profilerClientFactory = profilerClientFactory;
      return this;
    }

    public VerboseProfile build() {
      VerboseProfile function = new VerboseProfile();
      function.profilerClientFactory = profilerClientFactory;
      return function;
    }
  }

  /**
   * Use the {@link VerboseProfile.Builder} instead.
   */
  public VerboseProfile() {
    // constructor must be public to allow for stellar function resolution
  }

  /**
   * Allows the function to retrieve persisted {@link ProfileMeasurement} values.
   */
  private ProfilerClient profilerClient;

  /**
   * Creates the {@link ProfilerClient} used by this function.
   */
  private ProfilerClientFactory profilerClientFactory;

  @Override
  public void initialize(Context context) {
    // values stored in the global config that are used to initialize the ProfilerClient
    // are read only once during initialization.  if those values change during a Stellar
    // session, this function will not respond to them.  the Stellar session would need to be
    // restarted for those changes to take effect.  this differs from the behavior of `PROFILE_GET`.
    Map<String, Object> globals = getGlobals(context);
    profilerClient = profilerClientFactory.create(globals);
  }

  @Override
  public boolean isInitialized() {
    return profilerClient != null;
  }

  @Override
  public void close() throws IOException {
    if(profilerClient != null) {
      profilerClient.close();
    }
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    // required arguments
    String profile = getArg(PROFILE_ARG_INDEX, String.class, args);
    String entity = getArg(ENTITY_ARG_INDEX, String.class, args);
    List<ProfilePeriod> periods = getArg(PERIOD_ARG_INDEX, List.class, args);

    // optional 'groups' argument
    List<Object> groups = new ArrayList<>();
    if(args.size() > GROUPS_ARG_INDEX) {
      groups = getArg(GROUPS_ARG_INDEX, List.class, args);
    }

    // is there a default value?
    Optional<Object> defaultValue = Optional.empty();
    Map<String, Object> globals = getGlobals(context);
    if(globals != null) {
      defaultValue = Optional.ofNullable(PROFILER_DEFAULT_VALUE.get(globals));
    }

    // render a view of each profile measurement
    List<ProfileMeasurement> measurements = profilerClient.fetch(Object.class, profile, entity, groups, periods, defaultValue);
    List<Object> results = new ArrayList<>();
    for(ProfileMeasurement measurement: measurements) {
      results.add(render(measurement));
    }

    return results;
  }

  private static Map<String, Object> getGlobals(Context context) {
    return (Map<String, Object>) context.getCapability(GLOBAL_CONFIG)
            .orElse(Collections.emptyMap());
  }

  /**
   * Renders a view of the profile measurement.
   * @param measurement The profile measurement to render.
   */
  private Map<String, Object> render(ProfileMeasurement measurement) {
    Map<String, Object> view = new HashMap<>();
    view.put(PROFILE_KEY, measurement.getProfileName());
    view.put(ENTITY_KEY, measurement.getEntity());
    view.put(PERIOD_KEY, measurement.getPeriod().getPeriod());
    view.put(PERIOD_START_KEY, measurement.getPeriod().getStartTimeMillis());
    view.put(PERIOD_END_KEY, measurement.getPeriod().getEndTimeMillis());
    view.put(VALUE_KEY, measurement.getProfileValue());
    view.put(GROUPS_KEY, measurement.getGroups());
    return view;
  }
}
