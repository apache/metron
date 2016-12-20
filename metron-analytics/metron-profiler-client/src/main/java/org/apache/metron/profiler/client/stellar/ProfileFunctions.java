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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.apache.metron.common.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * Stellar functions that retrieve profiles created by the Profiler.
 */
public class ProfileFunctions {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileFunctions.class);

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
   *   <code>PROFILE_GET('profile1', 'entity1', 1, 'MONTHS', 'weekdays')</code>
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
                  "groups - Optional - The groups used to sort the profile."
          },
          returns="The profile measurements.")
  public static class ProfileGet implements StellarFunction {

    /**
     * The Profiler client that can retrieve profile values.
     */
    private ProfilerClient client;

    @Override
    public void initialize(Context context) {
      client = createProfilerClient(context);
    }

    @Override
    public boolean isInitialized() {
      return client != null;
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      String profile = getArg(0, args, String.class);
      String entity = getArg(1, args, String.class);
      long durationAgo = getArg(2, args, Long.class);
      String unitsName = getArg(3, args, String.class);
      TimeUnit units = TimeUnit.valueOf(unitsName);
      List<Object> groups = getGroupsArg(4, args);

      long end = System.currentTimeMillis();
      long start = end - units.toMillis(durationAgo);
      return client.fetch(Object.class, profile, entity, groups, start, end);
    }

  }

  @Stellar(
          namespace="PROFILE",
          name="GET_FROM",
          description="Retrieves a series of values from a stored profile.",
          params={
                  "profile - The name of the profile.",
                  "entity - The name of the entity.",
                  "lookBack - How long to look back in milliseconds",
                  "offset - When to start the look back from in epoch milliseconds. Defaults to current time.",
                  "groups - Optional - The groups used to sort the profile."
          },
          returns="The profile measurements.")
  public static class ProfileGetFrom implements StellarFunction {

    /**
     * The Profiler client that can retrieve profile values.
     */
    private ProfilerClient client;

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String profile = getArg(0, args, String.class);
      String entity = getArg(1, args, String.class);
      long lookBack = getArg(2, args, Long.class);
      long offset = getArg(3, args, Long.class);
      List<Object> groups = getGroupsArg(4, args);

      long end = offset;
      long start = end - lookBack;
      return client.fetch(Object.class, profile, entity, groups, start, end);
    }

    @Override
    public void initialize(Context context) {
      client = createProfilerClient(context);
    }

    @Override
    public boolean isInitialized() {
      return client != null;
    }
  }

  /**
   * Creates a ProfilerClient.
   * @param context The execution context used to create the client.
   */
  public static ProfilerClient createProfilerClient(Context context) {

    // get the global configuration
    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG)
            .orElse(Collections.emptyMap());

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

    // create the profiler client
    return new HBaseProfilerClient(
            getTable(global),
            getRowKeyBuilder(saltDivisor),
            getColumnBuilder(global),
            units.toMillis(duration));
  }

  /**
   * Get an argument from a list of arguments.
   * @param index The index within the list of arguments.
   * @param args All of the arguments.
   * @param clazz The type expected.
   * @param <T> The type of the argument expected.
   */
  private static <T> T getArg(int index, List<Object> args, Class<T> clazz) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }

  /**
   * Returns an optional argument if if exists.  Otherwise, returns the default.
   * @param index The index within the list of arguments.
   * @param args All of the arguments.
   * @param clazz The type expected.
   * @param defaultValue The default value if the argument does not exist.
   * @param <T> The type of the argument expected.
   */
  private static <T> T getOptionalArg(int index, List<Object> args, Class<T> clazz, T defaultValue) {
    T result;

    if(index < args.size()) {
      result = ConversionUtils.convert(args.get(index), clazz);

    } else {
      result = defaultValue;
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
  private static List<Object> getGroupsArg(int startIndex, List<Object> args) {
    List<Object> groups = new ArrayList<>();

    for(int i=startIndex; i<args.size(); i++) {
      String group = getArg(i, args, String.class);
      groups.add(group);
    }

    return groups;
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param global The global configuration.
   */
  private static ColumnBuilder getColumnBuilder(Map<String, Object> global) {
    // the builder is not currently configurable - but should be made so
    ColumnBuilder columnBuilder;

    if(global.containsKey(PROFILER_COLUMN_FAMILY)) {
      String columnFamily = (String) global.get(PROFILER_COLUMN_FAMILY);
      columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    } else {
      columnBuilder = new ValueOnlyColumnBuilder();
    }

    return columnBuilder;
  }

  /**
   * Creates the ColumnBuilder to use in accessing the profile data.
   * @param saltDivisor The salt divisor used by the row key builder.
   */
  private static RowKeyBuilder getRowKeyBuilder(int saltDivisor) {
    return new SaltyRowKeyBuilder(saltDivisor);
  }

  /**
   * Create an HBase table used when accessing HBase.
   * @param global The global configuration.
   * @return
   */
  private static HTableInterface getTable(Map<String, Object> global) {

    String tableName = (String) global.getOrDefault(PROFILER_HBASE_TABLE, "profiler");
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
  private static TableProvider getTableProvider(Map<String, Object> global) {
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
