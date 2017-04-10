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
import org.apache.metron.common.stellar.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.common.stellar.evaluators.NumberLiteralEvaluator;
import org.apache.metron.common.stellar.generated.StellarBaseListener;
import org.apache.metron.common.stellar.generated.StellarParser;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.dsl.FunctionMarker;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.utils.ConversionUtils;

import java.io.Serializable;
import java.util.*;

import static java.lang.String.format;

public class StellarCompiler extends StellarBaseListener {
  private static Token<?> EXPRESSION_REFERENCE = new Token<>(null, Object.class);
  private static Token<?> LAMBDA_VARIABLES = new Token<>(null, Object.class);

  private Expression expression;
  private final ArithmeticEvaluator arithmeticEvaluator;
  private final NumberLiteralEvaluator numberLiteralEvaluator;
  private final ComparisonExpressionWithOperatorEvaluator comparisonExpressionWithOperatorEvaluator;

  public static class ExpressionState {
    Context context;
    FunctionResolver functionResolver;
    VariableResolver variableResolver;
    public ExpressionState(Context context
              , FunctionResolver functionResolver
              , VariableResolver variableResolver
                          ) {
      this.context = context;
      this.variableResolver = variableResolver;
      this.functionResolver = functionResolver;
    }
  }

  public static class Expression implements Serializable {
    final Deque<Token<?>> tokenDeque;
    final Set<String> variablesUsed;
    public Expression(Deque<Token<?>> tokenDeque) {
      this.tokenDeque = tokenDeque;
      this.variablesUsed = new HashSet<>();
    }

    public Deque<Token<?>> getTokenDeque() {
      return tokenDeque;
    }

    public Object apply(ExpressionState state) {
      Deque<Token<?>> instanceDeque = new ArrayDeque<>();
      for(Iterator<Token<?>> it = getTokenDeque().descendingIterator();it.hasNext();) {
        Token<?> token = it.next();
        if(token.getUnderlyingType() == DeferredFunction.class) {
          DeferredFunction func = (DeferredFunction) token.getValue();
          func.apply(instanceDeque, state);
        }
        else {
          instanceDeque.push(token);
        }
      }

      if (instanceDeque.isEmpty()) {
        throw new ParseException("Invalid predicate: Empty stack.");
      }
      Token<?> token = instanceDeque.pop();
      if (instanceDeque.isEmpty()) {
        return token.getValue();
      }
      if (instanceDeque.isEmpty()) {
        throw new ParseException("Invalid parse, stack not empty: " + Joiner.on(',').join(instanceDeque));
      } else {
        throw new ParseException("Invalid parse, found " + token);
      }
    }
  }

  interface DeferredFunction {
    void apply( Deque<Token<?>> tokenDeque
              , ExpressionState state
              );
  }

  public StellarCompiler(
          final ArithmeticEvaluator arithmeticEvaluator,
          final NumberLiteralEvaluator numberLiteralEvaluator,
          final ComparisonExpressionWithOperatorEvaluator comparisonExpressionWithOperatorEvaluator
  ){
    this(new Expression(new ArrayDeque<>()), arithmeticEvaluator, numberLiteralEvaluator, comparisonExpressionWithOperatorEvaluator);
  }

  public StellarCompiler(
          final Expression expression,
          final ArithmeticEvaluator arithmeticEvaluator,
          final NumberLiteralEvaluator numberLiteralEvaluator,
          final ComparisonExpressionWithOperatorEvaluator comparisonExpressionWithOperatorEvaluator
  ){
    this.expression = expression;
    this.arithmeticEvaluator = arithmeticEvaluator;
    this.numberLiteralEvaluator = numberLiteralEvaluator;
    this.comparisonExpressionWithOperatorEvaluator = comparisonExpressionWithOperatorEvaluator;
  }

  @Override
  public void enterTransformation(StellarParser.TransformationContext ctx) {
    expression.tokenDeque.clear();
  }

