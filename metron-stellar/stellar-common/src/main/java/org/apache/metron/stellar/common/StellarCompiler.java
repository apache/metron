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
package org.apache.metron.stellar.common;

import com.google.common.base.Joiner;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.stellar.common.evaluators.ArithmeticEvaluator;
import org.apache.metron.stellar.common.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.stellar.common.evaluators.NumberLiteralEvaluator;
import org.apache.metron.stellar.common.generated.StellarBaseListener;
import org.apache.metron.stellar.common.generated.StellarParser;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.FunctionMarker;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;

import static java.lang.String.format;
import org.apache.metron.stellar.dsl.Context.ActivityType;

public class StellarCompiler extends StellarBaseListener {
  private static Token<?> EXPRESSION_REFERENCE = new Token<>(null, Object.class);
  private static Token<?> LAMBDA_VARIABLES = new Token<>(null, Object.class);

  private Expression expression;
  private final ArithmeticEvaluator arithmeticEvaluator;
  private final NumberLiteralEvaluator numberLiteralEvaluator;
  private final ComparisonExpressionWithOperatorEvaluator comparisonExpressionWithOperatorEvaluator;

  public interface ShortCircuitOp {}

  public static class ShortCircuitFrame {}
  public static class BooleanArg implements ShortCircuitOp {}
  public static class IfExpr implements ShortCircuitOp {}
  public static class ThenExpr implements ShortCircuitOp {}
  public static class ElseExpr implements ShortCircuitOp {}
  public static class EndConditional implements ShortCircuitOp {}
  public static class MatchClauseCheckExpr implements ShortCircuitOp {}
  public static class MatchClauseEnd implements ShortCircuitOp {}
  public static class MatchClausesEnd implements ShortCircuitOp {}

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
    final Deque<FrameContext.Context> multiArgumentState;
    final Set<String> variablesUsed;
    public Expression(Deque<Token<?>> tokenDeque) {
      this.tokenDeque = tokenDeque;
      this.variablesUsed = new HashSet<>();
      this.multiArgumentState = new ArrayDeque<>();
    }

    public void clear() {
      tokenDeque.clear();
      variablesUsed.clear();
      multiArgumentState.clear();
    }

    public Deque<Token<?>> getTokenDeque() {
      return tokenDeque;
    }

    /**
     * When treating empty or missing values as false, we need to ensure we ONLY do so in a conditional context.
     * @param tokenValueType
     * @return
     */
    private boolean isConditionalContext(Class<?> tokenValueType) {
      return tokenValueType != null && (
               tokenValueType == BooleanArg.class
            || tokenValueType == IfExpr.class
            || tokenValueType == MatchClauseCheckExpr.class
      );
    }

    /**
     * Determine if a token and value is an empty list in the appropriate conditional context
     * @param token
     * @param value
     * @return
     */
    private boolean isEmptyList(Token<?> token, Object value) {
      if(value != null && isConditionalContext(token.getUnderlyingType())) {
        if (value instanceof Iterable) {
          return Iterables.isEmpty((Iterable) value);
        } else if (value instanceof Map) {
          return ((Map) value).isEmpty();
        }
        else {
          return false;
        }
      }else {
        return false;
      }
    }

    /**
     * Determine if a token is missing in a conditional context.
     * @param token
     * @return
     */
    private boolean isBoolean(Token<?> token, Object value) {
      if(token == null || token.getValue() == null) {
        return false;
      }
      return value == null && isConditionalContext(token.getValue().getClass());
    }

