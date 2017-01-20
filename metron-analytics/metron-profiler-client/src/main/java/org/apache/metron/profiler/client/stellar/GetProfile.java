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

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.profiler.client.HBaseProfilerClient;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.apache.metron.common.dsl.Context.Capabilities.GLOBAL_CONFIG;

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
          "durationAgo - How long ago should values be retrieved from?",
          "units - The units of 'durationAgo'.",
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
   * A global property that defines the name of the HBase table used to store profile data.
   */
  public static final String PROFILER_HBASE_TABLE = "profiler.client.hbase.table";

  /**
   * A global property that defines the name of the column family used to store profile data.
   */
  public static final String PROFILER_COLUMN_FAMILY = "profiler.client.hbase.column.family";

  /**
   * A global property that defines the name of the HBaseTableProvider implementation class.
   */
  public static final String PROFILER_HBASE_TABLE_PROVIDER = "hbase.provider.impl";

  /**
   * A global property that defines the duration of each profile period.  This value
   * should be defined along with 'profiler.client.period.duration.units'.
   */
  public static final String PROFILER_PERIOD = "profiler.client.period.duration";

  /**
   * A global property that defines the units of the profile period duration.  This value
   * should be defined along with 'profiler.client.period.duration'.
   */
  public static final String PROFILER_PERIOD_UNITS = "profiler.client.period.duration.units";

  /**
   * A global property that defines the salt divisor used to store profile data.
   */
  public static final String PROFILER_SALT_DIVISOR = "profiler.client.salt.divisor";

  /**
   * The default Profile HBase table name should none be defined in the global properties.
   */
  public static final String PROFILER_HBASE_TABLE_DEFAULT = "profiler";

  /**
   * The default Profile column family name should none be defined in the global properties.
   */
  public static final String PROFILER_COLUMN_FAMILY_DEFAULT = "P";

  /**
   * The default Profile period duration should none be defined in the global properties.
   */
  public static final String PROFILER_PERIOD_DEFAULT = "15";

  /**
   * The default units of the Profile period should none be defined in the global properties.
   */
  public static final String PROFILER_PERIOD_UNITS_DEFAULT = "MINUTES";

  /**
   * The default salt divisor should none be defined in the global properties.
   */
  public static final String PROFILER_SALT_DIVISOR_DEFAULT = "1000";

  /**
   * Cached client that can retrieve profile values.
   */
  private ProfilerClient client;

  /**
   * Cached value of config map actually used to construct the previously cached client.
   */
  private Map<String, Object> cachedConfigMap = new HashMap<String, Object>(6);

  private static final Logger LOG = LoggerFactory.getLogger(GetProfile.class);

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
    long durationAgo = getArg(2, Long.class, args);
    String unitsName = getArg(3, String.class, args);
    TimeUnit units = TimeUnit.valueOf(unitsName);
    //Optional arguments
    @SuppressWarnings("unchecked")
    List<Object> groups = null;
    Map configOverridesMap = null;
    if (args.size() < 5) {
      // no optional args, so default 'groups' and configOverridesMap remains null.
      groups = new ArrayList<>(0);
    }
    else if (args.get(4) instanceof List) {
      // correct extensible usage
      groups = getArg(4, List.class, args);
      if (args.size() >= 6) {
        configOverridesMap = getArg(5, Map.class, args);
        if (configOverridesMap.isEmpty()) configOverridesMap = null;
      }
    }
    else {
      // Deprecated "varargs" style usage for groups_list
      // configOverridesMap cannot be specified so it remains null.
      groups = getGroupsArg(4, args);
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

    return client.fetch(Object.class, profile, entity, groups, durationAgo, units);
  }

  /**
   * Merge the configuration parameter override Map into the config from global context,
   * and return the result.  This has to be done on each call, because either may have changed.
   *
   * Only the six recognized profiler client config parameters may be set,
   * all other key-value pairs in either Map will be ignored.
   *
   * Type violations cause a Stellar ParseException.
   *
   * @param context - from which we get the global config Map.
   * @param configOverridesMap - Map of overrides as described above.
   * @return effective config Map with overrides applied.
   * @throws ParseException - if any override values are of wrong type.
   */
  private Map<String, Object> getEffectiveConfig(
              Context context
              , Map configOverridesMap
  ) throws ParseException {

    final String[] KEYLIST = {
            PROFILER_HBASE_TABLE, PROFILER_COLUMN_FAMILY,
            PROFILER_HBASE_TABLE_PROVIDER, PROFILER_PERIOD,
            PROFILER_PERIOD_UNITS, PROFILER_SALT_DIVISOR};

    // ensure the required capabilities are defined
    final Context.Capabilities[] required = { GLOBAL_CONFIG };
    validateCapabilities(context, required);
    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG).get();

    Map<String, Object> result = new HashMap<String, Object>(6);
    Object v;

    // extract the relevant parameters from global
    for (String k : KEYLIST) {
      v = global.get(k);
      if (v != null) result.put(k, v);
    }
    if (configOverridesMap == null) return result;

    // extract override values, typechecking as we go
    try {
      for (Object key : configOverridesMap.keySet()) {
        if (!(key instanceof String)) {
          // Probably unintended user error, so throw an exception rather than ignore
          throw new ParseException("Non-string key in config_overrides map is not allowed: " + key.toString());
        }
        switch ((String) key) {
          case PROFILER_HBASE_TABLE:
          case PROFILER_COLUMN_FAMILY:
          case PROFILER_HBASE_TABLE_PROVIDER:
          case PROFILER_PERIOD_UNITS:
            v = configOverridesMap.get(key);
            v = ConversionUtils.convert(v, String.class);
            result.put((String) key, v);
            break;
          case PROFILER_PERIOD:
          case PROFILER_SALT_DIVISOR:
            // be tolerant if the user put a number instead of a string
            // regardless, validate that it is an integer value
            v = configOverridesMap.get(key);
            long vlong = ConversionUtils.convert(v, Long.class);
            result.put((String) key, String.valueOf(vlong));
            break;
          default:
            LOG.warn("Ignoring unallowed key {} in config_overrides map.", key);
            break;
        }
      }
    } catch (ClassCastException | NumberFormatException cce) {
      throw new ParseException("Type violation in config_overrides map values: ", cce);
    }
    return result;
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
   * Ensure that the required capabilities are defined.
   * @param context The context to validate.
   * @param required The required capabilities.
   * @throws IllegalStateException if all of the required capabilities are not present in the Context.
   */
  private void validateCapabilities(Context context, Context.Capabilities[] required) throws IllegalStateException {

    // collect the name of each missing capability
    String missing = Stream
            .of(required)
            .filter(c -> !context.getCapability(c).isPresent())
            .map(c -> c.toString())
            .collect(Collectors.joining(", "));

    if(StringUtils.isNotBlank(missing) || context == null) {
      throw new IllegalStateException("missing required context: " + missing);
    }
  }

  /**
   * Get an argument from a list of arguments.
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  private <T> T getArg(int index, Class<T> clazz, List<Object> args) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private ColumnBuilder getColumnBuilder(Map<String, Object> global) {
    ColumnBuilder columnBuilder;

    String columnFamily = (String) global.getOrDefault(PROFILER_COLUMN_FAMILY, PROFILER_COLUMN_FAMILY_DEFAULT);
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    return columnBuilder;
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private RowKeyBuilder getRowKeyBuilder(Map<String, Object> global) {

    // how long is the profile period?
    String configuredDuration = (String) global.getOrDefault(PROFILER_PERIOD, PROFILER_PERIOD_DEFAULT);
    long duration = Long.parseLong(configuredDuration);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD, duration);

    // which units are used to define the profile period?
    String configuredUnits = (String) global.getOrDefault(PROFILER_PERIOD_UNITS, PROFILER_PERIOD_UNITS_DEFAULT);
    TimeUnit units = TimeUnit.valueOf(configuredUnits);
    LOG.debug("profiler client: {}={}", PROFILER_PERIOD_UNITS, units);

    // what is the salt divisor?
    String configuredSaltDivisor = (String) global.getOrDefault(PROFILER_SALT_DIVISOR, PROFILER_SALT_DIVISOR_DEFAULT);
    int saltDivisor = Integer.parseInt(configuredSaltDivisor);
    LOG.debug("profiler client: {}={}", PROFILER_SALT_DIVISOR, saltDivisor);

    return new SaltyRowKeyBuilder(saltDivisor, duration, units);
  }

  /**
   * Create an HBase table used when accessing HBase.
   * @param global The global configuration.
   * @return
   */
  private HTableInterface getTable(Map<String, Object> global) {

    String tableName = (String) global.getOrDefault(PROFILER_HBASE_TABLE, PROFILER_HBASE_TABLE_DEFAULT);
    TableProvider provider = getTableProvider(global);

    try {
      return provider.getTable(HBaseConfiguration.create(), tableName);

    } catch (IOException e) {
      throw new IllegalArgumentException(String.format("Unable to access table: %s", tableName));
    }
  }

  /**
   * Create the TableProvider to use when accessing HBase.
   * @param global The global configuration.
   */
  private TableProvider getTableProvider(Map<String, Object> global) {
    String clazzName = (String) global.getOrDefault(PROFILER_HBASE_TABLE_PROVIDER, HTableProvider.class.getName());

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
