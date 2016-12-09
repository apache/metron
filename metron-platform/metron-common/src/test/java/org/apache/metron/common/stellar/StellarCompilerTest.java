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

package org.apache.metron.common.stellar;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.stellar.evaluators.ArithmeticEvaluator;
import org.apache.metron.common.stellar.evaluators.NumberLiteralEvaluator;
import org.apache.metron.common.stellar.generated.StellarParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Stack;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ArithmeticEvaluator.class, NumberLiteralEvaluator.class})
public class StellarCompilerTest {
  VariableResolver variableResolver;
  FunctionResolver functionResolver;
  Context context;
  Stack<Token<?>> tokenStack;
  ArithmeticEvaluator arithmeticEvaluator;
  NumberLiteralEvaluator numberLiteralEvaluator;
  StellarCompiler compiler;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    variableResolver = mock(VariableResolver.class);
    functionResolver = mock(FunctionResolver.class);
    context = mock(Context.class);
    tokenStack = mock(Stack.class);
    arithmeticEvaluator = mock(ArithmeticEvaluator.class);
    numberLiteralEvaluator = mock(NumberLiteralEvaluator.class);

    compiler = new StellarCompiler(variableResolver, functionResolver, context, tokenStack, arithmeticEvaluator, numberLiteralEvaluator);
  }

  @Test
  public void exitIntLiteralShouldProperlyParseStringsAsIntegers() throws Exception {
    StellarParser.IntLiteralContext ctx = mock(StellarParser.IntLiteralContext.class);
    when(ctx.getText()).thenReturn("1000");

    compiler.exitIntLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx);
    verify(tokenStack).push(any(Token.class));
    verifyNoMoreInteractions(tokenStack);
    verifyZeroInteractions(variableResolver);
    verifyZeroInteractions(functionResolver);
    verifyZeroInteractions(context);
    verifyZeroInteractions(arithmeticEvaluator);
  }

  @Test
  public void exitDoubleLiteralShouldProperlyParseStringsAsDoubles() throws Exception {
    StellarParser.DoubleLiteralContext ctx = mock(StellarParser.DoubleLiteralContext.class);
    when(ctx.getText()).thenReturn("1000D");

    compiler.exitDoubleLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx);
    verify(tokenStack).push(any(Token.class));
    verifyNoMoreInteractions(tokenStack);
    verifyZeroInteractions(variableResolver);
    verifyZeroInteractions(functionResolver);
    verifyZeroInteractions(context);
    verifyZeroInteractions(arithmeticEvaluator);
  }

  @Test
  public void exitFloatLiteralShouldProperlyParseStringsAsFloats() throws Exception {
    StellarParser.FloatLiteralContext ctx = mock(StellarParser.FloatLiteralContext.class);
    when(ctx.getText()).thenReturn("1000f");

    compiler.exitFloatLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx);
    verify(tokenStack).push(any(Token.class));
    verifyNoMoreInteractions(tokenStack);
    verifyZeroInteractions(variableResolver);
    verifyZeroInteractions(functionResolver);
    verifyZeroInteractions(context);
    verifyZeroInteractions(arithmeticEvaluator);
  }

  @Test
  public void exitLongLiteralShouldProperlyParseStringsAsLongs() throws Exception {
    StellarParser.LongLiteralContext ctx = mock(StellarParser.LongLiteralContext.class);
    when(ctx.getText()).thenReturn("1000l");

    compiler.exitLongLiteral(ctx);

    verify(numberLiteralEvaluator).evaluate(ctx);
    verify(tokenStack).push(any(Token.class));
    verifyNoMoreInteractions(tokenStack);
    verifyZeroInteractions(variableResolver);
    verifyZeroInteractions(functionResolver);
    verifyZeroInteractions(context);
    verifyZeroInteractions(arithmeticEvaluator);
  }
}
