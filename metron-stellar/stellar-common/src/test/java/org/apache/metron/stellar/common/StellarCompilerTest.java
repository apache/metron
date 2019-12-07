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

package org.apache.metron.stellar.common;

import org.apache.metron.stellar.common.evaluators.ArithmeticEvaluator;
import org.apache.metron.stellar.common.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.stellar.common.evaluators.NumberLiteralEvaluator;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class StellarCompilerTest  {
  VariableResolver variableResolver;
  FunctionResolver functionResolver;
  Context context;
  Deque<Token<?>> tokenStack;
  ArithmeticEvaluator arithmeticEvaluator;
  NumberLiteralEvaluator numberLiteralEvaluator;
  ComparisonExpressionWithOperatorEvaluator comparisonExpressionWithOperatorEvaluator;
  StellarCompiler compiler;
  StellarCompiler.Expression expression;

  @BeforeEach
  public void setUp() throws Exception {
    variableResolver = mock(VariableResolver.class);
    functionResolver = mock(FunctionResolver.class);
    context = mock(Context.class);
    tokenStack = new ArrayDeque<>();
    arithmeticEvaluator = mock(ArithmeticEvaluator.class);
    numberLiteralEvaluator = mock(NumberLiteralEvaluator.class);
    comparisonExpressionWithOperatorEvaluator = mock(ComparisonExpressionWithOperatorEvaluator.class);
    expression = new StellarCompiler.Expression(tokenStack);
    compiler = new StellarCompiler(expression, arithmeticEvaluator, numberLiteralEvaluator, comparisonExpressionWithOperatorEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void exitIntLiteralShouldProperlyParseStringsAsIntegers() {
    StellarParser.IntLiteralContext ctx = mock(StellarParser.IntLiteralContext.class);
    Token result = mock(Token.class);
    when(ctx.getText()).thenReturn("1000");
    when(numberLiteralEvaluator.evaluate(ctx, null)).thenReturn(result);
    compiler.exitIntLiteral(ctx);
    verify(numberLiteralEvaluator).evaluate(ctx, null);
    assertEquals(1, tokenStack.size());
    assertEquals(tokenStack.getFirst(), result);
    verifyNoInteractions(variableResolver);
    verifyNoInteractions(functionResolver);
    verifyNoInteractions(context);
    verifyNoInteractions(arithmeticEvaluator);
    verifyNoInteractions(comparisonExpressionWithOperatorEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void exitDoubleLiteralShouldProperlyParseStringsAsDoubles() {
    StellarParser.DoubleLiteralContext ctx = mock(StellarParser.DoubleLiteralContext.class);
    Token result = mock(Token.class);
    when(numberLiteralEvaluator.evaluate(ctx, null)).thenReturn(result);
    when(ctx.getText()).thenReturn("1000D");

    compiler.exitDoubleLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx, null);
    assertEquals(1, tokenStack.size());
    assertEquals(tokenStack.getFirst(), result);
    verifyNoInteractions(variableResolver);
    verifyNoInteractions(functionResolver);
    verifyNoInteractions(context);
    verifyNoInteractions(arithmeticEvaluator);
    verifyNoInteractions(comparisonExpressionWithOperatorEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void exitFloatLiteralShouldProperlyParseStringsAsFloats() {
    StellarParser.FloatLiteralContext ctx = mock(StellarParser.FloatLiteralContext.class);
    when(ctx.getText()).thenReturn("1000f");
    Token result = mock(Token.class);
    when(numberLiteralEvaluator.evaluate(ctx, null)).thenReturn(result);

    compiler.exitFloatLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx, null);
    assertEquals(1, tokenStack.size());
    assertEquals(tokenStack.getFirst(), result);
    verifyNoInteractions(variableResolver);
    verifyNoInteractions(functionResolver);
    verifyNoInteractions(context);
    verifyNoInteractions(arithmeticEvaluator);
    verifyNoInteractions(comparisonExpressionWithOperatorEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void exitLongLiteralShouldProperlyParseStringsAsLongs() {
    StellarParser.LongLiteralContext ctx = mock(StellarParser.LongLiteralContext.class);
    when(ctx.getText()).thenReturn("1000l");
    Token result = mock(Token.class);
    when(numberLiteralEvaluator.evaluate(ctx, null)).thenReturn(result);

    compiler.exitLongLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx, null);
    assertEquals(1, tokenStack.size());
    assertEquals(tokenStack.getFirst(), result);
    verifyNoInteractions(variableResolver);
    verifyNoInteractions(functionResolver);
    verifyNoInteractions(context);
    verifyNoInteractions(arithmeticEvaluator);
    verifyNoInteractions(comparisonExpressionWithOperatorEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void properlyCompareTwoNumbers() {
    StellarParser.ComparisonExpressionWithOperatorContext ctx = mock(StellarParser.ComparisonExpressionWithOperatorContext.class);
    StellarParser.ComparisonOpContext mockOp = mock(StellarParser.ComparisonOpContext.class);
    when(ctx.comp_operator()).thenReturn(mockOp);
    Token result = mock(Token.class);
    when(comparisonExpressionWithOperatorEvaluator.evaluate(any(Token.class), any(Token.class), any(StellarParser.ComparisonOpContext.class), any())).thenReturn(result);

    compiler.exitComparisonExpressionWithOperator(ctx);
    assertEquals(1, tokenStack.size());
    StellarCompiler.DeferredFunction func = (StellarCompiler.DeferredFunction) tokenStack.pop().getValue();
    tokenStack.push(new Token<>(1000, Integer.class, null));
    tokenStack.push(new Token<>(1500f, Float.class, null));
    func.apply(tokenStack, new StellarCompiler.ExpressionState(context, functionResolver, variableResolver));
    assertEquals(1, tokenStack.size());
    assertEquals(tokenStack.getFirst(), result);
    verify(comparisonExpressionWithOperatorEvaluator).evaluate(any(Token.class), any(Token.class), eq(mockOp), any());
    verifyNoInteractions(numberLiteralEvaluator);
    verifyNoInteractions(variableResolver);
    verifyNoInteractions(functionResolver);
    verifyNoInteractions(context);
    verifyNoInteractions(arithmeticEvaluator);
  }
}
