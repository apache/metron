package org.apache.metron.common.stellar;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.stellar.evaluators.ArithmeticEvaluator;
import org.apache.metron.common.stellar.evaluators.NumberEvaluatorFactory;
import org.apache.metron.common.stellar.generated.StellarParser;
import org.junit.Before;
import org.junit.Test;

import java.util.Stack;

import static org.mockito.Mockito.*;

public class StellarCompilerTest {
  VariableResolver variableResolver;
  FunctionResolver functionResolver;
  Context context;
  Stack<Token<?>> tokenStack;
  ArithmeticEvaluator arithmeticEvaluator;
  NumberEvaluatorFactory numberEvaluatorFactory;
  StellarCompiler compiler;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    variableResolver = mock(VariableResolver.class);
    functionResolver = mock(FunctionResolver.class);
    context = mock(Context.class);
    tokenStack = mock(Stack.class);
    arithmeticEvaluator = mock(ArithmeticEvaluator.class);
    numberEvaluatorFactory = mock(NumberEvaluatorFactory.class);

    compiler = new StellarCompiler(variableResolver, functionResolver, context, tokenStack, arithmeticEvaluator, numberEvaluatorFactory);
  }

  @Test
  public void exitIntLiteralShouldProperlyParseStringsAsIntegers() throws Exception {
    StellarParser.IntLiteralContext ctx = mock(StellarParser.IntLiteralContext.class);
    when(ctx.getText()).thenReturn("1000");

    compiler.exitIntLiteral(ctx);

    verify(numberEvaluatorFactory).evaluate(ctx);
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

    verify(numberEvaluatorFactory).evaluate(ctx);
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

    verify(numberEvaluatorFactory).evaluate(ctx);
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

    verify(numberEvaluatorFactory).evaluate(ctx);
    verify(tokenStack).push(any(Token.class));
    verifyNoMoreInteractions(tokenStack);
    verifyZeroInteractions(variableResolver);
    verifyZeroInteractions(functionResolver);
    verifyZeroInteractions(context);
    verifyZeroInteractions(arithmeticEvaluator);
  }
}
