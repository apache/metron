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

package org.apache.metron.stellar.common;

import org.apache.metron.stellar.dsl.Context;

import java.util.Map;

/**
 * Executes Stellar expressions and maintains state across multiple invocations.
 */
public interface StellarStatefulExecutor {

  /**
   * Assign a variable a specific value.
   * @param variable The variable name.
   * @param value The value to assign to the variable.
   */
  void assign(String variable, Object value);

  /**
   * Execute an expression and assign the result to a variable.  The variable is maintained
   * in the context of this executor and is available to all subsequent expressions.
   *
   * @param variable   The name of the variable to assign to.
   * @param expression The expression to execute.
   * @param state      Additional state available to the expression.  This most often represents
   *                   the values available to the expression from an individual message. The state
   *                   maps a variable name to a variable's value.
   */
  void assign(String variable, String expression, Map<String, Object> state);

  /**
   * Execute a Stellar expression and return the result.  The internal state of the executor
   * is not modified.
   *
   * @param expression The expression to execute.
   * @param state      Additional state available to the expression.  This most often represents
   *                   the values available to the expression from an individual message. The state
   *                   maps a variable name to a variable's value.
   * @param clazz      The expected type of the expression's result.
   * @param <T>        The expected type of the expression's result.
   */
  <T> T execute(String expression, Map<String, Object> state, Class<T> clazz);

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
   *
   * @param context The Stellar context.
   */
  void setContext(Context context);
}
