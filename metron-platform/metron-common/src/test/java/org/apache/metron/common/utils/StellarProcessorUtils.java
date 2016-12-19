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

package org.apache.metron.common.utils;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.stellar.StellarPredicateProcessor;
import org.junit.Assert;

import java.util.Map;

public class StellarProcessorUtils {

  /**
   * Execute a Stellar expression.
   * @param expression The Stellar expression to execute.
   * @param variables The variables to expose to the expression.
   * @param context The execution context.
   * @return The result of executing the Stellar expression.
   */
  public static Object run(String expression, Map<String, Object> variables, Context context) {
    return new StellarExecutor()
            .withVariables(variables)
            .withContext(context)
            .execute(expression, Object.class);
  }

  /**
   * Execute a Stellar expression.
   * @param expression The Stellar expression to execute.
   * @param variables The variables to expose to the expression.
   * @return The result of executing the Stellar expression.
   */
  public static Object run(String expression, Map<String, Object> variables) {
    return new StellarExecutor()
            .withVariables(variables)
            .execute(expression, Object.class);
  }

  public static boolean runPredicate(String rule, Map resolver) {
    return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
  }

  public static boolean runPredicate(String rule, Map resolver, Context context) {
    return runPredicate(rule, new MapVariableResolver(resolver), context);
  }

  public static boolean runPredicate(String rule, VariableResolver resolver) {
    return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
  }

  public static boolean runPredicate(String rule, VariableResolver resolver, Context context) {
    StellarPredicateProcessor processor = new StellarPredicateProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule));
    return processor.parse(rule, resolver, StellarFunctions.FUNCTION_RESOLVER(), context);
  }
}
