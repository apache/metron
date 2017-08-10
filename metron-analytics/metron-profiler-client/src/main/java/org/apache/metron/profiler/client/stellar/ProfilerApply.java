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

import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Stellar(
        namespace="PROFILER",
        name="APPLY",
        description="Apply a message to a local profile runner.",
        params={
                "message", "The message to apply.",
                "profiler", "A local profile runner returned by PROFILER_INIT."
        },
        returns="The local profile runner."
)
public class ProfilerApply extends BaseStellarFunction {

  private JSONParser parser;

  @Override
  public void initialize(Context context) {
    parser = new JSONParser();
  }

  @Override
  public boolean isInitialized() {
    return parser != null;
  }

  @Override
  public Object apply(List<Object> args) {
    return apply(args, Context.EMPTY_CONTEXT());
  }

  @Override
  public Object apply(List<Object> args, Context context) throws ParseException {

    // user must provide the json telemetry message
    String arg0 = Util.getArg(0, String.class, args);
    JSONObject message;
    try {
      message = (JSONObject) parser.parse(arg0);

    } catch(org.json.simple.parser.ParseException e) {
      throw new IllegalArgumentException("invalid message", e);
    }

    // user must provide the stand alone profiler
    StandAloneProfiler profiler = Util.getArg(1, StandAloneProfiler.class, args);
    try {
      profiler.apply(message);

    } catch(ExecutionException e) {
      throw new IllegalArgumentException(e);
    }

    return profiler;
  }
}
