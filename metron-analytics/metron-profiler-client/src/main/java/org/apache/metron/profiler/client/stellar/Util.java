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

import static java.lang.String.format;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Ensure that the required capabilities are defined.
   * @param context The context to validate.
   * @param required The required capabilities.
   * @throws IllegalStateException if all of the required capabilities are not present in the Context.
   */
  public static void validateCapabilities(Context context, Context.Capabilities[] required) throws IllegalStateException {

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
  public static Map<String, Object> getEffectiveConfig(Context context , Map configOverridesMap ) throws ParseException {
    // ensure the required capabilities are defined
    final Context.Capabilities[] required = { GLOBAL_CONFIG };
    validateCapabilities(context, required);
    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG).get();

    Map<String, Object> result = new HashMap<>(6);

    // extract the relevant parameters from global, the overrides and the defaults
    for (ProfilerClientConfig k : ProfilerClientConfig.values()) {
      Object globalValue = global.containsKey(k.key)?ConversionUtils.convert(global.get(k.key), k.valueType):null;
      Object overrideValue = configOverridesMap == null?null:k.getOrDefault(configOverridesMap, null);
      Object defaultValue = k.defaultValue;
      if(overrideValue != null) {
        result.put(k.key, overrideValue);
      }
      else if(globalValue != null) {
        result.put(k.key, globalValue);
      }
      else if(defaultValue != null) {
        result.put(k.key, defaultValue);
      }
    }
    return result;
  }


  /**
   * Get an argument from a list of arguments.
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  public static <T> T getArg(int index, Class<T> clazz, List<Object> args) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("expected at least %d argument(s), found %d", index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }
}
