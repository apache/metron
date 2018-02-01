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

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.profiler.ProfilePeriod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Stellar(
      namespace="PROFILE",
      name="FIXED",
      description="The profiler periods associated with a fixed lookback starting from now.",
      params={
        "durationAgo - How long ago should values be retrieved from?",
        "units - The units of 'durationAgo'.",
        "config_overrides - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                "of the same name. Default is the empty Map, meaning no overrides."
      },
      returns="The selected profile measurement periods.  These are ProfilePeriod objects."
)
public class FixedLookback implements StellarFunction {

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    Optional<Map> configOverridesMap = Optional.empty();
    long durationAgo = Util.getArg(0, Long.class, args);
    String unitsName = Util.getArg(1, String.class, args);
    TimeUnit units = TimeUnit.valueOf(unitsName);
    if(args.size() > 2) {
      Map rawMap = Util.getArg(2, Map.class, args);
      configOverridesMap = rawMap == null || rawMap.isEmpty() ? Optional.empty() : Optional.of(rawMap);
    }
    Map<String, Object> effectiveConfigs = Util.getEffectiveConfig(context, configOverridesMap.orElse(null));
    Long tickDuration = ProfilerClientConfig.PROFILER_PERIOD.get(effectiveConfigs, Long.class);
    TimeUnit tickUnit = TimeUnit.valueOf(ProfilerClientConfig.PROFILER_PERIOD_UNITS.get(effectiveConfigs, String.class));
    long end = System.currentTimeMillis();
    long start = end - units.toMillis(durationAgo);
    return ProfilePeriod.visitPeriods(start, end, tickDuration, tickUnit, Optional.empty(), period -> period);
  }

  @Override
  public void initialize(Context context) {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }
}
