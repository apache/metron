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

import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.stellar.StellarPredicateProcessor;
import org.apache.metron.common.stellar.StellarProcessor;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of a StellarExecutor.
 */
public class DefaultStellarExecutor implements StellarExecutor, Serializable {

  /**
   * The current state of the Stellar execution environment.
   */
  private Map<String, Object> state;

  public DefaultStellarExecutor() {
    clearState();
  }

  /**
   * @param initialState Initial state loaded into the execution environment.
   */
  public DefaultStellarExecutor(Map<String, Object> initialState) {
    this.state = new HashMap<>(initialState);
  }

  @Override
  public Map<String, Object> getState() {
    return new HashMap<>(state);
  }

  /**
   * Execute an expression and assign the result to a variable.
   *
   * Stellar does not directly support assignment currently.  This makes
   * life easier until it does.
   *
   * @param variable The variable name to assign to.
   * @param expression The expression to execute.
   * @param message The message that provides additional context for the expression.
   */
  @Override
  public void assign(String variable, String expression, JSONObject message) {

    Object result = execute(expression, message);
    state.put(variable, result);
  }

  /**
   * Execute a Stellar expression and returns the result.
   *
   * @param expression The expression to execute.
   * @param message The message that is accessible when Stellar is executed.
   * @param clazz The expected class of the expression's result.
   * @param <T> The expected class of the expression's result.
   */
  @Override
  public <T> T execute(String expression, JSONObject message, Class<T> clazz) {

    Object result = execute(expression, message);
    if(clazz.isAssignableFrom(result.getClass())) {
      return (T) result;

    } else {
      throw new RuntimeException(String.format("Unexpected type; expected=%s, actual=%s, expression=%s",
              clazz.getSimpleName(), result.getClass().getSimpleName(), expression));
    }
  }

  @Override
  public void clearState() {
    this.state = new HashMap<>();
  }

  /**
   * Execute a Stellar expression.
   *
   * There are two sets of functions in Stellar.  One can be executed with
   * a PredicateProcessor and the other a TransformationProcessor.  This method
   * uses the TransformationProcessor.
   *
   * @param expression The expression to execute.
   * @param message The message that is accessible when Stellar is executed.
   */
  private Object execute(String expression, JSONObject message) {

    // vartables can be resolved from the execution state or the current message
    VariableResolver resolver = new MapVariableResolver(state, message);

    StellarProcessor processor = new StellarProcessor();
    return processor.parse(expression, resolver);
  }
}
