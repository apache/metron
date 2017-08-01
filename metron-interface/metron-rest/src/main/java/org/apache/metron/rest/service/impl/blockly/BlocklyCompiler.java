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
package org.apache.metron.rest.service.impl.blockly;

import org.apache.metron.stellar.common.evaluators.ArithmeticEvaluator;
import org.apache.metron.stellar.common.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.stellar.common.evaluators.NumberLiteralEvaluator;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.FunctionMarker;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.common.StellarCompiler;
import org.apache.metron.stellar.common.generated.StellarParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class BlocklyCompiler extends StellarCompiler {

  private Xml xml = new Xml();
  private Map<String, String[]> functionParamMap = new HashMap<>();
  private Stack<Token<?>> tokenStack;

  public BlocklyCompiler(VariableResolver variableResolver, FunctionResolver functionResolver, Context context, Stack<Token<?>> tokenStack) {
    super(ArithmeticEvaluator.INSTANCE, NumberLiteralEvaluator.INSTANCE, ComparisonExpressionWithOperatorEvaluator.INSTANCE);
    this.tokenStack = tokenStack;
    functionResolver.getFunctionInfo().forEach(stellarFunctionInfo -> functionParamMap.put(stellarFunctionInfo.getName(), stellarFunctionInfo.getParams()));
  }

  @Override
  public void exitNullConst(StellarParser.NullConstContext ctx) {
    Block nullBlock = new Block().withType("logic_null");
    tokenStack.push(new Token<>(nullBlock, Block.class));
  }

  @Override
  public void exitArithExpr_plus(StellarParser.ArithExpr_plusContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block plusBlock = new Block().withType("stellar_arithmetic")
            .addField(new Field().withName("OP").withValue("ADD"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(plusBlock, Block.class));
  }

  @Override
  public void exitArithExpr_minus(StellarParser.ArithExpr_minusContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block minusBlock = new Block().withType("stellar_arithmetic")
            .addField(new Field().withName("OP").withValue("MINUS"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(minusBlock, Block.class));
  }

  @Override
  public void exitArithExpr_div(StellarParser.ArithExpr_divContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block divBlock = new Block().withType("stellar_arithmetic")
            .addField(new Field().withName("OP").withValue("DIVIDE"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(divBlock, Block.class));
  }

  @Override
  public void exitArithExpr_mul(StellarParser.ArithExpr_mulContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block mulBlock = new Block().withType("stellar_arithmetic")
            .addField(new Field().withName("OP").withValue("MULTIPLY"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(mulBlock, Block.class));
  }

  private void handleConditional() {
    Token<?> elseExpr = popStack();
    Token<?> thenExpr = popStack();
    Token<?> ifExpr = popStack();
    Block ternaryBlock = new Block().withType("logic_ternary")
            .addValue(new Value().withName("IF").addBlock((Block) ifExpr.getValue()))
            .addValue(new Value().withName("THEN").addBlock((Block) thenExpr.getValue()))
            .addValue(new Value().withName("ELSE").addBlock((Block) elseExpr.getValue()));
    tokenStack.push(new Token<>(ternaryBlock, Block.class));
  }

  @Override
  public void exitTernaryFuncWithoutIf(StellarParser.TernaryFuncWithoutIfContext ctx) {
    handleConditional();
  }

  @Override
  public void exitTernaryFuncWithIf(StellarParser.TernaryFuncWithIfContext ctx) {
    handleConditional();
  }

  private void handleIn() {
    handleInNotIn(true);
  }

  private void handleNotIn() {
    handleInNotIn(false);
  }

  private void handleInNotIn(boolean in) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block inNotInBlock = new Block().withType("stellar_in")
            .addValue(new Value().withName("INPUT").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("LIST").addBlock((Block) right.getValue()));
    if (in) {
      inNotInBlock.addField(new Field().withName("OP").withValue("in"));
    } else {
      inNotInBlock.addField(new Field().withName("OP").withValue("not in"));
    }
    tokenStack.push(new Token<>(inNotInBlock, Block.class));
  }

  @Override
  public void exitInExpression(StellarParser.InExpressionContext ctx) {
    handleIn();
  }

//  @Override
//  public void exitNInExpression(StellarParser.NInExpressionContext ctx) {
//    handleNotIn();
//  }

  @Override
  public void exitNotFunc(StellarParser.NotFuncContext ctx) {
    Token<?> arg = popStack();
    Block notBlock = new Block().withType("stellar_negate")
            .addValue(new Value().withName("BOOL").addBlock((Block) arg.getValue()));
    tokenStack.push(new Token<>(notBlock, Block.class));
  }

  private Block getAvailableFieldsBlock(String fieldName) {
    return new Block().withType("available_fields").addField(new Field().withName("FIELD_NAME").withValue(fieldName));
  }

  @Override
  public void exitVariable(StellarParser.VariableContext ctx) {
    Block availableFieldsBlock = getAvailableFieldsBlock(ctx.getText());
    tokenStack.push(new Token<>(availableFieldsBlock, Block.class));
  }

  @Override
  public void exitStringLiteral(StellarParser.StringLiteralContext ctx) {
    Block textBlock = new Block().withType("text")
            .addField(new Field().withName("TEXT").withValue(ctx.getText().substring(1, ctx.getText().length() - 1)));
    tokenStack.push(new Token<>(textBlock, Block.class));
  }

  private void handleNumber(String numberText) {
    Block numBlock = new Block().withType("math_number")
            .addField(new Field().withName("NUM").withValue(numberText));
    tokenStack.push(new Token<>(numBlock, Block.class));
  }

  @Override
  public void exitIntLiteral(StellarParser.IntLiteralContext ctx) {
    handleNumber(ctx.getText());
  }

  @Override
  public void exitDoubleLiteral(StellarParser.DoubleLiteralContext ctx) {
    handleNumber(ctx.getText());
  }

  @Override
  public void exitFloatLiteral(StellarParser.FloatLiteralContext ctx) {
    handleNumber(ctx.getText());
  }

  @Override
  public void exitLongLiteral(StellarParser.LongLiteralContext ctx) {
    handleNumber(ctx.getText());
  }

  @Override
  public void exitLogicalExpressionAnd(StellarParser.LogicalExpressionAndContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block andBlock = new Block().withType("logic_operation")
            .addField(new Field().withName("OP").withValue("AND"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(andBlock, Block.class));
  }

  @Override
  public void exitLogicalExpressionOr(StellarParser.LogicalExpressionOrContext ctx) {
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block orBlock = new Block().withType("logic_operation")
            .addField(new Field().withName("OP").withValue("OR"))
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    tokenStack.push(new Token<>(orBlock, Block.class));
  }

  @Override
  public void exitLogicalConst(StellarParser.LogicalConstContext ctx) {
    Block booleanBlock = new Block().withType("logic_boolean")
            .addField(new Field().withName("BOOL").withValue(ctx.getText().toUpperCase()));
    tokenStack.push(new Token<>(booleanBlock, Block.class));
  }

  @Override
  public void exitTransformationFunc(StellarParser.TransformationFuncContext ctx) {
    String functionName = ctx.getChild(0).getText();
    String[] paramNames = functionParamMap.get(functionName);
    Block functionBlock = new Block().withType("stellar_" + functionName);
    List<Object> args = getFunctionArguments(popStack());
    for(int i = 0; i < args.size(); i++) {
      String paramName = paramNames[i].replaceAll("(.*?) .*", "$1").trim().toUpperCase();
      Value value = new Value().addBlock((Block) args.get(i)).withName(paramName);
      functionBlock.addValue(value);
    }
    tokenStack.push(new Token<>(functionBlock, Object.class));
  }

  private List<Object> getFunctionArguments(Token<?> token) {
    if (token.getUnderlyingType().equals(List.class)) {
      return (List<Object>) token.getValue();

    } else {
      throw new ParseException("Unable to process in clause because " + token.getValue() + " is not a set");
    }
  }

  @Override
  public void exitExistsFunc(StellarParser.ExistsFuncContext ctx) {
    Block availableFieldsBlock = getAvailableFieldsBlock(ctx.getChild(2).getText());
    Block existsBlock = new Block().withType("stellar_EXISTS")
            .addValue(new Value().withName("INPUT").addBlock(availableFieldsBlock));
    tokenStack.push(new Token<>(existsBlock, Block.class));
  }

  @Override
  public void enterFunc_args(StellarParser.Func_argsContext ctx) {
    tokenStack.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }

  @Override
  public void exitFunc_args(StellarParser.Func_argsContext ctx) {
    LinkedList<Object> args = new LinkedList<>();
    while (true) {
      Token<?> token = popStack();
      if (token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      } else {
        args.addFirst(token.getValue());
      }
    }
    tokenStack.push(new Token<>(args, List.class));
  }

  @Override
  public void exitMap_entity(StellarParser.Map_entityContext ctx) {
    List<Block> items = new ArrayList<>();
    Block mapBlock = new Block().withType("stellar_map_create");
    Block keyValueBlock = new Block().withType("stellar_key_value");
    for (int i = 0; true; i++) {
      Token<?> token = popStack();
      if (token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      } else {
        if (i % 2 == 0) {
          keyValueBlock.addValue(new Value().withName("VALUE").addBlock((Block) token.getValue()));
        } else {
          keyValueBlock.addValue(new Value().withName("KEY").addBlock((Block) token.getValue()));
          items.add(0, keyValueBlock);
          keyValueBlock = new Block().withType("stellar_key_value");
        }
      }
    }
    mapBlock.withMutation(new Mutation().withItems(items.size()));
    for(int i = 0; i < items.size(); i++) {
      mapBlock.addValue(new Value().withName("ADD" + i).addBlock(items.get(i)));
    }
    tokenStack.push(new Token<>(mapBlock, Block.class));
  }

  @Override
  public void exitList_entity(StellarParser.List_entityContext ctx) {
    List<Block> items = new ArrayList<>();
    Block listBlock = new Block().withType("lists_create_with");
    while (true) {
      Token<?> token = popStack();
      if (token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      } else {
        items.add(0, (Block) token.getValue());
      }
    }
    listBlock.withMutation(new Mutation().withItems(items.size()));
    for(int i = 0; i < items.size(); i++) {
      listBlock.addValue(new Value().withName("ADD" + i).addBlock(items.get(i)));
    }
    tokenStack.push(new Token<>(listBlock, Block.class));
  }

  @Override
  public void exitComparisonExpressionWithOperator(StellarParser.ComparisonExpressionWithOperatorContext ctx) {
    String op = ctx.getChild(1).getText();
    Token<?> right = popStack();
    Token<?> left = popStack();
    Block compareBlock = new Block().withType("logic_compare")
            .addValue(new Value().withName("A").addBlock((Block) left.getValue()))
            .addValue(new Value().withName("B").addBlock((Block) right.getValue()));
    Field operatorField = new Field().withName("OP");
    if (op.equals("==")) {
      operatorField.withValue("EQ");
    } else if (op.equals("!=")) {
      operatorField.withValue("NEQ");
    } else if (op.equals("<")) {
      operatorField.withValue("LT");
    } else if (op.equals(">")) {
      operatorField.withValue("GT");
    } else if (op.equals(">=")) {
      operatorField.withValue("GTE");
    } else {
      operatorField.withValue("LTE");
    }
    compareBlock.addField(operatorField);
    tokenStack.push(new Token<>(compareBlock, Block.class));
  }

  public Xml getXml() {
    xml.addBlock((Block) popStack().getValue());
    return this.xml;
  }

  private Token<?> popStack() {
    if (tokenStack.empty()) {
      throw new ParseException("Unable to pop an empty stack");
    }
    return tokenStack.pop();
  }
}