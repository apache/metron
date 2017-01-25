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

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ErrorListener;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.stellar.evaluators.ArithmeticEvaluator;
import org.apache.metron.common.stellar.evaluators.ComparisonExpressionWithOperatorEvaluator;
import org.apache.metron.common.stellar.evaluators.NumberLiteralEvaluator;
import org.apache.metron.common.stellar.generated.StellarBaseListener;
import org.apache.metron.common.stellar.generated.StellarLexer;
import org.apache.metron.common.stellar.generated.StellarParser;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * The base implementation of a Stellar processor. This is used to evaluate Stellar expressions.
 *
 * @param <T> The type that the processor expects to return after processing a Stellar expression.
 * @see StellarProcessor
 * @see StellarPredicateProcessor
 */
public class BaseStellarProcessor<T> {
  /**
   * The class containing the type that the Stellar expression being processed will evaluate to.
   */
  private Class<T> clazz;

  /**
   * @param clazz The class containing the type that the Stellar expression being processed will evaluate to.
   */
  BaseStellarProcessor(final Class<T> clazz) {
    this.clazz = clazz;
  }

  /**
   * Parses the given rule and returns a set of variables that are used in the given Stellar expression, {@code rule}.
   *
   * @param rule The Stellar expression to find out what variables are used.
   * @return A set of variables used in the given Stellar expression.
   */
  public Set<String> variablesUsed(final String rule) {
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
      public void exitVariable(final StellarParser.VariableContext ctx) {
        ret.add(ctx.getText());
      }
      @Override
      public void exitExistsFunc(final StellarParser.ExistsFuncContext ctx) {
        String variable = ctx.getChild(2).getText();
        ret.add(variable);
      }
    });
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    parser.transformation();
    return ret;
  }

  /**
   * Parses and evaluates the given Stellar expression, {@code rule}.
   * @param rule The Stellar expression to parse and evaluate.
   * @param variableResolver The {@link VariableResolver} to determine values of variables used in the Stellar expression, {@code rule}.
   * @param functionResolver The {@link FunctionResolver} to determine values of functions used in the Stellar expression, {@code rule}.
   * @param context The context used during validation.
   * @return The value of the evaluated Stellar expression, {@code rule}.
   */
  public T parse(final String rule, final VariableResolver variableResolver, final FunctionResolver functionResolver, final Context context) {
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
   * This method determines if a given rule is valid or not. If the given rule is valid then true
   * will be returned otherwise a {@link ParseException} is thrown. If it is desired to return a boolean
   * whether the rule is valid or not, use the {@link #validate(String, boolean, Context) validate} method. It is important
   * to note that all variables will resolve to 'null.'
   *
   * @param rule The rule to validate.
   * @return If the given rule is valid then true otherwise an exception is thrown.
   * @throws ParseException If the rule is invalid an exception of this type is thrown.
   */
  public boolean validate(final String rule) throws ParseException {
    return validate(rule, true, Context.EMPTY_CONTEXT());
  }

  /**
   * Validates a given Stellar expression based on given context.
   * @param rule The Stellar expression to validate.
   * @param context The context used to validate the Stellar expression.
   * @return If valid Stellar expression true, otherwise an exception will be thrown.
   * @throws ParseException The exception containing the information as to why the expression is not valid.
   */
  public boolean validate(final String rule, final Context context) throws ParseException {
    return validate(rule, true, context);
  }

  /**
   * Here it is not desirable to add our custom listener. It is not the intent to evaluate the rule.
   * The rule is only meant to be validated. Validate in this instance means check whether or not the
   * rule is syntactically valid and whether the functions used exist. For example, it will not check
   * for variables that are not defined. Currently all variables resolve to 'null.' This is mainly to
   * make sure things function as expected when values are null.
   *
   * @param rule The Stellar transformation to validate.
   * @param throwException If true an invalid Stellar transformation will throw a {@link ParseException} otherwise a boolean will be returned.
   * @param context The Stellar context to be used when validating the Stellar transformation.
   * @return If {@code throwException} is true and {@code rule} is invalid a {@link ParseException} is thrown. If
   *  {@code throwException} is false and {@code rule} is invalid then false is returned. Otherwise true if {@code rule} is valid,
   *  false if {@code rule} is invalid.
   * @throws ParseException Thrown if {@code rule} is invalid and {@code throwException} is true.
   */
  public boolean validate(final String rule, final boolean throwException, final Context context) throws ParseException {
    if (rule == null || isEmpty(rule.trim())) {
      return true;
    }

    try {
      parse(rule, x -> null, StellarFunctions.FUNCTION_RESOLVER(), context);
    } catch (Throwable t) {
      if (throwException) {
        throw new ParseException("Unable to parse " + rule + ": " + t.getMessage(), t);
      } else {
        return false;
      }
    }

    return true;
  }
}
