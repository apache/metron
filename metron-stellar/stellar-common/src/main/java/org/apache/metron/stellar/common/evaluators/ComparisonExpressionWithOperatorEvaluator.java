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

import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.FrameContext;
import org.apache.metron.stellar.common.generated.StellarParser;

/**
 * This is the evaluator used when evaluating Stellar comparison operators.
 *
 * @see EqualityOperatorsEvaluator
 * @see ComparisonOperatorsEvaluator
 */
public enum ComparisonExpressionWithOperatorEvaluator {
  /**
   * The instance of {@link ComparisonExpressionWithOperatorEvaluator} used in
   * order to evaluate Stellar comparison expressions.
   */
  INSTANCE;

  /**
   * The different strategies used to evaluate a Stellar comparison operator. They are broken into
   * two categories: equality operator comparisons and comparison operator comparisons.
   */
  enum Strategy {
    /**
     * The evaluator used to evaluate comparison operator expressions.
     */
    COMPARISON_OPERATORS(new ComparisonOperatorsEvaluator()),
    /**
     * The evaluator used to evaluate equality operator expressions.
     */
    EQUALITY_OPERATORS(new EqualityOperatorsEvaluator()),
    ;

    /**
     * The evaluator to be used when evaluating Stellar expressions.
     */
    private ComparisonExpressionEvaluator evaluator;

    Strategy(final ComparisonExpressionEvaluator evaluator) {
      this.evaluator = evaluator;
    }

    /**
     *
     * @return The evaluator needed to evaluate Stellar comparison expressions.
     */
    public ComparisonExpressionEvaluator evaluator() {
      return evaluator;
    }
  }

  /**
   * When evaluating comparison expressions with operators, they are broken into four cases:
   *
   * 1. Testing equality, see {@link EqualityOperatorsEvaluator}
   * 2. Testing not equal, see {@link EqualityOperatorsEvaluator}. This will be the negation of {@link EqualityOperatorsEvaluator#evaluate(Token, Token, StellarParser.ComparisonOpContext)}.
   * 3. Testing less than, less than or equal, greater than, and greater than or equal {@link ComparisonOperatorsEvaluator}
   * 4. Otherwise thrown {@link ParseException}.
   *
   * @param left The value of the left side of the Stellar expression.
   * @param right The value of the right side of the Stellar expression.
   * @param op The operator in the Stellar expression.
   * @return A token with type boolean. This is based on the comparison of the {@code right} and {@code left} values.
   */
  public Token<Boolean> evaluate(final Token<?> left, final Token<?> right, final StellarParser.ComparisonOpContext op, FrameContext.Context context) {
    if (op.EQ() != null) {
      return new Token<>(Strategy.EQUALITY_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class, context);
    } else if (op.NEQ() != null) {
      return new Token<>(!Strategy.EQUALITY_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class, context);
    } else if (op.LT() != null || op.GT() != null || op.LTE() != null || op.GTE() != null) {
      return new Token<>(Strategy.COMPARISON_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class, context);
    }

    throw new ParseException("Unsupported operations. The following expression is invalid: " + left.getValue() + op.getText() + right.getValue());
  }
}
