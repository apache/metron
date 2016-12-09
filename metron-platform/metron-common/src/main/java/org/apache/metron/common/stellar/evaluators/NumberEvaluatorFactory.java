package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class NumberEvaluatorFactory {
  private NumberEvaluator<StellarParser.IntLiteralContext> intLiteralEvaluator;
  private NumberEvaluator<StellarParser.DoubleLiteralContext> doubleLiteralEvaluator;
  private NumberEvaluator<StellarParser.FloatLiteralContext> floatLiteralEvaluator;
  private NumberEvaluator<StellarParser.LongLiteralContext> longLiteralEvaluator;

  public NumberEvaluatorFactory(NumberEvaluator<StellarParser.IntLiteralContext> intLiteralEvaluator,
                                NumberEvaluator<StellarParser.DoubleLiteralContext> doubleLiteralEvaluator,
                                NumberEvaluator<StellarParser.FloatLiteralContext> floatLiteralEvaluator,
                                NumberEvaluator<StellarParser.LongLiteralContext> longLiteralEvaluator) {
    this.intLiteralEvaluator = intLiteralEvaluator;
    this.doubleLiteralEvaluator = doubleLiteralEvaluator;
    this.floatLiteralEvaluator = floatLiteralEvaluator;
    this.longLiteralEvaluator = longLiteralEvaluator;
  }

  public NumberEvaluatorFactory() {
    this.intLiteralEvaluator = new IntLiteralEvaluator();
    this.doubleLiteralEvaluator = new DoubleLiteralEvaluator();
    this.floatLiteralEvaluator = new FloatLiteralEvaluator();
    this.longLiteralEvaluator = new LongLiteralEvaluator();
  }

  public Token<? extends Number> evaluate(StellarParser.Arithmetic_operandsContext context) {
    if (context instanceof StellarParser.IntLiteralContext) {
      return intLiteralEvaluator.evaluate((StellarParser.IntLiteralContext) context);
    } else if (context instanceof StellarParser.DoubleLiteralContext) {
      return doubleLiteralEvaluator.evaluate((StellarParser.DoubleLiteralContext) context);
    } else if (context instanceof StellarParser.FloatLiteralContext) {
      return floatLiteralEvaluator.evaluate((StellarParser.FloatLiteralContext) context);
    } else if (context instanceof StellarParser.LongLiteralContext) {
      return longLiteralEvaluator.evaluate((StellarParser.LongLiteralContext) context);
    }

    throw new ParseException("Does not support evaluation for type " + context.getClass());
  }

}
