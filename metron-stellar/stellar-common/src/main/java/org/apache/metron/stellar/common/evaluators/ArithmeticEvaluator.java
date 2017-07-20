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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.FrameContext;

import java.util.function.BiFunction;

public enum ArithmeticEvaluator {
  INSTANCE;

  public Token<? extends Number> evaluate(BiFunction<Number, Number, Token<? extends Number>> function,
                                          Pair<Token<? extends Number>, Token<? extends Number>> p) {
    if (p == null || p.getKey() == null || p.getValue() == null) {
      throw new IllegalArgumentException();
    }

    final Number l = p.getKey().getValue();
    final Number r = p.getValue().getValue();

    return function.apply(l == null ? 0 : l, r == null ? 0 : r);
  }

  /**
   * This is a helper class that defines how to handle arithmetic operations. The conversion between number
   * types is taken for the Java spec: http://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.6.2
   */
  public static final class ArithmeticEvaluatorFunctions {
    public static BiFunction<Number, Number, Token<? extends Number>> addition(final FrameContext.Context context) {
      return (Number l, Number r) -> {
        if (l instanceof Double || r instanceof Double) {
          return new Token<>(l.doubleValue() + r.doubleValue(), Double.class, context);
        } else if (l instanceof Float || r instanceof Float) {
          return new Token<>(l.floatValue() + r.floatValue(), Float.class, context);
        } else if (l instanceof Long || r instanceof Long) {
          return new Token<>(l.longValue() + r.longValue(), Long.class, context);
        } else {
          return new Token<>(l.intValue() + r.intValue(), Integer.class, context);
        }
      };
    }

    public static BiFunction<Number, Number, Token<? extends Number>> multiplication(final FrameContext.Context context) {
      return (Number l, Number r) -> {
        if (l instanceof Double || r instanceof Double) {
          return new Token<>(l.doubleValue() * r.doubleValue(), Double.class, context);
        } else if (l instanceof Float || r instanceof Float) {
          return new Token<>(l.floatValue() * r.floatValue(), Float.class, context);
        } else if (l instanceof Long || r instanceof Long) {
          return new Token<>(l.longValue() * r.longValue(), Long.class, context);
        } else {
          return new Token<>(l.intValue() * r.intValue(), Integer.class, context);
        }
      };
    }

    public static BiFunction<Number, Number, Token<? extends Number>> subtraction(final FrameContext.Context context) {
      return (Number l, Number r) -> {
        if (l instanceof Double || r instanceof Double) {
          return new Token<>(l.doubleValue() - r.doubleValue(), Double.class, context);
        } else if (l instanceof Float || r instanceof Float) {
          return new Token<>(l.floatValue() - r.floatValue(), Float.class, context);
        } else if (l instanceof Long || r instanceof Long) {
          return new Token<>(l.longValue() - r.longValue(), Long.class, context);
        } else {
          return new Token<>(l.intValue() - r.intValue(), Integer.class, context);
        }
      };
    }

    public static BiFunction<Number, Number, Token<? extends Number>> division(FrameContext.Context context) {
      return (Number l, Number r) -> {
        if (l instanceof Double || r instanceof Double) {
          return new Token<>(l.doubleValue() / r.doubleValue(), Double.class, context);
        } else if (l instanceof Float || r instanceof Float) {
          return new Token<>(l.floatValue() / r.floatValue(), Float.class, context);
        } else if (l instanceof Long || r instanceof Long) {
          return new Token<>(l.longValue() / r.longValue(), Long.class, context);
        } else {
          return new Token<>(l.intValue() / r.intValue(), Integer.class, context);
        }
      };
    }
  }
}
