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

package org.apache.metron.stellar.common.utils;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.stellar.common.StellarPredicateProcessor;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.dsl.VariableResolver;

/**
 * Utilities for executing and validating Stellar expressions.
 */
public class StellarProcessorUtils {

  /**
   * Execute and validate a Stellar expression.
   *
   * <p>This is intended for use while unit testing Stellar expressions.  This ensures that the expression
   * validates successfully and produces a result that can be serialized correctly.
   *
   * @param expression The expression to execute.
   * @param varResolver The variable resolver to use
   * @param context The execution context.
   * @return The result of executing the expression.
   */
  public static Object run(String expression, VariableResolver varResolver, Context context) {

    validate(expression, context);
    Object result = execute(expression, varResolver, context);
    ensureKryoSerializable(result, expression);
    ensureJavaSerializable(result, expression);

    return result;
  }

  /**
   * Execute and validate a Stellar expression.
   *
   * <p>This is intended for use while unit testing Stellar expressions.  This ensures that the expression
   * validates successfully and produces a result that can be serialized correctly.
   *
   * @param expression The expression to execute.
   * @param variables The variables to expose to the expression.
   * @param context The execution context.
   * @return The result of executing the expression.
   */
  public static Object run(String expression, Map<String, Object> variables, Context context) {
    VariableResolver varResolver = new DefaultVariableResolver(
            x -> {
              if(x.equals(MapVariableResolver.ALL_FIELDS)) {
                return variables;
              }
              return variables.get(x);
            }
            ,x-> x.equals(MapVariableResolver.ALL_FIELDS) || variables.containsKey(x)
    );
    return run(expression, varResolver, context);
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expression The expression to execute.
   * @param variableResolver Variable Resolver to use
   * @param context The execution context.
   * @return The result of executing the expression.
   */
  private static Object execute(String expression, VariableResolver variableResolver, Context context) {

    StellarProcessor processor = new StellarProcessor();

    Object result = processor.parse(
            expression,
            variableResolver,
            StellarFunctions.FUNCTION_RESOLVER(),
            context);

    return result;
  }

  /**
   * Ensure that a value can be serialized and deserialized using Kryo.
   *
   * <p>When a Stellar function is used in a Storm topology there are cases when the result
   * needs to be serializable, like when using the Profiler.  Storm can use either Kryo or
   * basic Java serialization.  It is highly recommended that all Stellar functions return a
   * result that is Kryo serializable to allow for the broadest possible use of the function.
   *
   * @param value The value to validate.
   */
  private static void ensureKryoSerializable(Object value, String expression) {

    String msg = String.format("Expression result is not Kryo serializable. It is highly recommended for all " +
            "functions to return a result that is Kryo serializable to allow for their broadest possible use. " +
            "expr=%s, value=%s", expression, value);

    byte[] raw = SerDeUtils.toBytes(value);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);
    if((value == null && actual != null) || (value != null && !value.equals(actual))) {
      throw new AssertionError(msg);
    }
  }

