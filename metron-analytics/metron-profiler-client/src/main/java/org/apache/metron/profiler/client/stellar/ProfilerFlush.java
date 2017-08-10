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
import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

import java.util.List;

@Stellar(
        namespace="PROFILER",
        name="FLUSH",
        description="Flush a local profile runner.",
        params={
                "profiler", "A local profile runner returned by PROFILER_INIT."
        },
        returns="The profile values produced by the local profile runner."
)
public class ProfilerFlush extends BaseStellarFunction {

  @Override
  public Object apply(List<Object> args) {

    // user must provide the stand-alone profiler
    StandAloneProfiler profiler = Util.getArg(0, StandAloneProfiler.class, args);
    List<ProfileMeasurement> measurements = profiler.flush();

    // return the 'value' from each profile measurement
    return measurements.stream().map(m -> m.getProfileValue());
  }
}
