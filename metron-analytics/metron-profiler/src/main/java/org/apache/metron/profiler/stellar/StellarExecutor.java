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

package org.apache.metron.profiler.stellar;

import org.apache.metron.common.dsl.Context;
import org.json.simple.JSONObject;

import java.util.Map;

/**
 * Executes Stellar expressions and maintains state across multiple invocations.
 *
 * There are two sets of functions in Stellar currently.  One can be executed with
 * a PredicateProcessor and the other a TransformationProcessor.  This interface
 * abstracts away that complication.
 */
public interface StellarExecutor {

  /**
   * Execute an expression and assign the result to a variable.
   *
   * @param variable The name of the variable to assign to.
   * @param expression The expression to execute.
   * @param message The message that provides additional context for the expression.
   */
  void assign(String variable, String expression, Map<String, Object> message);

  /**
   * Execute a Stellar expression and return the result.
   *
   * @param expression The expression to execute.
   * @param message A map of values, most often the JSON message itself, that is accessible within Stellar.
   * @param clazz The expected class of the expression's result.
   * @param <T> The expected class of the expression's result.
   */
  <T> T execute(String expression, Map<String, Object> message, Class<T> clazz);

  /**
   * The current state of the Stellar execution environment.
   */
  Map<String, Object> getState();

  /**
   * Removes all state from the execution environment.
   */
  void clearState();

  /**
   * Sets the Context for the Stellar execution environment.  This provides global data used
   * to initialize Stellar functions.
   * @param context The Stellar context.
   */
  void setContext(Context context);
}
