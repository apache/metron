package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IntLiteralEvaluatorTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  NumberEvaluator<StellarParser.IntLiteralContext> evaluator;
  StellarParser.IntLiteralContext context;

  @Before
  public void setUp() throws Exception {
    evaluator = new IntLiteralEvaluator();
    context = mock(StellarParser.IntLiteralContext.class);
  }

  @Test
  public void verifyHappyPathEvaluation() throws Exception {
    when(context.getText()).thenReturn("100");

    Token<? extends Number> evaluated = evaluator.evaluate(context);
    assertEquals(new Token<>(100, Integer.class), evaluated);

    verify(context).getText();
    verifyNoMoreInteractions(context);
  }

  @Test
  public void verifyNumberFormationExceptionWithEmptyString() throws Exception {
    exception.expect(NumberFormatException.class);

    when(context.getText()).thenReturn("");
    evaluator.evaluate(context);
  }

  @Test
  public void throwIllegalArgumentExceptionWhenContextIsNull() throws Exception {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot evaluate a context that is null.");

    evaluator.evaluate(null);
  }

}
