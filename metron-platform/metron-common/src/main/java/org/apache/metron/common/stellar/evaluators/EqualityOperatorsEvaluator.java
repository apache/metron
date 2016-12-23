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

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

/**
 * {@link EqualityOperatorsEvaluator} is used to evaluate equality expressions using the following operator '=='. There are four major cases when evaluating a equality expression.
 *
 * 1. If either side of the expression is null then check equality using Java's '==' expression.
 * 2. Else if both sides of the expression are of type {@link Number} then:
 *    1. If either side of the expression is a {@link Double} then use {@link Number#doubleValue()} to test equality.
 *    2. Else if either side of the expression is a {@link Float} then use {@link Number#floatValue()} to test equality.
 *    3. Else if either side of the expression is a {@link Long} then use {@link Number#longValue()} to test equality.
 *    4. Otherwise use {@link Number#intValue()} to test equality
 * 3. Otherwise use {@code equals} method compare the left side with the right side.
 */
public class EqualityOperatorsEvaluator implements ComparisonExpressionEvaluator {
  @Override
  public boolean evaluate(Token<?> left, Token<?> right, StellarParser.ComparisonOpContext op) {
    if (left.getValue() == null || right.getValue() == null) {
      return left.getValue() == right.getValue();
    } else if (left.getValue() instanceof Number && right.getValue() instanceof Number) {
      return eq((Number) left.getValue(), (Number) right.getValue());
    } else {
      return left.getValue().equals(right.getValue());
    }
  }

  private boolean eq(Number l, Number r) {
    if (l instanceof Double || r instanceof Double) {
      return l.doubleValue() == r.doubleValue();
    } else if (l instanceof Float || r instanceof Float) {
      return l.floatValue() == r.floatValue();
    } else if (l instanceof Long || r instanceof Long) {
      return l.longValue() == r.longValue();
    } else {
      return l.intValue() == r.intValue();
    }
  }
}
