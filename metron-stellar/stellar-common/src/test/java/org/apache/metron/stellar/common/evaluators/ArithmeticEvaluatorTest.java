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

package org.apache.metron.stellar.common.evaluators;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.stellar.dsl.Token;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ArithmeticEvaluatorTest {
  ArithmeticEvaluator evaluator = ArithmeticEvaluator.INSTANCE;

  @Test
  public void evaluateDoubleShouldReturnDoubleAdd() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(3.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerAdd() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(3, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatAdd() {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(3F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongAdd() {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(3L, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnDoubleMul() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(2.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerMul() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(2, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatMul() {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(2F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongMul() {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(2L, evaluated.getValue());
  }

  @Test
  public void evaluateDoubleShouldReturnDoubleSub() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(-1.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerSub() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(-1, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatSub() {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(-1F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongSub() {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(-1L, evaluated.getValue());
  }

  @Test
  public void evaluateDoubleShouldReturnDoubleDiv() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(1 / 2D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerDiv() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(1 / 2, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatDiv() {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(0.5F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongDiv() {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(0L, evaluated.getValue());
  }

  @Test
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputIsNull() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            evaluator.evaluate(
                ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), null));
  }

  @Test
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputsKeyIsNull() {
    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(null, mock(Token.class));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p));
  }

  @Test
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputsValueIsNull() {
    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(mock(Token.class), null);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), p));
  }

  @Test
  public void evaluateShouldConvertShortsToIntegersType() {
    Token<Short> l = mock(Token.class);
    when(l.getValue()).thenReturn((short) 2);

    Token<Short> r = mock(Token.class);
    when(r.getValue()).thenReturn((short) 3);

    Token<? extends Number> evaluated0 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), Pair.of(l, r));
    Token<? extends Number> evaluated1 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), Pair.of(l, r));
    Token<? extends Number> evaluated2 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), Pair.of(l, r));
    Token<? extends Number> evaluated3 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), Pair.of(l, r));

    assertTrue(evaluated0.getValue() instanceof Integer);
    assertEquals(5, evaluated0.getValue());

    assertTrue(evaluated1.getValue() instanceof Integer);
    assertEquals(-1, evaluated1.getValue());

    assertTrue(evaluated2.getValue() instanceof Integer);
    assertEquals(6, evaluated2.getValue());

    assertTrue(evaluated3.getValue() instanceof Integer);
    assertEquals(0, evaluated3.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerWhenLeftsValueIsNull() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(null);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(2, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerWhenRightsValueIsNull() {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(null);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(1, evaluated.getValue());
  }

  @Test
  public void verifyExpectedReturnTypes() {
    Token<Integer> integer = mock(Token.class);
    when(integer.getValue()).thenReturn(1);

    Token<Long> lng = mock(Token.class);
    when(lng.getValue()).thenReturn(1L);

    Token<Double> dbl = mock(Token.class);
    when(dbl.getValue()).thenReturn(1.0D);

    Token<Float> flt = mock(Token.class);
    when(flt.getValue()).thenReturn(1.0F);

    Map<Pair<Token<? extends Number>, Token<? extends Number>>, Class<? extends Number>> expectedReturnTypeMappings =
        new HashMap<Pair<Token<? extends Number>, Token<? extends Number>>, Class<? extends Number>>() {{
          put(Pair.of(flt, lng), Float.class);
          put(Pair.of(flt, dbl), Double.class);
          put(Pair.of(flt, flt), Float.class);
          put(Pair.of(flt, integer), Float.class);

          put(Pair.of(lng, lng), Long.class);
          put(Pair.of(lng, dbl), Double.class);
          put(Pair.of(lng, flt), Float.class);
          put(Pair.of(lng, integer), Long.class);

          put(Pair.of(dbl, lng), Double.class);
          put(Pair.of(dbl, dbl), Double.class);
          put(Pair.of(dbl, flt), Double.class);
          put(Pair.of(dbl, integer), Double.class);

          put(Pair.of(integer, lng), Long.class);
          put(Pair.of(integer, dbl), Double.class);
          put(Pair.of(integer, flt), Float.class);
          put(Pair.of(integer, integer), Integer.class);
    }};

    expectedReturnTypeMappings.forEach( (pair, expectedClass) -> {
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(null), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(null), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(null), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(null), pair).getValue().getClass() == expectedClass);
    });
  }
}
