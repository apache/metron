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
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.profiler.client.HBaseProfilerClient;
import org.apache.metron.profiler.client.ProfilerClient;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_COLUMN_BUILDER;
import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_HBASE_TABLE;
import static org.apache.metron.common.dsl.Context.Capabilities.PROFILER_ROW_KEY_BUILDER;

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
        returns="The profile measurements."
)
public class GetProfile implements StellarFunction {

  private ProfilerClient client;

  /**
   * Initialization.
   */
  @Override
  public void initialize(Context context) {

    Context.Capabilities[] required = { PROFILER_HBASE_TABLE, PROFILER_ROW_KEY_BUILDER, PROFILER_COLUMN_BUILDER };
    validateContext(context, required);

    HTableInterface table = (HTableInterface) context.getCapability(PROFILER_HBASE_TABLE).get();
    RowKeyBuilder rowKeyBuilder = (RowKeyBuilder) context.getCapability(PROFILER_ROW_KEY_BUILDER).get();
    ColumnBuilder columnBuilder = (ColumnBuilder) context.getCapability(PROFILER_COLUMN_BUILDER).get();

    client = new HBaseProfilerClient(table, rowKeyBuilder, columnBuilder);
  }

  /**
   * Is the function initialized?
   */
  @Override
  public boolean isInitialized() {
    return client != null;
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
    List<Object> groups = getGroupsArg(4, args);

    return client.fetch(profile, entity, durationAgo, units, Integer.class, groups);
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
   * Ensure that the context has all of the required capabilities.
   * @param context The context to validate.
   * @param required The required capabilities.
   * @throws IllegalStateException if all of the required capabilities are not present in the Context.
   */
  private void validateContext(Context context, Context.Capabilities[] required) throws IllegalStateException {

    // ensure that the required configuration is available
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
}
