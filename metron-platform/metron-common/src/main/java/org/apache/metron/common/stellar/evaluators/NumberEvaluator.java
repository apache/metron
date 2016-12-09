package org.apache.metron.common.stellar.evaluators;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.stellar.generated.StellarParser;

public interface NumberEvaluator<T extends StellarParser.Arithmetic_operandsContext> {
  Token<? extends Number> evaluate(T context);
}
