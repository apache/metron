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
package org.apache.metron.profiler.spark.function;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;

import java.io.Serializable;
import java.util.Map;

public class TaskUtils implements Serializable {

  /**
   * Create the execution context for running Stellar.
   */
  public static Context getContext(Map<String, String> globals) {
    Context context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> globals)
            .with(Context.Capabilities.STELLAR_CONFIG, () -> globals)
            .build();
    StellarFunctions.initialize(context);
    return context;
  }
}
