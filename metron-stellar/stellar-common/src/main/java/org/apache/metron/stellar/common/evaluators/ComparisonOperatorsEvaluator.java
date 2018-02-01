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
import org.apache.metron.stellar.common.generated.StellarParser;

/**
 * {@link ComparisonOperatorsEvaluator} is used to evaluate comparison expressions using the following
 * operator {@literal '<', '<=', '>', or '>='.} There are four major cases when evaluating a comparison expression.
 */
public class ComparisonOperatorsEvaluator implements ComparisonExpressionEvaluator {

  /**
   * 1. If either the left or right's value is null then return false.
   * 2. If both sides of the expression are instances of {@link Number} then:
   *    1. If either side is a {@link Double} then get {@link Number#doubleValue()} from both sides and compare using given operator.
   *    2. Else if either side is a {@link Float} then get {@link Number#floatValue()} from both sides and compare using given operator.
   *    3. Else if either side is a {@link Long} then get {@link Number#longValue()} from both sides and compare using given operator.
   *    4. Otherwise get {@link Number#intValue()} from both sides and compare using given operator.
   * 3. If both sides are of the same type and implement the {@link Comparable} interface then use {@code compareTo} method.
   * 4. If none of the above are met then a {@link ParseException} is thrown.
   *
   * @param left  The token representing the left side of a comparison expression.
   * @param right The token representing the right side of a comparison expression.
   * @param op    This is a representation of a comparison operator {@literal (eg. <, <=, >, >=, ==, !=) }
   * @return A boolean value based on the comparison of {@code left} and {@code right}.
   */
  @Override
  public boolean evaluate(final Token<?> left, final Token<?> right, final StellarParser.ComparisonOpContext op) {
    if (left.getValue() == null || right.getValue() == null) {
      return false;
    } else if (left.getValue() instanceof Number && right.getValue() instanceof Number) {
      return compareNumbers((Number) left.getValue(), (Number) right.getValue(), op);
    } else if (left.getValue().getClass() == right.getValue().getClass()
        && left.getValue() instanceof Comparable && right.getValue() instanceof Comparable) {
      return compare((Comparable<?>) left.getValue(), (Comparable<?>) right.getValue(), op);
    }

    throw new ParseException("Unsupported operations. The following expression is invalid: " + left.getValue() + op + right.getValue());
  }

  /**
   * This method uses the inputs' ability to compare with one another's values by using the {@code compareTo} method. It will use this and
   * the operator to evaluate the output.
   *
   * @param l The value of the left side of the expression.
   * @param r The value of the right side of the expression.
   * @param op The operator to use when comparing.
   * @param <T> The type of values being compared.
   * @return A boolean value representing the comparison of the two values with the given operator. For example, {@code 1 <= 1} would be true.
   */
  @SuppressWarnings("unchecked")
  private <T extends Comparable> boolean compare(final T l, final T r, final StellarParser.ComparisonOpContext op) {
    int compareTo = l.compareTo(r);

    if (op.LT() != null) {
      return compareTo < 0;
    } else if (op.LTE() != null) {
      return compareTo <= 0;
    } else if (op.GT() != null) {
      return compareTo > 0;
    } else if (op.GTE() != null) {
      return compareTo >= 0;
    }

    throw new ParseException("Unsupported operator: " + op);
  }

  /**
   * This method uses the inputs' ability to compare with one another's values by using the {@code compareTo} method. It will use this and
   * the operator to evaluate the output.
   *
   * @param l The left side of the expression.
   * @param r The right side of the expression
   * @param op The operator used in the expression.
   * @return A boolean value representing the comparison of the two values with the given operator. For example, {@code 1 <= 1} would be true.
   */
  private boolean compareNumbers(final Number l, final Number r, final StellarParser.ComparisonOpContext op) {
    if (op.LT() != null) {
      return lessThan(l, r);
    } else if (op.LTE() != null) {
      return lessThanEqual(l, r);
    } else if (op.GT() != null) {
      return greaterThan(l, r);
    } else if (op.GTE() != null) {
      return greaterThanEqual(l, r);
    }

    throw new ParseException("Unsupported operator: " + op);
  }

  /**
   * If the left side of the expression is less than the right then true otherwise false.
   *
   * @param l The value of the left side of the expression.
   * @param r The value of the right side of the expression.
   * @return If the left side of the expression is less than the right then true otherwise false.
   */
  private boolean lessThan(final Number l, final Number r) {
    if (l instanceof Double || r instanceof Double) {
      return l.doubleValue() < r.doubleValue();
    } else if (l instanceof Float || r instanceof Float) {
      return l.floatValue() < r.floatValue();
    } else if (l instanceof Long || r instanceof Long) {
      return l.longValue() < r.longValue();
    } else {
      return l.intValue() < r.intValue();
    }
  }

  /**
   * If the left side of the expression is less than or equal to the right then true otherwise false.
   *
   * @param l The value of the left side of the expression.
   * @param r The value of the right side of the expression.
   * @return If the left side of the expression is less than or equal to the right then true otherwise false.
   */
  private boolean lessThanEqual(final Number l, final Number r) {
    if (l instanceof Double || r instanceof Double) {
      return l.doubleValue() <= r.doubleValue();
    } else if (l instanceof Float || r instanceof Float) {
      return l.floatValue() <= r.floatValue();
    } else if (l instanceof Long || r instanceof Long) {
      return l.longValue() <= r.longValue();
    } else {
      return l.intValue() <= r.intValue();
    }
  }

  /**
   * If the left side of the expression is greater than the right then true otherwise false.
   *
   * @param l The value of the left side of the expression.
   * @param r The value of the right side of the expression.
   * @return If the left side of the expression is greater than the right then true otherwise false.
   */
  private boolean greaterThan(final Number l, final Number r) {
    if (l instanceof Double || r instanceof Double) {
      return l.doubleValue() > r.doubleValue();
    } else if (l instanceof Float || r instanceof Float) {
      return l.floatValue() > r.floatValue();
    } else if (l instanceof Long || r instanceof Long) {
      return l.longValue() > r.longValue();
    } else {
      return l.intValue() > r.intValue();
    }
  }

  /**
   * If the left side of the expression is greater than or equal to the right then true otherwise false.
   *
   * @param l The value of the left side of the expression.
   * @param r The value of the right side of the expression.
   * @return If the left side of the expression is greater than or equal to the right then true otherwise false.
   */
  private boolean greaterThanEqual(final Number l, final Number r) {
    if (l instanceof Double || r instanceof Double) {
      return l.doubleValue() >= r.doubleValue();
    } else if (l instanceof Float || r instanceof Float) {
      return l.floatValue() >= r.floatValue();
    } else if (l instanceof Long || r instanceof Long) {
      return l.longValue() >= r.longValue();
    } else {
      return l.intValue() >= r.intValue();
    }
  }
}
