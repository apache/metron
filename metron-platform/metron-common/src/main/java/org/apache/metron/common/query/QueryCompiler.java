/**
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

package org.apache.metron.common.query;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.metron.common.query.generated.PredicateBaseListener;
import org.apache.metron.common.query.generated.PredicateParser;

import java.util.*;
import java.util.function.Predicate;

class QueryCompiler extends PredicateBaseListener {
  private VariableResolver resolver = null;
  private Stack<PredicateToken> tokenStack = new Stack<>();

  public QueryCompiler(VariableResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void enterSingle_rule(org.apache.metron.common.query.generated.PredicateParser.Single_ruleContext ctx) {
    tokenStack.clear();
  }

  @Override
  public void exitSingle_rule(org.apache.metron.common.query.generated.PredicateParser.Single_ruleContext ctx) {
  }

  @Override
  public void exitLogicalExpressionAnd(PredicateParser.LogicalExpressionAndContext ctx) {
    PredicateToken<?> left = popStack();
    PredicateToken<?> right = popStack();
    tokenStack.push(new PredicateToken<>(booleanOp(left, right, (l, r) -> l && r, "&&"), Boolean.class));
  }

  @Override
  public void exitLogicalExpressionOr(PredicateParser.LogicalExpressionOrContext ctx) {
    PredicateToken<?> left = popStack();
    PredicateToken<?> right = popStack();

    tokenStack.push(new PredicateToken<>(booleanOp(left, right, (l, r) -> l || r, "||"), Boolean.class));
  }

  private boolean booleanOp(PredicateToken<?> left, PredicateToken<?> right, BooleanOp op, String opName)
  {
    if(left.getUnderlyingType().equals(right.getUnderlyingType()) && left.getUnderlyingType().equals(Boolean.class)) {
      Boolean l = (Boolean) left.getValue();
      Boolean r = (Boolean) right.getValue();
      if(l == null || r == null) {
        throw new ParseException("Unable to operate on " + left.getValue()  + " " + opName + " " + right.getValue() + ", null value");
      }
      return op.op(l, r);
    }
    else {
      throw new ParseException("Unable to operate on " + left.getValue()  + " " + opName + " " + right.getValue() + ", bad types");
    }
  }


  @Override
  public void exitLogicalConst(PredicateParser.LogicalConstContext ctx) {
    Boolean b = null;
    switch(ctx.getText().toUpperCase()) {
      case "TRUE":
        b = true;
        break;
      case "FALSE":
        b = false;
        break;
      default:
        throw new ParseException("Unable to process " + ctx.getText() + " as a boolean constant");
    }
    tokenStack.push(new PredicateToken<>(b, Boolean.class));
  }

  @Override
  public void exitComparisonExpressionWithOperator(PredicateParser.ComparisonExpressionWithOperatorContext ctx) {
    boolean isEqualsOp = ctx.getChild(1).getText().equals("==");
    PredicateToken<?> left = popStack();
    PredicateToken<?> right = popStack();
    if(left.getUnderlyingType().equals(right.getUnderlyingType())) {
      boolean isEquals = left.equals(right);
      tokenStack.push(new PredicateToken<>(isEqualsOp?isEquals:!isEquals, Boolean.class));
    }
    else {
      throw new ParseException("Unable to compare " + left.getValue() + " " + ctx.getText() + " " + right.getValue());
    }
  }

  public PredicateToken<?> popStack() {
    if(tokenStack.empty()) {
      throw new ParseException("Unable to pop an empty stack");
    }
    return tokenStack.pop();
  }

  @Override
  public void exitLogicalVariable(PredicateParser.LogicalVariableContext ctx) {
    tokenStack.push(new PredicateToken<>(resolver.resolve(ctx.getText()), String.class));
  }


  @Override
  public void exitStringLiteral(PredicateParser.StringLiteralContext ctx) {
    String val = ctx.getText();
    tokenStack.push(new PredicateToken<>(val.substring(1, val.length() - 1), String.class));
  }


  @Override
  public void enterList_entity(PredicateParser.List_entityContext ctx) {
    tokenStack.push(new PredicateToken<>(new FunctionMarker(), FunctionMarker.class));
  }


  @Override
  public void exitList_entity(PredicateParser.List_entityContext ctx) {
    Set<String> inSet = new HashSet<>();
    while(true) {
      PredicateToken<?> token = popStack();
      if(token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      }
      else {
        inSet.add((String)token.getValue());
      }
    }
    tokenStack.push(new PredicateToken<>(inSet, Set.class));
  }


  @Override
  public void enterFunc_args(PredicateParser.Func_argsContext ctx) {
    tokenStack.push(new PredicateToken<>(new FunctionMarker(), FunctionMarker.class));
  }


  @Override
  public void exitFunc_args(PredicateParser.Func_argsContext ctx) {
    LinkedList<String> args = new LinkedList<>();
    while(true) {
      PredicateToken<?> token = popStack();
      if(token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      }
      else {
        args.addFirst((String)token.getValue());
      }
    }
    tokenStack.push(new PredicateToken<>(args, List.class));
  }

  @Override
  public void exitInExpression(PredicateParser.InExpressionContext ctx) {
    PredicateToken<?> left = popStack();
    PredicateToken<?> right = popStack();
    String key = null;
    Set<String> set = null;
    if(left.getUnderlyingType().equals(Set.class)) {
      set = (Set<String>) left.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + left.getValue() + " is not a set");
    }
    if(right.getUnderlyingType().equals(String.class)) {
      key = (String) right.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + right.getValue() + " is not a string");
    }
    tokenStack.push(new PredicateToken<>(set.contains(key), Boolean.class));
  }

  @Override
  public void exitNInExpression(PredicateParser.NInExpressionContext ctx) {
    PredicateToken<?> left = popStack();
    PredicateToken<?> right = popStack();
    String key = null;
    Set<String> set = null;
    if(left.getUnderlyingType().equals(Set.class)) {
      set = (Set<String>) left.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + left.getValue() + " is not a set");
    }
    if(right.getUnderlyingType().equals(String.class)) {
      key = (String) right.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + right.getValue() + " is not a string");
    }
    tokenStack.push(new PredicateToken<>(!set.contains(key), Boolean.class));
  }


  @Override
  public void exitLogicalFunc(PredicateParser.LogicalFuncContext ctx) {
    String funcName = ctx.getChild(0).getText();
    Predicate<List<String>> func;
    try {
      func = LogicalFunctions.valueOf(funcName);
    }
    catch(IllegalArgumentException iae) {
      throw new ParseException("Unable to find logical function " + funcName + ".  Valid functions are "
              + Joiner.on(',').join(LogicalFunctions.values())
      );
    }
    PredicateToken<?> left = popStack();
    List<String> argList = null;
    if(left.getUnderlyingType().equals(List.class)) {
      argList = (List<String>) left.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + left.getValue() + " is not a set");
    }
    Boolean result = func.test(argList);
    tokenStack.push(new PredicateToken<>(result, Boolean.class));
  }

  @Override
  public void exitStringFunc(PredicateParser.StringFuncContext ctx) {
    String funcName = ctx.getChild(0).getText();
    Function<List<String>, String> func;
    try {
      func = StringFunctions.valueOf(funcName);
    }
    catch(IllegalArgumentException iae) {
      throw new ParseException("Unable to find string function " + funcName + ".  Valid functions are "
              + Joiner.on(',').join(StringFunctions.values())
      );
    }
    PredicateToken<?> left = popStack();
    List<String> argList = null;
    if(left.getUnderlyingType().equals(List.class)) {
      argList = (List<String>) left.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + left.getValue() + " is not a set");
    }
    String result = func.apply(argList);
    tokenStack.push(new PredicateToken<>(result, String.class));
  }

  @Override
  public void exitExistsFunc(PredicateParser.ExistsFuncContext ctx) {
    String variable = ctx.getChild(2).getText();
    boolean exists = resolver.resolve(variable) != null;
    tokenStack.push(new PredicateToken<>(exists, Boolean.class));
  }

  @Override
  public void exitNotFunc(PredicateParser.NotFuncContext ctx) {
    PredicateToken<Boolean> arg = (PredicateToken<Boolean>) popStack();
    tokenStack.push(new PredicateToken<>(!arg.getValue(), Boolean.class));
  }

  public boolean getResult() throws ParseException {
    if(tokenStack.empty()) {
      throw new ParseException("Invalid predicate: Empty stack.");
    }
    PredicateToken<?> token = popStack();
    if(token.getUnderlyingType().equals(Boolean.class) && tokenStack.empty()) {
      return (Boolean)token.getValue();
    }
    if(tokenStack.empty()) {
      throw new ParseException("Invalid parse, stack not empty: " + Joiner.on(',').join(tokenStack));
    }
    else {
      throw new ParseException("Invalid parse, found " + token + " but expected boolean");
    }
  }
}
