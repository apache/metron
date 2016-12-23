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

package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

/**
 * When evaluating comparison expressions with operators, they are broken into four cases:
 *
 * 1. Testing equality, see {@link EqualityOperatorsEvaluator}
 * 2. Testing not equal, see {@link EqualityOperatorsEvaluator}. This will be the negation of {@link EqualityOperatorsEvaluator#evaluate(Token, Token, StellarParser.ComparisonOpContext)}.
 * 3. Testing less than, less than or equal, greater than, and greater than or equal {@link ComparisonOperatorsEvaluator}
 * 4. Otherwise thrown {@link ParseException}.
 *
 * @see EqualityOperatorsEvaluator
 * @see ComparisonOperatorsEvaluator
 */
public enum ComparisonExpressionWithOperatorEvaluator {
  INSTANCE;

  enum Strategy {
    COMPARISON_OPERATORS(new ComparisonOperatorsEvaluator()),
    EQUALITY_OPERATORS(new EqualityOperatorsEvaluator()),
    ;

    private ComparisonExpressionEvaluator evaluator;

    Strategy(ComparisonExpressionEvaluator evaluator) {
      this.evaluator = evaluator;
    }

    public ComparisonExpressionEvaluator evaluator() {
      return evaluator;
    }
  }

  public Token<Boolean> evaluate(Token<?> left, Token<?> right, StellarParser.ComparisonOpContext op) {
    if (op.EQ() != null) {
      return new Token<>(Strategy.EQUALITY_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class);
    } else if (op.NEQ() != null) {
      return new Token<>(!Strategy.EQUALITY_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class);
    } else if (op.LT() != null || op.GT() != null || op.LTE() != null || op.GTE() != null) {
      return new Token<>(Strategy.COMPARISON_OPERATORS.evaluator().evaluate(left, right, op), Boolean.class);
    }

    throw new ParseException("Unsupported operations. The following expression is invalid: " + left.getValue() + op.getText() + right.getValue());
  }
}
