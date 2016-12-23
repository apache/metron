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
 * {@link ComparisonOperatorsEvaluator} is used to evaluate comparison expressions using the following operator '<', '<=', '>',
 * or '>='. There are four major cases when evaluating a comparison expression.
 *
 * 1. If either the left or right's value is null then return false.
 * 2. If both sides of the expression are instances of {@link Number} then:
 *    1. If either side is a {@link Double} then get {@link Number#doubleValue()} from both sides and compare using given operator.
 *    2. Else if either side is a {@link Float} then get {@link Number#floatValue()} from both sides and compare using given operator.
 *    3. Else if either side is a {@link Long} then get {@link Number#longValue()} from both sides and compare using given operator.
 *    4. Otherwise get {@link Number#intValue()} from both sides and compare using given operator.
 * 3. If both sides are of the same type and implement the {@link Comparable} interface then use {@code compareTo} method.
 * 4. If none of the above are met then a {@link ParseException} is thrown.
 */
public class ComparisonOperatorsEvaluator implements ComparisonExpressionEvaluator {
  @Override
  public boolean evaluate(Token<?> left, Token<?> right, StellarParser.ComparisonOpContext op) {
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

  @SuppressWarnings("unchecked")
  private <T extends Comparable> boolean compare(T l, T r, StellarParser.ComparisonOpContext op) {
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

  private boolean compareNumbers(Number l, Number r, StellarParser.ComparisonOpContext op) {
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

  private boolean lessThan(Number l, Number r) {
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

  private boolean lessThanEqual(Number l, Number r) {
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

  private boolean greaterThan(Number l, Number r) {
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

  private boolean greaterThanEqual(Number l, Number r) {
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
