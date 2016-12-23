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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.metron.common.dsl.*;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.stellar.evaluators.ArithmeticEvaluator;
import org.apache.metron.common.stellar.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.common.stellar.evaluators.NumberLiteralEvaluator;
import org.apache.metron.common.stellar.generated.StellarBaseListener;
import org.apache.metron.common.stellar.generated.StellarLexer;
import org.apache.metron.common.stellar.generated.StellarParser;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class BaseStellarProcessor<T> {
  Class<T> clazz;
  public BaseStellarProcessor(Class<T> clazz) {
    this.clazz = clazz;
  }

  public Set<String> variablesUsed(String rule) {
    if (rule == null || isEmpty(rule.trim())) {
      return null;
    }
    ANTLRInputStream input = new ANTLRInputStream(rule);
    StellarLexer lexer = new StellarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    StellarParser parser = new StellarParser(tokens);
    final Set<String> ret = new HashSet<>();
    parser.addParseListener(new StellarBaseListener() {
      @Override
      public void exitVariable(StellarParser.VariableContext ctx) {
        ret.add(ctx.getText());
      }
      @Override
      public void exitExistsFunc(StellarParser.ExistsFuncContext ctx) {
        String variable = ctx.getChild(2).getText();
        ret.add(variable);
      }
    });
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    parser.transformation();
    return ret;
  }

  public T parse(String rule, VariableResolver variableResolver, FunctionResolver functionResolver, Context context) {
    if (rule == null || isEmpty(rule.trim())) {
      return null;
    }

    ANTLRInputStream input = new ANTLRInputStream(rule);
    StellarLexer lexer = new StellarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    StellarParser parser = new StellarParser(tokens);

    StellarCompiler treeBuilder = new StellarCompiler(variableResolver, functionResolver, context, new Stack<>(),
        ArithmeticEvaluator.INSTANCE,
        NumberLiteralEvaluator.INSTANCE,
        ComparisonExpressionWithOperatorEvaluator.INSTANCE
    );
    parser.addParseListener(treeBuilder);
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    parser.transformation();
    return clazz.cast(treeBuilder.getResult());
  }

  /**
   * This method determines if a given rule is syntactically valid or not. If the given rule is valid then true
   * will be returned otherwise a {@link ParseException} is thrown. If it is desired that to return a boolean
   * whether the rule is valid or not see {@link this#validate(String, boolean, Context)}.
   *
   * @param rule The rule to validate.
   * @return If the given rule is syntactically valid then true otherwise an exception is thrown.
   * @throws ParseException If the rule is invalid an exception of this type is thrown.
   */
  public boolean validate(String rule) throws ParseException {
    return validate(rule, true, Context.EMPTY_CONTEXT());
  }

  public boolean validate(String rule, Context context) throws ParseException {
    return validate(rule, true, context);
  }

  /**
   * Here it is not desirable to add our custom listener. It is not the intent to evaluate the rule.
   * The rule is only meant to be validated. Validate in this instance means check whether or not the
   * rule is syntactically valid. For example, it will not check for incorrect variable or function uses.
   * It will check for rules that do not conform to the grammar.
   *
   * @param rule The Stellar transformation to validate.
   * @param throwException If true an invalid Stellar transformation will throw a {@link ParseException} otherwise a boolean will be returned.
   * @param context The Stellar context to be used when validating the Stellar transformation.
   * @return If {@code throwException} is true and {@code rule} is invalid a {@link ParseException} is thrown. If
   *  {@code throwException} is false and {@code rule} is invalid then false is returned. Otherwise true if {@code rule} is valid,
   *  false if {@code rule} is invalid.
   * @throws ParseException Thrown if {@code rule} is invalid and {@code throwException} is true.
   */
  public boolean validate(String rule, boolean throwException, Context context) throws ParseException {
    if (rule == null || isEmpty(rule.trim())) {
      return true;
    }

    ANTLRInputStream input = new ANTLRInputStream(rule);
    StellarLexer lexer = new StellarLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());

    TokenStream tokens = new CommonTokenStream(lexer);
    StellarParser parser = new StellarParser(tokens);

    parser.removeErrorListeners();
    parser.setBuildParseTree(false);
    parser.addErrorListener(new ErrorListener());

    try {
      parser.transformation();
    } catch(Throwable t) {
      if(throwException) {
        throw new ParseException("Unable to parse " + rule + ": " + t.getMessage(), t);
      } else {
        return false;
      }
    }

    return true;
  }
}