  /**
   * Ensure a value can be serialized and deserialized using Java serialization.
   *
   * <p>When a Stellar function is used in a Storm topology there are cases when the result
   * needs to be serializable, like when using the Profiler.  Storm can use either Kryo or
   * basic Java serialization.  It is highly recommended that all Stellar functions return a
   * result that is Java serializable to allow for the broadest possible use of the function.
   *
   * @param value The value to serialize
   */
  private static void ensureJavaSerializable(Object value, String expression) {

    String msg = String.format("Expression result is not Java serializable. It is highly recommended for all " +
            "functions to return a result that is Java serializable to allow for their broadest possible use. " +
            "expr=%s, value=%s", expression, value);

    try {
      // serialize using java
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bytes);
      out.writeObject(value);

      // the serialized bits
      byte[] raw = bytes.toByteArray();
      if(!(raw.length > 0)) {
        throw new AssertionError("Serialized byte length not greater than 0");
      }

      // deserialize using java
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw));
      Object actual = in.readObject();

      // ensure that the round-trip was successful
      if((value == null && actual != null) || (value != null && !value.equals(actual))) {
        throw new AssertionError(msg);
      }

    } catch(IOException | ClassNotFoundException e) {

      String error = String.format("Expression result is not Java serializable. It is highly recommended for all " +
              "functions to return a result that is Java serializable to allow for their broadest possible use. " +
              "expr=%s, value=%s, error=%s", expression, value, ExceptionUtils.getRootCauseMessage(e));
      throw new AssertionError(error);
    }
  }

  /**
   * Execute and validate a Stellar expression.
   *
   * <p>This is intended for use while unit testing Stellar expressions.  This ensures that the expression
   * validates successfully and produces a result that can be serialized correctly.
   *
   * @param expression The expression to execute.
   * @param variables The variables to expose to the expression.
   * @return The result of executing the expression.
   */
  public static Object run(String expression, Map<String, Object> variables) {
    return run(expression, variables, Context.EMPTY_CONTEXT());
  }

  /**
   * Execute and validate a Stellar expression.
   *
   * <p>This is intended for use while unit testing Stellar expressions.  This ensures that the expression
   * validates successfully and produces a result that can be serialized correctly.
   *
   * @param expression The expression to execute.
   * @param variables The variables to expose to the expression.
   * @return The result of executing the expression.
   */
  public static Object run(String expression, VariableResolver variables) {
    return run(expression, variables, Context.EMPTY_CONTEXT());
  }

  /**
   * Execute and validate a Stellar expression.
   *
   * <p>This is intended for use while unit testing Stellar expressions.  This ensures that the expression
   * validates successfully and produces a result that can be serialized correctly.
   *
   * @param expression The expression to execute.
   * @param context The execution context.
   * @return The result of executing the expression.
   */
  public static Object run(String expression, Context context) {
    return run(expression, Collections.emptyMap(), context);
  }

  public static void validate(String expression, Context context) {
    StellarProcessor processor = new StellarProcessor();
    if(!processor.validate(expression, context)) {
      throw new AssertionError("Invalid expression; expr=" + expression);
    }
  }

  public static void validate(String rule) {
    validate(rule, Context.EMPTY_CONTEXT());
  }

  public static boolean runPredicate(String rule, Map resolver) {
    return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
  }

  public static boolean runPredicate(String rule, Map resolver, Context context) {
    return runPredicate(rule, new MapVariableResolver(resolver), context);
  }

  public static boolean runPredicate(String rule, VariableResolver resolver) {
    return runPredicate(rule, resolver, Context.EMPTY_CONTEXT());
  }

  public static boolean runPredicate(String rule, VariableResolver resolver, Context context) {
    StellarPredicateProcessor processor = new StellarPredicateProcessor();
    if(!processor.validate(rule)) {
      throw new AssertionError(rule + " not valid.");
    }
    return processor.parse(rule, resolver, StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  public static void runWithArguments(String function, Object argument, Object expected) {
    runWithArguments(function, ImmutableList.of(argument), expected);
  }

  public static void runWithArguments(String function, List<Object> arguments, Object expected) {
    Supplier<Stream<Map.Entry<String, Object>>> kvStream = () -> StreamSupport
            .stream(new XRange(arguments.size()), false)
            .map(i -> new AbstractMap.SimpleImmutableEntry<>("var" + i, arguments.get(i)));

    String args = kvStream.get().map(kv -> kv.getKey()).collect(Collectors.joining(","));
    Map<String, Object> variables = kvStream.get().collect(Collectors.toMap(kv -> kv.getKey(), kv -> kv.getValue()));
    String stellarStatement = function + "(" + args + ")";
    String reason = stellarStatement + " != " + expected + " with variables: " + variables;

    if (expected instanceof Double) {
      if(!(Math.abs((Double) expected - (Double) run(stellarStatement, variables)) < 1e-6)) {
        throw new AssertionError(reason);
      }
    } else {
      if(!expected.equals(run(stellarStatement, variables))) {
        throw new AssertionError(reason);
      }
    }
  }

  public static class XRange extends Spliterators.AbstractIntSpliterator {
    int end;
    int i = 0;

    public XRange(int start, int end) {
      super(end - start, 0);
      i = start;
      this.end = end;
    }

    public XRange(int end) {
      this(0, end);
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
      boolean isDone = i >= end;
      if (isDone) {
        return false;
      } else {
        action.accept(i);
        i++;
        return true;
      }
    }

    /**
     * {@inheritDoc}
     *
     * @param action to {@code IntConsumer} and passed to {@link #tryAdvance(IntConsumer)};
     *     otherwise the action is adapted to an instance of {@code IntConsumer}, by boxing the
     *     argument of {@code IntConsumer}, and then passed to {@link #tryAdvance(IntConsumer)}.
     */
    @Override
    public boolean tryAdvance(Consumer<? super Integer> action) {
      boolean isDone = i >= end;
      if (isDone) {
        return false;
      } else {
        action.accept(i);
        i++;
        return true;
      }
    }
  }
}
