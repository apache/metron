package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public class LongLiteralEvaluator implements NumberEvaluator<StellarParser.LongLiteralContext> {
  @Override
  public Token<Long> evaluate(StellarParser.LongLiteralContext context) {
    if (context == null) {
      throw new IllegalArgumentException("Cannot evaluate a context that is null.");
    }

    String value = context.getText();
    if (value.endsWith("l") || value.endsWith("L")) {
      value = value.substring(0, value.length() - 1); // Drop the 'L' or 'l'. Long.parseLong does not accept a string with either of these.
      return new Token<>(Long.parseLong(value), Long.class);
    } else {
      // Technically this should never happen, but just being safe.
      throw new ParseException("Invalid format for long. Failed trying to parse a long with the following value: " + value);
    }
  }
}
