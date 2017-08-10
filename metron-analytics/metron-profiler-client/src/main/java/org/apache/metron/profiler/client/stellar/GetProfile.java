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

import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.ProfilerConfig.PROFILER_SALT_DIVISOR;
import static org.apache.metron.profiler.client.stellar.Util.getArg;
import static org.apache.metron.profiler.client.stellar.Util.getEffectiveConfig;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.client.HBaseProfilerClient;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
          "periods - The list of profile periods to grab.  These are ProfilePeriod objects.",
          "groups_list - Optional, must correspond to the 'groupBy' list used in profile creation - List (in square brackets) of "+
                  "groupBy values used to filter the profile. Default is the " +
                  "empty list, meaning groupBy was not used when creating the profile.",
          "config_overrides - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                  "of the same name. Default is the empty Map, meaning no overrides."
        },
        returns="The selected profile measurements."
)
public class GetProfile implements StellarFunction {



  /**
   * Cached client that can retrieve profile values.
   */
  private ProfilerClient client;

  /**
   * Cached value of config map actually used to construct the previously cached client.
   */
  private Map<String, Object> cachedConfigMap = new HashMap<String, Object>(6);

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Initialization.  No longer need to do anything in initialization,
   * as all setup is done lazily and cached.
   */
  @Override
  public void initialize(Context context) {
  }

  /**
   * Is the function initialized?
   */
  @Override
  public boolean isInitialized() {
    return true;
  }

  /**
   * Apply the function.
   * @param args The function arguments.
   * @param context
   */
  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {

    String profile = getArg(0, String.class, args);
    String entity = getArg(1, String.class, args);
    Optional<List<ProfilePeriod>> periods = Optional.ofNullable(getArg(2, List.class, args));
    //Optional arguments
    @SuppressWarnings("unchecked")
    List<Object> groups = null;
    Map configOverridesMap = null;
    if (args.size() < 4) {
      // no optional args, so default 'groups' and configOverridesMap remains null.
      groups = new ArrayList<>(0);
    }
    else if (args.get(3) instanceof List) {
      // correct extensible usage
      groups = getArg(3, List.class, args);
      if (args.size() >= 5) {
        configOverridesMap = getArg(4, Map.class, args);
        if (configOverridesMap.isEmpty()) configOverridesMap = null;
      }
    }
    else {
      // Deprecated "varargs" style usage for groups_list
      // configOverridesMap cannot be specified so it remains null.
      groups = getGroupsArg(3, args);
    }

    Map<String, Object> effectiveConfig = getEffectiveConfig(context, configOverridesMap);

    //lazily create new profiler client if needed
    if (client == null || !cachedConfigMap.equals(effectiveConfig)) {
      RowKeyBuilder rowKeyBuilder = getRowKeyBuilder(effectiveConfig);
      ColumnBuilder columnBuilder = getColumnBuilder(effectiveConfig);
      HTableInterface table = getTable(effectiveConfig);
      client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder);
      cachedConfigMap = effectiveConfig;
    }

    return client.fetch(Object.class, profile, entity, groups, periods.orElse(new ArrayList<>(0)));
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
  private List<Object> getGroupsArg(int startIndex, List<Object> args) {
    List<Object> groups = new ArrayList<>();

    for(int i=startIndex; i<args.size(); i++) {
      String group = getArg(i, String.class, args);
      groups.add(group);
    }

    return groups;
  }





  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private ColumnBuilder getColumnBuilder(Map<String, Object> global) {
    ColumnBuilder columnBuilder;

    String columnFamily = PROFILER_COLUMN_FAMILY.get(global, String.class);
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    return columnBuilder;
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private RowKeyBuilder getRowKeyBuilder(Map<String, Object> global) {

    // how long is the profile period?
    long duration = PROFILER_PERIOD.get(global, Long.class);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD, duration);

    // which units are used to define the profile period?
    String configuredUnits = PROFILER_PERIOD_UNITS.get(global, String.class);
    TimeUnit units = TimeUnit.valueOf(configuredUnits);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD_UNITS, units);

    // what is the salt divisor?
    Integer saltDivisor = PROFILER_SALT_DIVISOR.get(global, Integer.class);
    LOG.debug("profiler client: {}={}", PROFILER_SALT_DIVISOR, saltDivisor);

    return new SaltyRowKeyBuilder(saltDivisor, duration, units);
  }

  /**
   * Create an HBase table used when accessing HBase.
   * @param global The global configuration.
   * @return
   */
  private HTableInterface getTable(Map<String, Object> global) {

    String tableName = PROFILER_HBASE_TABLE.get(global, String.class);
    TableProvider provider = getTableProvider(global);

    try {
      return provider.getTable(HBaseConfiguration.create(), tableName);

    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Unable to access table: %s", tableName), e);
    }
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
