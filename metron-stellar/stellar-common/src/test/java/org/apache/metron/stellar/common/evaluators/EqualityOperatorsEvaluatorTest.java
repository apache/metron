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
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class EqualityOperatorsEvaluatorTest {
  ComparisonExpressionEvaluator evaluator;

  @Before
  public void setUp() throws Exception {
    evaluator = new EqualityOperatorsEvaluator();
  }

  @Test
  public void leftAndRightNullShouldBeTrue() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(null);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(null);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    boolean evaluated = evaluator.evaluate(left, right, op);

    assertTrue(evaluated);
  }

  @Test
  public void leftNullAndRightNotShouldBeFalse() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(null);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    boolean evaluated = evaluator.evaluate(left, right, op);

    assertFalse(evaluated);
  }

  @Test
  public void leftNotNullAndRightNullShouldBeFalse() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(1D);

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(null);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    boolean evaluated = evaluator.evaluate(left, right, op);

    assertFalse(evaluated);
  }

  @Test
  public void eqTestForTwoLongs() throws Exception {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(1L);

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(1L);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertTrue(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForTwoDoubles() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(1D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertTrue(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForTwoFloats() throws Exception {
    Token<Float> left = mock(Token.class);
    when(left.getValue()).thenReturn(1F);

    Token<Float> right = mock(Token.class);
    when(right.getValue()).thenReturn(1F);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertTrue(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForTwoIntegers() throws Exception {
    Token<Integer> left = mock(Token.class);
    when(left.getValue()).thenReturn(1);

    Token<Integer> right = mock(Token.class);
    when(right.getValue()).thenReturn(1);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertTrue(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForTwoStrings() throws Exception {
    Token<String> left = mock(Token.class);
    when(left.getValue()).thenReturn("1");

    Token<String> right = mock(Token.class);
    when(right.getValue()).thenReturn("1");

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertTrue(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForUnlikeTypes() throws Exception {
    Token<String> left = mock(Token.class);
    when(left.getValue()).thenReturn("1");

    Token<Long> right = mock(Token.class);
    when(right.getValue()).thenReturn(1L);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertFalse(evaluator.evaluate(left, right, op));
  }

  @Test
  public void eqTestForUnlikeTypesLongString() throws Exception {
    Token<Long> left = mock(Token.class);
    when(left.getValue()).thenReturn(1L);

    Token<String> right = mock(Token.class);
    when(right.getValue()).thenReturn("1");

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    assertFalse(evaluator.evaluate(left, right, op));
  }
}