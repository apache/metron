package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class DoubleLiteralEvaluator implements NumberEvaluator<StellarParser.DoubleLiteralContext> {
  @Override
  public Token<Double> evaluate(StellarParser.DoubleLiteralContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Cannot evaluate a context that is null.");
    }

    return new Token<>(Double.parseDouble(context.getText()), Double.class);
  }
}