  private boolean handleIn(final Token<?> left, final Token<?> right) {
    Object key = right.getValue();


    if (left.getValue() != null) {
      if (left.getValue() instanceof String && key instanceof String) {
        return ((String) left.getValue()).contains(key.toString());
      }
      else if (left.getValue() instanceof Collection) {
        return ((Collection) left.getValue()).contains(key);
      }
      else if (left.getValue() instanceof Map) {
        return ((Map) left.getValue()).containsKey(key);
      }
      else {
        if (key == null) {
          return key == left.getValue();
        }
        else {
          return key.equals(left.getValue());
        }
      }
    } else {
      return false;
    }
  }

  @Override
  public void exitNullConst(StellarParser.NullConstContext ctx) {
    expression.tokenDeque.push(new Token<>(null, Object.class));
  }

  @Override
  public void exitArithExpr_plus(StellarParser.ArithExpr_plusContext ctx) {
    expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
      Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
      tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p));
    }, DeferredFunction.class));
  }

  @Override
  public void exitArithExpr_minus(StellarParser.ArithExpr_minusContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), p));
    }, DeferredFunction.class));
  }

  @Override
  public void exitArithExpr_div(StellarParser.ArithExpr_divContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p));
    }, DeferredFunction.class));
  }

  @Override
  public void exitArithExpr_mul(StellarParser.ArithExpr_mulContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), p));
    }, DeferredFunction.class));
  }

  @SuppressWarnings("unchecked")
  private Pair<Token<? extends Number>, Token<? extends Number>> getArithExpressionPair(Deque<Token<?>> tokenDeque) {
    Token<? extends Number> right = (Token<? extends Number>) popDeque(tokenDeque);
    Token<? extends Number> left = (Token<? extends Number>) popDeque(tokenDeque);
    return Pair.of(left, right);
  }

  private void handleConditional() {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      Token<?> elseExpr = popDeque(tokenDeque);
      Token<?> thenExpr = popDeque(tokenDeque);
      Token<?> ifExpr = popDeque(tokenDeque);
      @SuppressWarnings("unchecked") boolean b = ((Token<Boolean>) ifExpr).getValue();
      if (b) {
        tokenDeque.push(thenExpr);
      } else {
        tokenDeque.push(elseExpr);
      }
    }, DeferredFunction.class));
  }

  @Override
  public void exitTernaryFuncWithoutIf(StellarParser.TernaryFuncWithoutIfContext ctx) {
    handleConditional();
  }

  @Override
  public void exitTernaryFuncWithIf(StellarParser.TernaryFuncWithIfContext ctx) {
    handleConditional();
  }

  @Override
  public void exitInExpressionStatement(StellarParser.InExpressionStatementContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(handleIn(left, right), Boolean.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitNInExpressionStatement(StellarParser.NInExpressionStatementContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(!handleIn(left, right), Boolean.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitNotFunc(StellarParser.NotFuncContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<Boolean> arg = (Token<Boolean>) popDeque(tokenDeque);
    tokenDeque.push(new Token<>(!arg.getValue(), Boolean.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitVariable(StellarParser.VariableContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      tokenDeque.push(new Token<>(state.variableResolver.resolve(ctx.getText()), Object.class));
    }, DeferredFunction.class));
    expression.variablesUsed.add(ctx.getText());
  }

  @Override
  public void exitStringLiteral(StellarParser.StringLiteralContext ctx) {
    expression.tokenDeque.push(new Token<>(ctx.getText().substring(1, ctx.getText().length() - 1), String.class));
  }

  @Override
  public void exitIntLiteral(StellarParser.IntLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx));
  }

  @Override
  public void exitDoubleLiteral(StellarParser.DoubleLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx));
  }

  @Override
  public void exitFloatLiteral(StellarParser.FloatLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx));
  }

  @Override
  public void exitLongLiteral(StellarParser.LongLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx));
  }

  @Override
  public void exitLogicalExpressionAnd(StellarParser.LogicalExpressionAndContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(booleanOp(left, right, (l, r) -> l && r, "&&"), Boolean.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitLogicalExpressionOr(StellarParser.LogicalExpressionOrContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);

    tokenDeque.push(new Token<>(booleanOp(left, right, (l, r) -> l || r, "||"), Boolean.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitLogicalConst(StellarParser.LogicalConstContext ctx) {
    Boolean b;
    switch (ctx.getText().toUpperCase()) {
      case "TRUE":
        b = true;
        break;
      case "FALSE":
        b = false;
        break;
      default:
        throw new ParseException("Unable to process " + ctx.getText() + " as a boolean constant");
    }
    expression.tokenDeque.push(new Token<>(b, Boolean.class));
  }

  private boolean booleanOp(final Token<?> left, final Token<?> right, final BooleanOp op, final String opName) {
    Boolean l = ConversionUtils.convert(left.getValue(), Boolean.class);
    Boolean r = ConversionUtils.convert(right.getValue(), Boolean.class);
    if (l == null || r == null) {
      throw new ParseException("Unable to operate on " + left.getValue() + " " + opName + " " + right.getValue() + ", null value");
    }
    return op.op(l, r);
  }


  @Override
  public void enterSingle_lambda_variable(StellarParser.Single_lambda_variableContext ctx) {
    enterLambdaVariables();
  }

  @Override
  public void exitSingle_lambda_variable(StellarParser.Single_lambda_variableContext ctx) {
    exitLambdaVariables();
  }

  @Override
  public void enterLambda_variables(StellarParser.Lambda_variablesContext ctx) {
    enterLambdaVariables();
  }

  @Override
  public void exitLambda_variables(StellarParser.Lambda_variablesContext ctx) {
    exitLambdaVariables();
  }

  @Override
  public void exitLambda_variable(StellarParser.Lambda_variableContext ctx) {
    expression.tokenDeque.push(new Token<>(ctx.getText(), String.class));
  }

  private void enterLambdaVariables() {
    expression.tokenDeque.push(LAMBDA_VARIABLES);
  }

  private void exitLambdaVariables() {
    Token<?> t = expression.tokenDeque.pop();
    LinkedList<String> variables = new LinkedList<>();
    for(; !expression.tokenDeque.isEmpty() && t != LAMBDA_VARIABLES; t = expression.tokenDeque.pop()) {
      variables.addFirst(t.getValue().toString());
    }
    expression.tokenDeque.push(new Token<>(variables, List.class));
  }

  private void enterLambda() {
    expression.tokenDeque.push(EXPRESSION_REFERENCE);
  }

  private void exitLambda(boolean hasArgs) {
    Token<?> t = expression.tokenDeque.pop();
    final Deque<Token<?>> instanceDeque = new ArrayDeque<>();
    for(; !expression.tokenDeque.isEmpty() && t != EXPRESSION_REFERENCE; t = expression.tokenDeque.pop()) {
      instanceDeque.addLast(t);
    }
    final List<String> variables = hasArgs? (List<String>) instanceDeque.removeLast().getValue() :new ArrayList<>();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      LambdaExpression expr = new LambdaExpression(variables, instanceDeque, state);
      tokenDeque.push(new Token<>(expr, Object.class));
    }, DeferredFunction.class) );
  }

  @Override
  public void enterLambda_with_args(StellarParser.Lambda_with_argsContext ctx) {
    enterLambda();
  }

  @Override
  public void exitLambda_with_args(StellarParser.Lambda_with_argsContext ctx) {
    exitLambda(true);
  }

  @Override
  public void enterLambda_without_args(StellarParser.Lambda_without_argsContext ctx) {
    enterLambda();
  }

  @Override
  public void exitLambda_without_args(StellarParser.Lambda_without_argsContext ctx) {
    exitLambda(false);
  }

  @Override
  public void exitTransformationFunc(StellarParser.TransformationFuncContext ctx) {

    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      // resolve and initialize the function
      String functionName = ctx.getChild(0).getText();
      StellarFunction function = resolveFunction(state.functionResolver, functionName);
      initializeFunction(state.context, function, functionName);

      // fetch the args, execute, and push result onto the stack
      List<Object> args = getFunctionArguments(popDeque(tokenDeque));
      Object result = function.apply(args, state.context);
      tokenDeque.push(new Token<>(result, Object.class));
    }, DeferredFunction.class));
  }

  /**
   * Get function arguments.
   * @param token The token containing the function arguments.
   * @return
   */
  @SuppressWarnings("unchecked")
  private List<Object> getFunctionArguments(final Token<?> token) {
    if (token.getUnderlyingType().equals(List.class)) {
      return (List<Object>) token.getValue();

    } else {
      throw new ParseException("Unable to process in clause because " + token.getValue() + " is not a set");
    }
  }

  /**
   * Resolves a function by name.
   * @param funcName
   * @return
   */
  private StellarFunction resolveFunction(FunctionResolver functionResolver, String funcName) {
    try {
      return functionResolver.apply(funcName);

    } catch (Exception e) {
      String valid = Joiner.on(',').join(functionResolver.getFunctions());
      String error = format("Unable to resolve function named '%s'.  Valid functions are %s", funcName, valid);
      throw new ParseException(error, e);
    }
  }

  /**
   * Initialize a Stellar function.
   * @param function The function to initialize.
   * @param functionName The name of the functions.
   */
  private void initializeFunction(Context context, StellarFunction function, String functionName) {
    try {
      if (!function.isInitialized()) {
        function.initialize(context);
      }
    } catch (Throwable t) {
      String error = format("Unable to initialize function '%s'", functionName);
      throw new ParseException(error, t);
    }
  }

  @Override
  public void exitExistsFunc(StellarParser.ExistsFuncContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      String variable = ctx.getChild(2).getText();
      boolean exists = state.variableResolver.resolve(variable) != null;
      tokenDeque.push(new Token<>(exists, Boolean.class));
    }, DeferredFunction.class));
    String variable = ctx.getChild(2).getText();
    expression.variablesUsed.add(variable);
  }

  @Override
  public void enterFunc_args(StellarParser.Func_argsContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }

  @Override
  public void exitFunc_args(StellarParser.Func_argsContext ctx) {
    expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
      LinkedList<Object> args = new LinkedList<>();
      while (true) {
        Token<?> token = popDeque(tokenDeque);
        if (token.getUnderlyingType().equals(FunctionMarker.class)) {
          break;
        } else {
          args.addFirst(token.getValue());
        }
      }
      tokenDeque.push(new Token<>(args, List.class));
    }, DeferredFunction.class));
  }

  @Override
  public void enterMap_entity(StellarParser.Map_entityContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }

  @Override
  public void exitMap_entity(StellarParser.Map_entityContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      HashMap<String, Object> args = new HashMap<>();
      Object value = null;
      for (int i = 0; true; i++) {
        Token<?> token = popDeque(tokenDeque);
        if (token.getUnderlyingType().equals(FunctionMarker.class)) {
          break;
        } else {
          if (i % 2 == 0) {
            value = token.getValue();
          } else {
            args.put(token.getValue() + "", value);
          }
        }
      }
      tokenDeque.push(new Token<>(args, Map.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitList_entity(StellarParser.List_entityContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      LinkedList<Object> args = new LinkedList<>();
      while (true) {
        Token<?> token = popDeque(tokenDeque);
        if (token.getUnderlyingType().equals(FunctionMarker.class)) {
          break;
        } else {
          args.addFirst(token.getValue());
        }
      }
      tokenDeque.push(new Token<>(args, List.class));
    }, DeferredFunction.class));
  }

  @Override
  public void exitComparisonExpressionWithOperator(StellarParser.ComparisonExpressionWithOperatorContext ctx) {
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      StellarParser.Comp_operatorContext op = ctx.comp_operator();
      Token<?> right = popDeque(tokenDeque);
      Token<?> left = popDeque(tokenDeque);

      tokenDeque.push(comparisonExpressionWithOperatorEvaluator.evaluate(left, right, (StellarParser.ComparisonOpContext) op));
    }, DeferredFunction.class));
  }

  @Override
  public void enterList_entity(StellarParser.List_entityContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }

  private Token<?> popDeque(Deque<Token<?>> tokenDeque) {
    if (tokenDeque.isEmpty()) {
      throw new ParseException("Unable to pop an empty stack");
    }
    return tokenDeque.pop();
  }

  public Expression getExpression() {return expression;}

}
