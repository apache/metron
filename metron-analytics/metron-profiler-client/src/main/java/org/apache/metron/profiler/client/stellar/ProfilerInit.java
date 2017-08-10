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

import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Stellar;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.Util.getArg;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

@Stellar(
        namespace="PROFILER",
        name="INIT",
        description="Creates a local profile runner that can execute profiles.",
        params={
                "config", "The profiler configuration as a string."
        },
        returns="A local profile runner."
)
public class ProfilerInit extends BaseStellarFunction {

  @Override
  public Object apply(List<Object> args) {
    return apply(args, Context.EMPTY_CONTEXT());
  }

  @Override
  public Object apply(List<Object> args, Context context) {

    @SuppressWarnings("unchecked")
    Map<String, Object> global = (Map<String, Object>) context.getCapability(GLOBAL_CONFIG).get();

    // how long is the profile period?
    long duration = PROFILER_PERIOD.get(global, Long.class);
    String configuredUnits = PROFILER_PERIOD_UNITS.get(global, String.class);
    long periodDurationMillis = TimeUnit.valueOf(configuredUnits).toMillis(duration);

    // user must provide the configuration for the profiler
    String arg0 = getArg(0, String.class, args);
    org.apache.metron.common.configuration.profiler.ProfilerConfig profilerConfig;
    try {
       profilerConfig = JSONUtils.INSTANCE.load(arg0, ProfilerConfig.class);
    } catch(IOException e) {
      throw new IllegalArgumentException("Invalid profiler configuration", e);
    }

    return new StandAloneProfiler(profilerConfig, periodDurationMillis, context);
  }
}
