package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class FloatLiteralEvaluator implements NumberEvaluator<StellarParser.FloatLiteralContext> {
  @Override
  public Token<Float> evaluate(StellarParser.FloatLiteralContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Cannot evaluate a context that is null.");
    }

    return new Token<>(Float.parseFloat(context.getText()), Float.class);
  }
}
