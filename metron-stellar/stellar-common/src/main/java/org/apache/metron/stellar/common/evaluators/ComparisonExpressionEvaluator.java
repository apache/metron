/*
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

package org.apache.metron.stellar.common.evaluators;

import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.generated.StellarParser;

/**
 * This is used to determine what is needed to evaluate a Stellar comparison expression. A Stellar comparison
 * expression is an expression that uses operators such as {@literal '<', '<=', '>', '>=', '==', '!=' } to compare
 * values in Stellar. There are two main types of comparisons in Stellar,
 * {@literal equality ('==', '!=') and comparison ('<', '<=', '>', '>='). }
 */
public interface ComparisonExpressionEvaluator {

  /**
   * This will compare the values of {@code left} and {@code right} using the {@code op} input to determine a value
   * to return.
   * @param left  The token representing the left side of a comparison expression.
   * @param right The token representing the right side of a comparison expression.
   * @param op    This is a representation of a comparison operator {@literal (eg. <, <=, >, >=, ==, !=) }
   * @return True if the expression is evaluated to be true, otherwise false. An example of expressions that
   * should be true are {@code 1 == 1}, {@code 1f > 0}, etc.
   */
  boolean evaluate(Token<?> left, Token<?> right, StellarParser.ComparisonOpContext op);
}
