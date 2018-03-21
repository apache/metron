/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.enrichment.parallel;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;

/**
 * The full context needed for an enrichment.  This is an abstraction to pass information from the underlying
 * environment (e.g. a storm bolt) to the set of storm independent enrichment infrastructure.
 */
public class EnrichmentContext {
  private FunctionResolver functionResolver;
  private Context stellarContext;

  public EnrichmentContext(FunctionResolver functionResolver, Context stellarContext) {
    this.functionResolver = functionResolver;
    this.stellarContext = stellarContext;
  }

  public FunctionResolver getFunctionResolver() {
    return functionResolver;
  }

  public Context getStellarContext() {
    return stellarContext;
  }
}
