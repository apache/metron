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

package org.apache.metron.common.transformation;

import com.google.common.base.Joiner;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.dsl.TransformationFunctions;
import org.apache.metron.common.dsl.FunctionMarker;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.transformation.generated.TransformationBaseListener;
import org.apache.metron.common.transformation.generated.TransformationParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

public class TransformationCompiler extends TransformationBaseListener {
  private VariableResolver resolver = null;
  private Stack<Token> tokenStack = new Stack<>();
  public TransformationCompiler(VariableResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void enterTransformation(TransformationParser.TransformationContext ctx) {
    tokenStack.clear();
  }


  @Override
  public void exitVariable(TransformationParser.VariableContext ctx) {
    tokenStack.push(new Token<>(resolver.resolve(ctx.getText()), Object.class));
  }

  @Override
  public void exitStringLiteral(TransformationParser.StringLiteralContext ctx) {
    tokenStack.push(new Token<>(ctx.getText().substring(1, ctx.getText().length() - 1), String.class));
  }


  @Override
  public void exitIntegerLiteral(TransformationParser.IntegerLiteralContext ctx) {
    tokenStack.push(new Token<>(Integer.parseInt(ctx.getText()), Integer.class));
  }


  @Override
  public void exitDoubleLiteral(TransformationParser.DoubleLiteralContext ctx) {
    tokenStack.push(new Token<>(Double.parseDouble(ctx.getText()), Double.class));
  }


  @Override
  public void exitTransformationFunc(TransformationParser.TransformationFuncContext ctx) {
    String funcName = ctx.getChild(0).getText();
    Function<List<Object>, Object> func;
    try {
      func = TransformationFunctions.valueOf(funcName);
    }
    catch(IllegalArgumentException iae) {
      throw new ParseException("Unable to find string function " + funcName + ".  Valid functions are "
              + Joiner.on(',').join(TransformationFunctions.values())
      );
    }
    Token<?> left = popStack();
    List<Object> argList = null;
    if(left.getUnderlyingType().equals(List.class)) {
      argList = (List<Object>) left.getValue();
    }
    else {
      throw new ParseException("Unable to process in clause because " + left.getValue() + " is not a set");
    }
    Object result = func.apply(argList);
    tokenStack.push(new Token<>(result, Object.class));
  }


  @Override
  public void enterFunc_args(TransformationParser.Func_argsContext ctx) {
    tokenStack.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }


  @Override
  public void exitFunc_args(TransformationParser.Func_argsContext ctx) {
    LinkedList<Object> args = new LinkedList<>();
    while(true) {
      Token<?> token = popStack();
      if(token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      }
      else {
        args.addFirst(token.getValue());
      }
    }
    tokenStack.push(new Token<>(args, List.class));
  }


  @Override
  public void exitList_entity(TransformationParser.List_entityContext ctx) {
    LinkedList<Object> args = new LinkedList<>();
    while(true) {
      Token<?> token = popStack();
      if(token.getUnderlyingType().equals(FunctionMarker.class)) {
        break;
      }
      else {
        args.addFirst(token.getValue());
      }
    }
    tokenStack.push(new Token<>(args, List.class));
  }

  @Override
  public void enterList_entity(TransformationParser.List_entityContext ctx) {
    tokenStack.push(new Token<>(new FunctionMarker(), FunctionMarker.class));
  }

  public Token<?> popStack() {
    if(tokenStack.empty()) {
      throw new ParseException("Unable to pop an empty stack");
    }
    return tokenStack.pop();
  }

  public Object getResult() throws ParseException {
    if(tokenStack.empty()) {
      throw new ParseException("Invalid predicate: Empty stack.");
    }
    Token<?> token = popStack();
    if(tokenStack.empty()) {
      return token.getValue();
    }
    if(tokenStack.empty()) {
      throw new ParseException("Invalid parse, stack not empty: " + Joiner.on(',').join(tokenStack));
    }
    else {
      throw new ParseException("Invalid parse, found " + token);
    }
  }
}
