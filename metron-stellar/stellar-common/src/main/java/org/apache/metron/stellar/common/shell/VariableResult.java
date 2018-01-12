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
package org.apache.metron.stellar.common.shell;

import java.util.Optional;

/**
 * The value assigned to a variable.
 *
 * Allows us to maintain not only the resulting value, but the
 * expression that resulted in that value.
 */
public class VariableResult {

  /**
   * The expression that resulted in the value.  Not always available.
   */
  private Optional<String> expression;

  /**
   * The value of the variable.
   */
  private Object result;

  /**
   * Create a new VariableResult when the expression that resulted in a value is known.
   *
   * @param value The value.
   * @param expression The expression that resulted in the given value.
   * @return A VariableResult.
   */
  public static VariableResult withExpression(Object value, String expression) {
    return new VariableResult(Optional.of(expression), value);
  }

  /**
   * Create a new VariableResult when the expression that resulted in a value is known.
   *
   * @param value The value.
   * @param expression The expression that resulted in the given value.
   * @return A VariableResult.
   */
  public static VariableResult withExpression(Object value, Optional<String> expression) {
    return new VariableResult(expression, value);
  }

  /**
   * Create a new VariableResult when only the value is known.
   *
   * @param value The value.
   * @return A VariableResult.
   */
  public static VariableResult withValue(Object value) {
    return new VariableResult(Optional.empty(), value);
  }

  /**
   * Private constructor.  Use the static method 'withExpression' and 'withValue'.
   *
   * @param expression The expression that resulted in the given value.
   * @param result The value assigned to the variable.
   */
  private VariableResult(Optional<String> expression, Object result) {
    this.expression = expression;
    this.result = result;
  }

  public Optional<String> getExpression() {
    return expression;
  }

  public Object getResult() {
    return result;
  }

  @Override
  public String toString() {
    String ret = "" + result;
    if(getExpression().isPresent()) {
      ret += " via " + expression.get();
    }
    return ret;
  }
}