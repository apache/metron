package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class IntLiteralEvaluator implements NumberEvaluator<StellarParser.IntLiteralContext> {
  @Override
  public Token<Integer> evaluate(StellarParser.IntLiteralContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Cannot evaluate a context that is null.");
    }

    return new Token<>(Integer.parseInt(context.getText()), Integer.class);
  }
}
