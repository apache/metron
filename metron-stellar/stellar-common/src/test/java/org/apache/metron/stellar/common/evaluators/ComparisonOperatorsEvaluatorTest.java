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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class ComparisonOperatorsEvaluatorTest {
  ComparisonExpressionEvaluator evaluator;

  @BeforeEach
  public void setUp() throws Exception {
    evaluator = new ComparisonOperatorsEvaluator();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void nonSupportedOperatorThrowsExceptionNonNumbericComparable() {
    Token<String> left = mock(Token.class);
    when(left.getValue()).thenReturn("b");

    Token<String> right = mock(Token.class);
    when(right.getValue()).thenReturn("a");

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);

    ParseException e = assertThrows(ParseException.class, () -> evaluator.evaluate(left, right, op));
    assertTrue(e.getMessage().contains("Unsupported operator: "));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void nonSupportedOperatorThrowsExceptionNumbericComparison() {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(1L);

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(0L);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);

    ParseException e = assertThrows(ParseException.class, () -> evaluator.evaluate(left, right, op));
    assertTrue(e.getMessage().contains("Unsupported operator: "));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void leftIsNullThenThrowException() {
    Token<Long> left = mock(Token.class);
    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(1L);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LT()).thenReturn(mock(TerminalNode.class));

    assertFalse(evaluator.evaluate(left, right, op));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rightIsNullThenReturnFalse() {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(1L);
    Token<Long> right = mock(Token.class);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LT()).thenReturn(mock(TerminalNode.class));

    assertFalse(evaluator.evaluate(left, right, op));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rightAndLeftIsNullThenReturnFalse() {
    Token<Long> left = mock(Token.class);
    Token<Long> right = mock(Token.class);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LT()).thenReturn(mock(TerminalNode.class));

    assertFalse(evaluator.evaluate(left, right, op));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void throwParseExceptionWhenTryingToCompareNonComparable() {
    Token<Serializable> left = mock(Token.class);
    when(left.getValue()).thenReturn(mock(Serializable.class));

    Token<Serializable> right = mock(Token.class);
    when(right.getValue()).thenReturn(mock(Serializable.class));

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LT()).thenReturn(mock(TerminalNode.class));

    ParseException e = assertThrows(ParseException.class, () -> evaluator.evaluate(left, right, op));
    assertTrue(e.getMessage().contains("Unsupported operations. The following expression is invalid: "));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsProperlyWorkForLongs() {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(0L);

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(1L);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsProperlyWorkForDoubles() {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(0D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsProperlyWorkForFloats() {
    Token<Float> left = mock(Token.class);
    when(left.getValue()).thenReturn(0F);

    Token<Float> right = mock(Token.class);
    when(right.getValue()).thenReturn(1F);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsProperlyWorkForInts() {
    Token<Integer> left = mock(Token.class);
    when(left.getValue()).thenReturn(0);

    Token<Integer> right = mock(Token.class);
    when(right.getValue()).thenReturn(1);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForMixedTypesDoublesLong() {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(1L);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1.0000001D);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertTrue(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertFalse(evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForMixedTypesDoublesFloat() {
    final double leftValue = 1.0000001D;
    final float rightValue = 1.0000001F;

    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(leftValue);

    Token<Float> right = mock(Token.class);
    when(right.getValue()).thenReturn(rightValue);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue < rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue <= rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue > rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue >= rightValue, evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForMixedTypesFloatIntegers() {
    final int leftValue = 1;
    final float rightValue = 1.0000001F;

    Token<Integer> left = mock(Token.class);
    when(left.getValue()).thenReturn(leftValue);

    Token<Float> right = mock(Token.class);
    when(right.getValue()).thenReturn(rightValue);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue < rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue <= rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue > rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue >= rightValue, evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForMixedTypesFloatIntegers2() {
    final int leftValue = 1;
    final float rightValue = 1.00000001F;

    Token<Integer> left = mock(Token.class);
    when(left.getValue()).thenReturn(leftValue);

    Token<Float> right = mock(Token.class);
    when(right.getValue()).thenReturn(rightValue);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue < rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue <= rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue > rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue >= rightValue, evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForMixedTypesLongIntegers() {
    final int leftValue = 1;
    final long rightValue = 3L;

    Token<Integer> left = mock(Token.class);
    when(left.getValue()).thenReturn(leftValue);

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(rightValue);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue < rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue <= rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue > rightValue, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue >= rightValue, evaluator.evaluate(left, right, op));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void makeSureAllOperatorsWorkForNonIntegerComparableTypes() {
    final String leftValue = "a";
    final String rightValue = "b";

    Token<String> left = mock(Token.class);
    when(left.getValue()).thenReturn(leftValue);

    Token<String> right = mock(Token.class);
    when(right.getValue()).thenReturn(rightValue);

    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue.compareTo(rightValue) < 0, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.LTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue.compareTo(rightValue) <= 0, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GT()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue.compareTo(rightValue) > 0, evaluator.evaluate(left, right, op));
    }
    {
      StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
      when(op.GTE()).thenReturn(mock(TerminalNode.class));
      assertEquals(leftValue.compareTo(rightValue) >= 0, evaluator.evaluate(left, right, op));
    }
  }
}
