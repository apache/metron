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
import static org.apache.metron.profiler.client.stellar.Util.getEffectiveConfig;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * A Stellar function that can retrieve data contained within a Profile.
 *
 *  PROFILE_GET
 *
 * Retrieve all values for 'entity1' from 'profile1' over the past 4 hours.
 *
 *   <code>PROFILE_GET('profile1', 'entity1', 4, 'HOURS')</code>
 *
 * Retrieve all values for 'entity1' from 'profile1' over the past 2 days.
 *
 *   <code>PROFILE_GET('profile1', 'entity1', 2, 'DAYS')</code>
 *
 * Retrieve all values for 'entity1' from 'profile1' that occurred on 'weekdays' over the past month.
 *
 *   <code>PROFILE_GET('profile1', 'entity1', 1, 'MONTHS', ['weekdays'])</code>
 *
 * Retrieve all values for 'entity1' from 'profile1' over the past 2 days, with no 'groupBy',
 * and overriding the usual global client configuration parameters for window duration.
 *
 *   <code>PROFILE_GET('profile1', 'entity1', 2, 'DAYS', [], {'profiler.client.period.duration' : '2', 'profiler.client.period.duration.units' : 'MINUTES'})</code>
 *
 * Retrieve all values for 'entity1' from 'profile1' that occurred on 'weekdays' over the past month,
 * overriding the usual global client configuration parameters for window duration.
 *
 *   <code>PROFILE_GET('profile1', 'entity1', 1, 'MONTHS', ['weekdays'], {'profiler.client.period.duration' : '2', 'profiler.client.period.duration.units' : 'MINUTES'})</code>
 *
 */
@Stellar(
        namespace="PROFILE",
        name="GET",
        description="Retrieves a series of values from a stored profile.",
        params={
          "profile - The name of the profile.",
          "entity - The name of the entity.",
          "periods - The list of profile periods to fetch. Use PROFILE_WINDOW or PROFILE_FIXED.",
          "groups - Optional - The groups to retrieve. Must correspond to the 'groupBy' " +
                    "list used during profile creation. Defaults to an empty list, meaning no groups.",
          "config_overrides - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                  "of the same name. Default is the empty Map, meaning no overrides."
        },
        returns="The selected profile measurements."
)
public class GetProfile implements StellarFunction {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Allows the function to retrieve persisted {@link ProfileMeasurement} values.
   */
  private ProfilerClient profilerClient;

  /**
   * Creates the {@link ProfilerClient} used by this function.
   */
  private ProfilerClientFactory profilerClientFactory;

  /**
   * Last known global configuration used to create the {@link ProfilerClient}. If the
   * global configuration changes, a new {@link ProfilerClient} needs to be constructed.
   */
  private Map<String, Object> lastKnownGlobals = new HashMap<>();

  public GetProfile() {
    profilerClientFactory = new HBaseProfilerClientFactory();
  }

  @Override
  public void initialize(Context context) {
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
    String profile = getArg(0, String.class, args);
    String entity = getArg(1, String.class, args);
    List<ProfilePeriod> periods = getArg(2, List.class, args);

    // optional arguments
    List<Object> groups = getGroups(args);
    Map<String, Object> overrides = getOverrides(args);

    // lazily create new profiler client if needed
    Map<String, Object> effectiveConfig = getEffectiveConfig(context, overrides);
    if (profilerClient == null || !lastKnownGlobals.equals(effectiveConfig)) {
      profilerClient = profilerClientFactory.create(effectiveConfig);
      lastKnownGlobals = effectiveConfig;
    }

    // is there a default value?
    Optional<Object> defaultValue = Optional.empty();
    if(effectiveConfig != null) {
      defaultValue = Optional.ofNullable(PROFILER_DEFAULT_VALUE.get(effectiveConfig));
    }

    // return only the value of each profile measurement
    List<ProfileMeasurement> measurements = profilerClient.fetch(Object.class, profile, entity, groups, periods, defaultValue);
    List<Object> values = new ArrayList<>();
    for(ProfileMeasurement m: measurements) {
      values.add(m.getProfileValue());
    }

    return values;
  }

  private Map<String, Object> getOverrides(List<Object> args) {
    Map<String, Object> configOverridesMap = null;
    if(args.size() >= 5 && args.get(3) instanceof List) {
      configOverridesMap = getArg(4, Map.class, args);
      if (configOverridesMap.isEmpty()) {
        configOverridesMap = null;
      }
    }
    return configOverridesMap;
  }

  private List<Object> getGroups(List<Object> args) {
    List<Object> groups;
    if (args.size() < 4) {
      // no optional args, so default 'groups' and configOverridesMap remains null.
      groups = new ArrayList<>(0);

    } else if (args.get(3) instanceof List) {
      // correct extensible usage
      groups = getArg(3, List.class, args);

    } else {
      // deprecated "varargs" style usage for groups_list
      groups = getVarArgGroups(3, args);
    }
    return groups;
  }

  /**
   * Get the groups defined by the user.
   *
   * The user can specify 0 or more groups.  All arguments from the specified position
   * on are assumed to be groups.  If there is no argument in the specified position,
   * then it is assumed the user did not specify any groups.
   *
   * @param startIndex The starting index of groups within the function argument list.
   * @param args The function arguments.
   * @return The groups.
   */
  private static List<Object> getVarArgGroups(int startIndex, List<Object> args) {
    List<Object> groups = new ArrayList<>();

    for(int i=startIndex; i<args.size(); i++) {
      String group = getArg(i, String.class, args);
      groups.add(group);
    }

    return groups;
  }

  private static Map<String, Object> getGlobals(Context context) {
    return (Map<String, Object>) context.getCapability(GLOBAL_CONFIG)
            .orElse(Collections.emptyMap());
  }

  public GetProfile withProfilerClientFactory(ProfilerClientFactory creator) {
    this.profilerClientFactory = creator;
    return this;
  }
}
