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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.ClassUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of a StellarStatefulExecutor.
 */
public class DefaultStellarStatefulExecutor implements StellarStatefulExecutor, Serializable {

  /**
   * The current state of the Stellar execution environment.
   */
  private Map<String, Object> state;

  /**
   * Provides additional context for initializing certain Stellar functions.  For
   * example, references to Zookeeper or HBase.
   */
  private Context context;

  /**
   * Responsible for function resolution.
   */
  private FunctionResolver functionResolver;

  public DefaultStellarStatefulExecutor() {
    this(StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }

  public DefaultStellarStatefulExecutor(FunctionResolver functionResolver, Context context) {
    clearState();
    this.context = context;
    this.functionResolver = functionResolver;
  }

  /**
   * @param initialState Initial state loaded into the execution environment.
   */
  public DefaultStellarStatefulExecutor(Map<String, Object> initialState) {
    this();
    this.state = new HashMap<>(initialState);
  }

  /**
   * The current state of the Stellar execution environment.
   */
  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.copyOf(state);
  }

  /**
   * Execute an expression and assign the result to a variable.  The variable is maintained
   * in the context of this executor and is available to all subsequent expressions.
   *
   * @param variable       The name of the variable to assign to.
   * @param expression     The expression to execute.
   * @param transientState Additional state available to the expression.  This most often represents
   *                       the values available to the expression from an individual message. The state
   *                       maps a variable name to a variable's value.
   */
  @Override
  public void assign(String variable, String expression, Map<String, Object> transientState) {
    Object result = execute(expression, transientState);
    if(result == null || variable == null) {
      return;
    }
    state.put(variable, result);
  }

  @Override
  public void assign(String variable, Object value) {
    if(value == null || variable == null) {
      return;
    }
    state.put(variable, value);
  }

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
  @Override
  public <T> T execute(String expression, Map<String, Object> state, Class<T> clazz) {
    Object resultObject = execute(expression, state);

    // perform type conversion, if necessary
    T result = ConversionUtils.convert(resultObject, clazz);
    if (result == null) {
      throw new IllegalArgumentException(String.format("Unexpected type: expected=%s, actual=%s, expression=%s",
              clazz.getSimpleName(), ClassUtils.getShortClassName(resultObject,"null"), expression));
    }

    return result;
  }

  @Override
  public void clearState() {
    this.state = new HashMap<>();
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  public void setFunctionResolver(FunctionResolver functionResolver) {
    this.functionResolver = functionResolver;
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expression     The expression to execute.
   * @param transientState Additional state available to the expression.  This most often represents
   *                       the values available to the expression from an individual message. The state
   *                       maps a variable name to a variable's value.
   */
  private Object execute(String expression, Map<String, Object> transientState) {
    VariableResolver variableResolver = new MapVariableResolver(state, transientState);
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(expression, variableResolver, functionResolver, context);
  }
}
