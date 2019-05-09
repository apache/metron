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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.client.window.Window;
import org.apache.metron.profiler.client.window.WindowProcessor;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Stellar(
      namespace="PROFILE",
      name="WINDOW",
      description="The profiler periods associated with a window selector statement from an optional reference timestamp.",
      params={
        "windowSelector - The statement specifying the window to select.",
        "now - Optional - The timestamp to use for now.",
        "config_overrides - Optional - Map (in curly braces) of name:value pairs, each overriding the global config parameter " +
                "of the same name. Default is the empty Map, meaning no overrides."
      },
      returns="The selected profile measurement periods.  These are ProfilePeriod objects."
)
public class WindowLookback implements StellarFunction {

  private Cache<String, Window> windowCache;

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {
    Optional<Map> configOverridesMap = Optional.empty();
    long now = System.currentTimeMillis();
    String windowSelector = Util.getArg(0, String.class, args);
    if(args.size() > 1) {
      Optional<Object> arg2 = Optional.ofNullable(args.get(1));
      Optional<Object> mapArg = args.size() > 2?Optional.ofNullable(args.get(2)):Optional.empty();
      if(!mapArg.isPresent() && arg2.isPresent() && arg2.get() instanceof Map) {
        mapArg = arg2;
      }

      if(arg2.isPresent() && arg2.get() instanceof Number) {
        now = ConversionUtils.convert(arg2.get(), Long.class);
      }

      if(mapArg.isPresent()) {
        Map rawMap = ConversionUtils.convert(mapArg.get(), Map.class);
        configOverridesMap = rawMap == null || rawMap.isEmpty() ? Optional.empty() : Optional.of(rawMap);
      }

    }
    Map<String, Object> effectiveConfigs = Util.getEffectiveConfig(context, configOverridesMap.orElse(null));
    Long tickDuration = ProfilerClientConfig.PROFILER_PERIOD.get(effectiveConfigs, Long.class);
    TimeUnit tickUnit = TimeUnit.valueOf(ProfilerClientConfig.PROFILER_PERIOD_UNITS.get(effectiveConfigs, String.class));
    Window w = null;
    try {
      w = windowCache.get(windowSelector, (selector) -> WindowProcessor.process(selector));
    } catch (ParseException e) {
      throw new IllegalStateException("Unable to process " + windowSelector + ": " + e.getMessage(), e);
    }
    long end = w.getEndMillis(now);
    long start = w.getStartMillis(now);
    IntervalPredicate<ProfilePeriod> intervalSelector = new IntervalPredicate<>(period -> period.getStartTimeMillis()
                                                                               , w.toIntervals(now)
                                                                               , ProfilePeriod.class
                                                                               );
    return ProfilePeriod.visitPeriods(start, end, tickDuration, tickUnit, Optional.of(intervalSelector), period -> period);
  }

  @Override
  public void initialize(Context context) {
    windowCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
  }

  @Override
  public boolean isInitialized() {
    return windowCache != null;
  }
}
