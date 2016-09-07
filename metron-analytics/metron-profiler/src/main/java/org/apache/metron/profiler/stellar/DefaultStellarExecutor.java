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

  /**
   * Provides additional context for initializing certain Stellar functions.  For
   * example, references to find Zookeeper or HBase.
   */
  private Context context;

  public DefaultStellarExecutor() {
    clearState();
    context = Context.EMPTY_CONTEXT();
  }

  /**
   * @param initialState Initial state loaded into the execution environment.
   */
  public DefaultStellarExecutor(Map<String, Object> initialState) {
    this();
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
   */
  @Override
  public void assign(String variable, String expression, Map<String, Object> message) {
    Object result = execute(expression, message);
    state.put(variable, result);
  }

  /**
   * Execute a Stellar expression and returns the result.
   *
   * @param expr The expression to execute.
   * @param message The message that is accessible when Stellar is executed.
   * @param clazz The expected class of the expression's result.
   * @param <T> The expected class of the expression's result.
   */
  @Override
  public <T> T execute(String expr, Map<String, Object> message, Class<T> clazz) {
    Object resultObject = execute(expr, message);

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
   * Sets the Context for the Stellar execution environment.  This provides global data used
   * to initialize Stellar functions.
   * @param context The Stellar context.
   */
  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expr The expression to execute.
   * @param msg The message that is accessible when Stellar is executed.
   */
  private Object execute(String expr, Map<String, Object> msg) {
    try {
      VariableResolver resolver = new MapVariableResolver(state, msg);
      StellarProcessor processor = new StellarProcessor();
      return processor.parse(expr, resolver, StellarFunctions.FUNCTION_RESOLVER(), context);

    } catch (ParseException e) {
      throw new ParseException(String.format("Bad expression: expr=%s, msg=%s, state=%s", expr, msg, state));
    }
  }
}