    public Object apply(ExpressionState state) {
      Deque<Token<?>> instanceDeque = new ArrayDeque<>();
      {
        int skipElseCount = 0;
        boolean skipMatchClauses = false;
        Token<?> token = null;
        for (Iterator<Token<?>> it = getTokenDeque().descendingIterator(); it.hasNext(); ) {
          token = it.next();
          //if we've skipped an else previously, then we need to skip the deferred tokens associated with the else.
          if (skipElseCount > 0 && token.getUnderlyingType() == ElseExpr.class) {
            while (it.hasNext()) {
              token = it.next();
              if (token.getUnderlyingType() == EndConditional.class) {
                break;
              }
            }
            // We've completed a single else.
            skipElseCount--;
          }
          if (skipMatchClauses && (token.getUnderlyingType() == MatchClauseEnd.class
              || token.getUnderlyingType() == MatchClauseCheckExpr.class)) {
            while (it.hasNext()) {
              token = it.next();
              if (token.getUnderlyingType() == MatchClausesEnd.class) {
                break;
              }
            }
            skipMatchClauses = false;
          }
          /*
          curr is the current value on the stack.  This is the non-deferred actual evaluation for this expression
          and with the current context.
           */
          Token<?> curr = instanceDeque.peek();
          boolean isFalsey = curr != null &&
                  (isBoolean(token, curr.getValue()) || isEmptyList(token, curr.getValue()));
          if(isFalsey){
            //If we're in a situation where the token is a boolean token and the current value is one of the implicitly falsey scenarios
            //* null or missing variable
            //* empty list
            // then we want to treat it as explicitly false by replacing the current token.
            curr = new Token<>(false, Boolean.class, curr.getMultiArgContext());
            instanceDeque.removeFirst();
            instanceDeque.addFirst(curr);
          }
          if (curr != null && curr.getValue() != null && curr.getValue() instanceof Boolean
              && ShortCircuitOp.class.isAssignableFrom(token.getUnderlyingType())) {
            //if we have a boolean as the current value and the next non-contextual token is a short circuit op
            //then we need to short circuit possibly
            if (token.getUnderlyingType() == BooleanArg.class) {
              if (token.getMultiArgContext() != null
                  && token.getMultiArgContext().getVariety() == FrameContext.BOOLEAN_OR
                  && (Boolean) (curr.getValue())) {
                //short circuit the or
                FrameContext.Context context = curr.getMultiArgContext();
                shortCircuit(it, context);
              } else if (token.getMultiArgContext() != null
                  && token.getMultiArgContext().getVariety() == FrameContext.BOOLEAN_AND
                  && !(Boolean) (curr.getValue())) {
                //short circuit the and
                FrameContext.Context context = curr.getMultiArgContext();
                shortCircuit(it, context);
              }
            } else if (token.getUnderlyingType() == IfExpr.class) {
              //short circuit the if/then/else
              instanceDeque.pop();
              if((Boolean)curr.getValue()) {
                //choose then.  Need to make sure we're keeping track of nesting.
                skipElseCount++;
              } else {
                //choose else
                // Need to count in case we see another if-else, to avoid breaking on wrong else.
                int innerIfCount = 0;
                while (it.hasNext()) {
                  Token<?> t = it.next();
                  if (t.getUnderlyingType() == IfExpr.class) {
                    innerIfCount++;
                  } else if (t.getUnderlyingType() == ElseExpr.class) {
                    if (innerIfCount == 0) {
                      break;
                    } else {
                      innerIfCount--;
                    }
                  }
                }
              }
            } else if (token.getUnderlyingType() == MatchClauseCheckExpr.class) {
              instanceDeque.pop();
              if ((Boolean) curr.getValue()) {
                //skip everything else after lambda
                skipMatchClauses = true;
              } else {
                while (it.hasNext()) {
                  Token<?> t = it.next();
                  if (t.getUnderlyingType() == MatchClauseEnd.class) {
                    break;
                  }
                }
              }
            }
          }
          if (token.getUnderlyingType() == DeferredFunction.class) {
            DeferredFunction func = (DeferredFunction) token.getValue();
            func.apply(instanceDeque, state);
          }
          else if(token.getUnderlyingType() != ShortCircuitFrame.class
               && !ShortCircuitOp.class.isAssignableFrom(token.getUnderlyingType())
                  ) {
            instanceDeque.push(token);
          }

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



    public void shortCircuit(Iterator<Token<?>> it, FrameContext.Context context) {
      while (it.hasNext()) {
        Token<?> token = it.next();
        if (token.getUnderlyingType() == ShortCircuitFrame.class && token.getMultiArgContext() == context) {
          break;
        }
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
    expression.clear();
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
    expression.tokenDeque.push(new Token<>(null, Object.class, getArgContext()));
  }

  @Override
  public void exitNaNArith(StellarParser.NaNArithContext ctx) {
    expression.tokenDeque.push(new Token<>(Double.NaN, Double.class, getArgContext()));
  }

  @Override
  public void exitArithExpr_plus(StellarParser.ArithExpr_plusContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
      Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
      tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(context), p));
    }, DeferredFunction.class, context));
  }

  @Override
  public void exitArithExpr_minus(StellarParser.ArithExpr_minusContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(context), p));
    }, DeferredFunction.class, context));
  }

  @Override
  public void exitArithExpr_div(StellarParser.ArithExpr_divContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(context), p));
    }, DeferredFunction.class, context));
  }

  @Override
  public void exitArithExpr_mul(StellarParser.ArithExpr_mulContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Pair<Token<? extends Number>, Token<? extends Number>> p = getArithExpressionPair(tokenDeque);
    tokenDeque.push(arithmeticEvaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(context), p));
    }, DeferredFunction.class, context));
  }

  @SuppressWarnings("unchecked")
  private Pair<Token<? extends Number>, Token<? extends Number>> getArithExpressionPair(Deque<Token<?>> tokenDeque) {
    Token<? extends Number> right = (Token<? extends Number>) popDeque(tokenDeque);
    Token<? extends Number> left = (Token<? extends Number>) popDeque(tokenDeque);
    return Pair.of(left, right);
  }

  @Override
  public void exitIf_expr(StellarParser.If_exprContext ctx) {
    expression.tokenDeque.push(new Token<>(new IfExpr(), IfExpr.class, getArgContext()));
  }

  @Override
  public void enterThen_expr(StellarParser.Then_exprContext ctx) {
    expression.tokenDeque.push(new Token<>(new ThenExpr(), ThenExpr.class, getArgContext()));
  }

  @Override
  public void enterElse_expr(StellarParser.Else_exprContext ctx) {
    expression.tokenDeque.push(new Token<>(new ElseExpr(), ElseExpr.class, getArgContext()));
  }

  @Override
  public void exitElse_expr(StellarParser.Else_exprContext ctx) {
    expression.tokenDeque.push(new Token<>(new EndConditional(), EndConditional.class, getArgContext()));
  }

  @Override
  public void exitInExpressionStatement(StellarParser.InExpressionStatementContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(handleIn(left, right), Boolean.class, context));
    }, DeferredFunction.class, context));
  }


  @Override
  public void exitNInExpressionStatement(StellarParser.NInExpressionStatementContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(!handleIn(left, right), Boolean.class, context));
    }, DeferredFunction.class, context));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void exitNotFunc(StellarParser.NotFuncContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
      Token<Boolean> arg = (Token<Boolean>) popDeque(tokenDeque);
      Boolean v = Optional.ofNullable(ConversionUtils.convert(arg.getValue(), Boolean.class)).orElse(false);
      tokenDeque.push(new Token<>(!v, Boolean.class, context));
    }, DeferredFunction.class, context));
  }


  @Override
  public void exitVariable(StellarParser.VariableContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      String varName = ctx.getText();
      if(state.context.getActivityType().equals(ActivityType.PARSE_ACTIVITY) && !state.variableResolver.exists(varName)) {
        // when parsing, missing variables are an error!
        throw new ParseException(String.format("variable: %s is not defined",varName));
      }
      Object resolved = state.variableResolver.resolve(varName);
      tokenDeque.push(new Token<>(resolved, Object.class, context));
    }, DeferredFunction.class, context));
    expression.variablesUsed.add(ctx.getText());
  }

  @Override
  public void exitStringLiteral(StellarParser.StringLiteralContext ctx) {
    String rawToken = ctx.getText();
    String literal = StringEscapeUtils.UNESCAPE_JSON.translate(rawToken);
    expression.tokenDeque.push(new Token<>(literal.substring(1, literal.length()-1), String.class, getArgContext()));
  }

  @Override
  public void exitIntLiteral(StellarParser.IntLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx, getArgContext()));
  }

  @Override
  public void exitDoubleLiteral(StellarParser.DoubleLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx, getArgContext()));
  }

  @Override
  public void exitFloatLiteral(StellarParser.FloatLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx, getArgContext()));
  }

  @Override
  public void exitLongLiteral(StellarParser.LongLiteralContext ctx) {
    expression.tokenDeque.push(numberLiteralEvaluator.evaluate(ctx, getArgContext()));
  }

  @Override
  public void enterB_expr(StellarParser.B_exprContext ctx) {
    //Enter is not guaranteed to be called by Antlr for logical labels, so we need to
    //emulate it like this.  See  https://github.com/antlr/antlr4/issues/802
    if(ctx.getParent() instanceof StellarParser.LogicalExpressionOrContext) {
      expression.multiArgumentState.push(FrameContext.BOOLEAN_OR.create());
    }
    else if(ctx.getParent() instanceof StellarParser.LogicalExpressionAndContext) {
      expression.multiArgumentState.push(FrameContext.BOOLEAN_AND.create());
    }
  }

  @Override
  public void exitB_expr(StellarParser.B_exprContext ctx) {
    if(ctx.getParent() instanceof StellarParser.LogicalExpressionOrContext
    || ctx.getParent() instanceof StellarParser.LogicalExpressionAndContext
      )
    {
      //we want to know when the argument to the boolean expression is complete
      expression.tokenDeque.push(new Token<>(new BooleanArg(), BooleanArg.class, getArgContext()));
    }
  }

  @Override
  public void exitLogicalExpressionAnd(StellarParser.LogicalExpressionAndContext ctx) {
    final FrameContext.Context context = getArgContext();
    popArgContext();
    final FrameContext.Context parentContext = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);
    tokenDeque.push(new Token<>(booleanOp(left, right, (l, r) -> l && r, "&&"), Boolean.class, parentContext));
    }, DeferredFunction.class, context));
    expression.tokenDeque.push(new Token<>(new ShortCircuitFrame(), ShortCircuitFrame.class, context));
  }

  @Override
  public void exitLogicalExpressionOr(StellarParser.LogicalExpressionOrContext ctx) {
    final FrameContext.Context context = getArgContext();
    popArgContext();
    final FrameContext.Context parentContext = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
    Token<?> left = popDeque(tokenDeque);
    Token<?> right = popDeque(tokenDeque);

    tokenDeque.push(new Token<>(booleanOp(left, right, (l, r) -> l || r, "||"), Boolean.class, parentContext));
    }, DeferredFunction.class, context));
    expression.tokenDeque.push(new Token<>(new ShortCircuitFrame(), ShortCircuitFrame.class, context));
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
    expression.tokenDeque.push(new Token<>(b, Boolean.class, getArgContext()));
  }

  private boolean booleanOp(final Token<?> left, final Token<?> right, final BooleanOp op, final String opName) {
    Boolean l = Optional.ofNullable(ConversionUtils.convert(left.getValue(), Boolean.class)).orElse(false);
    Boolean r = Optional.ofNullable(ConversionUtils.convert(right.getValue(), Boolean.class)).orElse(false);
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
    expression.tokenDeque.push(new Token<>(ctx.getText(), String.class, getArgContext()));
  }

  private void enterLambdaVariables() {
    expression.tokenDeque.push(LAMBDA_VARIABLES);
  }

  @SuppressWarnings("ReferenceEquality")
  private void exitLambdaVariables() {
    Token<?> t = expression.tokenDeque.pop();
    LinkedList<String> variables = new LinkedList<>();
    for (; !expression.tokenDeque.isEmpty() && t != LAMBDA_VARIABLES; t = expression.tokenDeque.pop()) {
      variables.addFirst(t.getValue().toString());
    }
    expression.tokenDeque.push(new Token<>(variables, List.class, getArgContext()));
  }

  private void enterLambda() {
    expression.tokenDeque.push(EXPRESSION_REFERENCE);
  }

  @SuppressWarnings({"unchecked","ReferenceEquality"})
  private void exitLambda(boolean hasArgs) {
    final FrameContext.Context context = getArgContext();
    Token<?> t = expression.tokenDeque.pop();
    final Deque<Token<?>> instanceDeque = new ArrayDeque<>();
    for(; !expression.tokenDeque.isEmpty() && t != EXPRESSION_REFERENCE; t = expression.tokenDeque.pop()) {
      instanceDeque.addLast(t);
    }
    final List<String> variables = hasArgs ? (List<String>) instanceDeque.removeLast().getValue() :new ArrayList<>();
    expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
      LambdaExpression expr = new LambdaExpression(variables, instanceDeque, state);
      tokenDeque.push(new Token<>(expr, Object.class, context));
    }, DeferredFunction.class, context));
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
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      // resolve and initialize the function
      String functionName = ctx.getChild(0).getText();
      StellarFunction function = resolveFunction(state.functionResolver, functionName);
      initializeFunction(state.context, function, functionName);

      // fetch the args, execute, and push result onto the stack
      List<Object> args = getFunctionArguments(popDeque(tokenDeque));
      Object result = function.apply(args, state.context);
      tokenDeque.push(new Token<>(result, Object.class, context));
    }, DeferredFunction.class, context));
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
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      String variable = ctx.getChild(2).getText();
      boolean exists = state.variableResolver.resolve(variable) != null;
      tokenDeque.push(new Token<>(exists, Boolean.class, context));
    }, DeferredFunction.class, context));
    String variable = ctx.getChild(2).getText();
    expression.variablesUsed.add(variable);
  }

  @Override
  public void enterFunc_args(StellarParser.Func_argsContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class, getArgContext()));
  }

  @Override
  public void exitFunc_args(StellarParser.Func_argsContext ctx) {
    final FrameContext.Context context = getArgContext();
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
      tokenDeque.push(new Token<>(args, List.class, context));
    }, DeferredFunction.class, context));
  }

  @Override
  public void enterMap_entity(StellarParser.Map_entityContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class, getArgContext()));
  }

  @Override
  public void exitMap_entity(StellarParser.Map_entityContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      HashMap<Object, Object> args = new HashMap<>();
      Object value = null;
      for (int i = 0; true; i++) {
        Token<?> token = popDeque(tokenDeque);
        if (token.getUnderlyingType().equals(FunctionMarker.class)) {
          break;
        } else {
          if (i % 2 == 0) {
            value = token.getValue();
          } else {
            args.put(token.getValue(), value);
          }
        }
      }
      tokenDeque.push(new Token<>(args, Map.class, context));
    }, DeferredFunction.class, context));
  }

  @Override
  public void exitList_entity(StellarParser.List_entityContext ctx) {
    final FrameContext.Context context = getArgContext();
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
      tokenDeque.push(new Token<>(args, List.class, context));
    }, DeferredFunction.class, context));
  }

  @Override
  public void exitDefault(StellarParser.DefaultContext ctx) {
    expression.tokenDeque.push(new Token<>(true, Boolean.class, getArgContext()));
  }

  @Override
  public void exitMatchClauseCheckExpr(StellarParser.MatchClauseCheckExprContext ctx) {
    final FrameContext.Context context = getArgContext();
    // if we are validating, and we have a single variable then we will get
    // a null and we need to protect against that
    if(ctx.getStart() == ctx.getStop()) {
      expression.tokenDeque.push(new Token<>((tokenDeque, state) -> {
          if (tokenDeque.size() == 1 && (tokenDeque.peek().getValue() == null
                  || tokenDeque.peek().getUnderlyingType() == Boolean.class)) {
            tokenDeque.pop();
            tokenDeque.add(new Token<>(false, Boolean.class, getArgContext()));
          }
      }, DeferredFunction.class, context));
    }
    expression.tokenDeque.push(new Token<>(new MatchClauseCheckExpr(), MatchClauseCheckExpr.class, getArgContext()));
  }

  @Override
  public void exitMatchClauseAction(StellarParser.MatchClauseActionContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      Token<?> token = popDeque(tokenDeque);
      Object value = token.getValue();
      if (value != null && LambdaExpression.class.isAssignableFrom(value.getClass())) {
        LambdaExpression expr = (LambdaExpression) value;
        // at this time we don't support lambdas with arguments
        // there is no context for it as such here
        // it is possible that we add match variables, and use those
        // as the context, but that could also be done with plain variables
        // so it remains to be determined
        Object result = expr.apply(new ArrayList<>());
        tokenDeque.push(new Token<>(result, Object.class, context));
      } else {
        tokenDeque.push(new Token<>(value, Object.class, context));
      }

    }, DeferredFunction.class, context));
  }

  @Override
  public void exitMatch_clause(StellarParser.Match_clauseContext ctx) {
    expression.tokenDeque.push(new Token<>(new MatchClauseEnd(), MatchClauseEnd.class, getArgContext()));
  }

  @Override
  public void exitMatchClauses(StellarParser.MatchClausesContext ctx) {
    expression.tokenDeque.push(new Token<>(new MatchClausesEnd(),MatchClausesEnd.class, getArgContext()));
  }

  @Override
  public void exitComparisonExpressionWithOperator(StellarParser.ComparisonExpressionWithOperatorContext ctx) {
    final FrameContext.Context context = getArgContext();
    expression.tokenDeque.push(new Token<>( (tokenDeque, state) -> {
      StellarParser.Comp_operatorContext op = ctx.comp_operator();
      Token<?> right = popDeque(tokenDeque);
      Token<?> left = popDeque(tokenDeque);

      tokenDeque.push(comparisonExpressionWithOperatorEvaluator.evaluate(left, right, (StellarParser.ComparisonOpContext) op, context));
    }, DeferredFunction.class, context));
  }

  @Override
  public void enterList_entity(StellarParser.List_entityContext ctx) {
    expression.tokenDeque.push(new Token<>(new FunctionMarker(), FunctionMarker.class, getArgContext()));
  }

  private void popArgContext() {
    if(!expression.multiArgumentState.isEmpty()) {
      expression.multiArgumentState.pop();
    }
  }

  private FrameContext.Context getArgContext() {
    return expression.multiArgumentState.isEmpty() ? null : expression.multiArgumentState.peek();
  }

  private Token<?> popDeque(Deque<Token<?>> tokenDeque) {
    if (tokenDeque.isEmpty()) {
      throw new ParseException("Unable to pop an empty stack");
    }
    return tokenDeque.pop();
  }

  public Expression getExpression() {
    return expression;
  }

}
