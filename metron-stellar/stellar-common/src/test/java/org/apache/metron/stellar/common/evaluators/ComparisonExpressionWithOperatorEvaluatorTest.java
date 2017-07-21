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
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings({"unchecked"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ComparisonExpressionWithOperatorEvaluator.class, ComparisonExpressionWithOperatorEvaluator.Strategy.class})
public class ComparisonExpressionWithOperatorEvaluatorTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  final ComparisonExpressionWithOperatorEvaluator evaluator = ComparisonExpressionWithOperatorEvaluator.INSTANCE;

  @Test
  public void evaluateEqShouldProperlyCallEqualityOperatorsEvaluator() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(1D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.EQ()).thenReturn(mock(TerminalNode.class));

    Token<Boolean> evaluated = evaluator.evaluate(left, right, op, null);

    assertTrue(evaluated.getValue());
  }

  @Test
  public void evaluateNotEqShouldProperlyCallEqualityOperatorsEvaluator() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(1D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.NEQ()).thenReturn(mock(TerminalNode.class));

    Token<Boolean> evaluated = evaluator.evaluate(left, right, op, null);

    assertFalse(evaluated.getValue());
  }

  @Test
  public void evaluateLessThanEqShouldProperlyCallEqualityOperatorsEvaluator() throws Exception {
    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(0D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LTE()).thenReturn(mock(TerminalNode.class));

    Token<Boolean> evaluated = evaluator.evaluate(left, right, op, null);

    assertTrue(evaluated.getValue());
  }

  @Test
  public void unexpectedOperatorShouldThrowException() throws Exception {
    exception.expect(ParseException.class);
    exception.expectMessage("Unsupported operations. The following expression is invalid: ");

    Token<Double> left = mock(Token.class);
    when(left.getValue()).thenReturn(0D);

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);

    evaluator.evaluate(left, right, op, null);
  }

  @Test
  public void nonExpectedOperatorShouldThrowException() throws Exception {
    exception.expect(ParseException.class);
    exception.expectMessage("Unsupported operations. The following expression is invalid: ");

    Token<String> left = mock(Token.class);
    when(left.getValue()).thenReturn("adsf");

    Token<Double> right = mock(Token.class);
    when(right.getValue()).thenReturn(1D);

    StellarParser.ComparisonOpContext op = mock(StellarParser.ComparisonOpContext.class);
    when(op.LTE()).thenReturn(mock(TerminalNode.class));

    evaluator.evaluate(left, right, op, null);
  }
}
