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

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_DEFAULT_VALUE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_SALT_DIVISOR;
import static org.apache.metron.profiler.client.stellar.Util.getArg;
import static org.apache.metron.profiler.client.stellar.Util.getPeriodDurationInMillis;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.client.HBaseProfilerClient;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  protected static String PROFILE_KEY = "profile";
  protected static String ENTITY_KEY = "entity";
  protected static String PERIOD_KEY = "period";
  protected static String PERIOD_START_KEY = "period.start";
  protected static String PERIOD_END_KEY = "period.end";
  protected static String VALUE_KEY = "value";
  protected static String GROUPS_KEY = "groups";
  private ProfilerClient client;

  @Override
  public void initialize(Context context) {
    // nothing to do
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    // required arguments
    String profile = getArg(0, String.class, args);
    String entity = getArg(1, String.class, args);
    List<ProfilePeriod> periods = getArg(2, List.class, args);

    // optional 'groups' argument
    List<Object> groups = new ArrayList<>();
    if(args.size() >= 4) {
      groups = getArg(3, List.class, args);
    }

    // get globals from the context
    Map<String, Object> globals = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG)
            .orElse(Collections.emptyMap());

    // lazily create the profiler client, if needed
    if (client == null) {
      RowKeyBuilder rowKeyBuilder = getRowKeyBuilder(globals);
      ColumnBuilder columnBuilder = getColumnBuilder(globals);
      TableProvider provider = getTableProvider(globals);
      long periodDuration = getPeriodDurationInMillis(globals);
      client = new HBaseProfilerClient(provider, rowKeyBuilder, columnBuilder, periodDuration, getTableName(globals), HBaseConfiguration.create());
    }

    // is there a default value?
    Optional<Object> defaultValue = Optional.empty();
    if(globals != null) {
      defaultValue = Optional.ofNullable(PROFILER_DEFAULT_VALUE.get(globals));
    }

    List<ProfileMeasurement> measurements = client.fetch(Object.class, profile, entity, groups, periods, defaultValue);

    // render a view of each profile measurement
    List<Object> results = new ArrayList<>();
    for(ProfileMeasurement measurement: measurements) {
      results.add(render(measurement));
    }
    return results;
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

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private ColumnBuilder getColumnBuilder(Map<String, Object> global) {
    String columnFamily = PROFILER_COLUMN_FAMILY.get(global, String.class);
    return new ValueOnlyColumnBuilder(columnFamily);
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private RowKeyBuilder getRowKeyBuilder(Map<String, Object> global) {
    Integer saltDivisor = PROFILER_SALT_DIVISOR.get(global, Integer.class);
    return new SaltyRowKeyBuilder(saltDivisor, getPeriodDurationInMillis(global), TimeUnit.MILLISECONDS);
  }

  private String getTableName(Map<String, Object> global) {
    return PROFILER_HBASE_TABLE.get(global, String.class);
  }

  /**
   * Create the TableProvider to use when accessing HBase.
   * @param global The global configuration.
   */
  private TableProvider getTableProvider(Map<String, Object> global) {
    String clazzName = PROFILER_HBASE_TABLE_PROVIDER.get(global, String.class);
    TableProvider provider;
    try {
      @SuppressWarnings("unchecked")
      Class<? extends TableProvider> clazz = (Class<? extends TableProvider>) Class.forName(clazzName);
      provider = clazz.getConstructor().newInstance();

    } catch (Exception e) {
      provider = new HTableProvider();
    }

    return provider;
  }
}
