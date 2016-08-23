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
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.stellar.StellarProcessor;
import org.apache.metron.common.utils.ConversionUtils;
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
   * @param variable The variable name to assign to.
   * @param expression The expression to execute.
   * @param message The message that provides additional context for the expression.
   * @param stellarContext The context which holds global state for Stellar functions
   */
  @Override
  public void assign(String variable, String expression, JSONObject message, Context stellarContext) {
    Object result = execute(expression, message, stellarContext);
    state.put(variable, result);
  }

  /**
   * Execute a Stellar expression and returns the result.
   *
   * @param expr The expression to execute.
   * @param message The message that is accessible when Stellar is executed.
   * @param clazz The expected class of the expression's result.
   * @param <T> The expected class of the expression's result.
   * @param stellarContext The context which holds global state for Stellar functions
   */
  @Override
  public <T> T execute(String expr, JSONObject message, Class<T> clazz, Context stellarContext) {
    Object resultObject = execute(expr, message, stellarContext);

    // perform type conversion, if necessary
    T result = ConversionUtils.convert(resultObject, clazz);
    if(result == null) {
      throw new IllegalArgumentException(String.format("Unexpected type: expected=%s, actual=%s, expression=%s",
              clazz.getSimpleName(), resultObject.getClass().getSimpleName(), expr));
    }

    return result;
  }

  @Override
  public void clearState() {
    this.state = new HashMap<>();
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expr The expression to execute.
   * @param msg The message that is accessible when Stellar is executed.
   * @param stellarContext The context which holds global state for Stellar functions
   */
  private Object execute(String expr, JSONObject msg, Context stellarContext) {
    try {
      VariableResolver resolver = new MapVariableResolver(state, msg);
      StellarProcessor processor = new StellarProcessor();
      return processor.parse(expr, resolver, StellarFunctions.FUNCTION_RESOLVER(), stellarContext);

    } catch (ParseException e) {
      throw new ParseException(String.format("Bad expression: expr=%s, msg=%s, state=%s", expr, msg, state));
    }
  }
}
