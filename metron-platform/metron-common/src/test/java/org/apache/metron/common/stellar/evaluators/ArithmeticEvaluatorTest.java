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

package org.apache.metron.common.stellar.evaluators;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.dsl.Token;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class ArithmeticEvaluatorTest {
  ArithmeticEvaluator evaluator = ArithmeticEvaluator.INSTANCE;

  @Test
  public void evaluateDoubleShouldReturnDoubleAdd() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(3.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerAdd() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(3, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatAdd() throws Exception {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(3F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongAdd() throws Exception {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(3L, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnDoubleMul() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(2.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerMul() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(2, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatMul() throws Exception {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(2F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongMul() throws Exception {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(2L, evaluated.getValue());
  }

  @Test
  public void evaluateDoubleShouldReturnDoubleSub() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(-1.0D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerSub() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(-1, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatSub() throws Exception {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(-1F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongSub() throws Exception {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(-1L, evaluated.getValue());
  }

  @Test
  public void evaluateDoubleShouldReturnDoubleDiv() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Double> r = mock(Token.class);
    when(r.getValue()).thenReturn(2D);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);

    assertTrue(evaluated.getValue() instanceof Double);
    assertEquals(1 / 2D, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerDiv() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(1 / 2, evaluated.getValue());
  }

  @Test
  public void evaluateFloatsShouldReturnFloatDiv() throws Exception {
    Token<Float> l = mock(Token.class);
    when(l.getValue()).thenReturn(1F);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);

    assertTrue(evaluated.getValue() instanceof Float);
    assertEquals(0.5F, evaluated.getValue());
  }

  @Test
  public void evaluateLongsShouldReturnLongDiv() throws Exception {
    Token<Long> l = mock(Token.class);
    when(l.getValue()).thenReturn(1L);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);

    assertTrue(evaluated.getValue() instanceof Long);
    assertEquals(0L, evaluated.getValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputIsNull() throws Exception {
    evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputsKeyIsNull() throws Exception {
    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(null, mock(Token.class));
    evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);
  }

  @Test(expected = IllegalArgumentException.class)
  public void evaluateShouldThroughIllegalArgumentExceptionWhenInputsValueIsNull() throws Exception {
    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(mock(Token.class), null);
    evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), p);
  }

  @Test
  public void evaluateShouldConvertShortsToIntegersType() throws Exception {
    Token<Short> l = mock(Token.class);
    when(l.getValue()).thenReturn((short) 2);

    Token<Short> r = mock(Token.class);
    when(r.getValue()).thenReturn((short) 3);

    Token<? extends Number> evaluated0 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), Pair.of(l, r));
    Token<? extends Number> evaluated1 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), Pair.of(l, r));
    Token<? extends Number> evaluated2 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), Pair.of(l, r));
    Token<? extends Number> evaluated3 = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), Pair.of(l, r));

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
  public void evaluateIntegerShouldReturnIntegerWhenLeftsValueIsNull() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(null);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(2);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(2, evaluated.getValue());
  }

  @Test
  public void evaluateIntegerShouldReturnIntegerWhenRightsValueIsNull() throws Exception {
    Token<Integer> l = mock(Token.class);
    when(l.getValue()).thenReturn(1);

    Token<Integer> r = mock(Token.class);
    when(r.getValue()).thenReturn(null);

    Pair<Token<? extends Number>, Token<? extends Number>> p = Pair.of(l, r);

    Token<? extends Number> evaluated = evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), p);

    assertTrue(evaluated.getValue() instanceof Integer);
    assertEquals(1, evaluated.getValue());
  }

  @Test
  public void verifyExpectedReturnTypes() throws Exception {
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
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.addition(), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.division(), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.subtraction(), pair).getValue().getClass() == expectedClass);
      assertTrue(evaluator.evaluate(ArithmeticEvaluator.ArithmeticEvaluatorFunctions.multiplication(), pair).getValue().getClass() == expectedClass);
    });
  }
}
