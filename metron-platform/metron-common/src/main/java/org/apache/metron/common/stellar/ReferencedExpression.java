package org.apache.metron.common.stellar;

import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.dsl.VariableResolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ReferencedExpression extends StellarCompiler.Expression {
  StellarCompiler.ExpressionState state;
  public ReferencedExpression(Deque<Token<?>> tokenDeque, StellarCompiler.ExpressionState state) {
    super(tokenDeque);
    this.state = state;
  }

  @Override
  public Deque<Token<?>> getTokenDeque() {
    Deque<Token<?>> ret = new ArrayDeque<>(super.getTokenDeque().size());
    for(Token<?> token : super.getTokenDeque()) {
      ret.add(token);
    }
    return ret;
  }

  public Object apply(List<Object> variables) {
    VariableResolver variableResolver = new VariableResolver() {
      @Override
      public Object resolve(String variable) {
        if(variable.startsWith("$")) {
          int idx = Integer.parseInt(variable.substring(1));
          return variables.get(idx);
        }
        return state.variableResolver.resolve(variable);
      }
    };
    StellarCompiler.ExpressionState localState = new StellarCompiler.ExpressionState(
            state.context
          , state.functionResolver
          , variableResolver);
    return apply(localState);
  }
}
