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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class StellarArithmeticTest {
  @Test
  public void addingLongsShouldYieldLong() {
    final long timestamp = 1452013350000L;
    String query = "TO_EPOCH_TIMESTAMP('2016-01-05 17:02:30', 'yyyy-MM-dd HH:mm:ss', 'UTC') + 2";
    assertEquals(timestamp + 2, run(query, new HashMap<>()));
  }

  @Test
  public void addingIntegersShouldYieldAnInteger() {
    String query = "1 + 2";
    assertEquals(3, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoublesShouldYieldADouble() {
    String query = "1.0 + 2.0";
    assertEquals(3.0, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoubleAndIntegerWhereSubjectIsDoubleShouldYieldADouble() {
    String query = "2.1 + 1";
    assertEquals(3.1, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoubleAndIntegerWhereSubjectIsIntegerShouldYieldADouble() {
    String query = "1 + 2.1";
    assertEquals(3.1, run(query, new HashMap<>()));
  }

  @Test
  public void testArithmetic() {
    assertEquals(3, run("1 + 2", new HashMap<>()));
    assertEquals(3.2, run("1.2 + 2", new HashMap<>()));
    assertEquals(1.2e-3 + 2, run("1.2e-3 + 2", new HashMap<>()));
    assertEquals(1.2f + 3.7, run("1.2f + 3.7", new HashMap<>()));
    assertEquals(12L * (1.2f + 7), run("12L*(1.2f + 7)", new HashMap<>()));
    assertEquals(12.2f * (1.2f + 7L), run("TO_FLOAT(12.2) * (1.2f + 7L)", new HashMap<>()));
  }

  @Test
  public void testNumericOperations() {
    {
      String query = "TO_INTEGER(1 + 2*2 + 3 - 4 - 0.5)";
      assertEquals(3, (Integer) run(query, new HashMap<>()), 1e-6);
    }
    {
      String query = "1 + 2*2 + 3 - 4 - 0.5";
      assertEquals(3.5, (Double) run(query, new HashMap<>()), 1e-6);
    }
    {
      String query = "2*one*(1 + 2*2 + 3 - 4)";
      assertEquals(8, run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "2*(1 + 2 + 3 - 4)";
      assertEquals(4, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 - 4 - 2";
      assertEquals(0, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 + 4";
      assertEquals(10, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "(one + 2)*3";
      assertEquals(9, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "TO_INTEGER((one + 2)*3.5)";
      assertEquals(10, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2*3";
      assertEquals(7, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "TO_LONG(foo)";
      assertNull(run(query, ImmutableMap.of("foo", "not a number")));
    }
    {
      String query = "TO_LONG(foo)";
      assertEquals(232321L, run(query, ImmutableMap.of("foo", "00232321")));
    }
    {
      String query = "TO_LONG(foo)";
      assertEquals(Long.MAX_VALUE, run(query, ImmutableMap.of("foo", Long.toString(Long.MAX_VALUE))));
    }
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

    Map<Pair<String, String>, Class<? extends Number>> expectedReturnTypeMappings =
        new HashMap<Pair<String, String>, Class<? extends Number>>() {{
          put(Pair.of("TO_FLOAT(3.0)", "TO_LONG(1)"), Float.class);
          put(Pair.of("TO_FLOAT(3)", "3.0"), Double.class);
          put(Pair.of("TO_FLOAT(3)", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("TO_FLOAT(3)", "3"), Float.class);

          put(Pair.of("TO_LONG(1)", "TO_LONG(1)"), Long.class);
          put(Pair.of("TO_LONG(1)", "3.0"), Double.class);
          put(Pair.of("TO_LONG(1)", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("TO_LONG(1)", "3"), Long.class);

          put(Pair.of("3.0", "TO_LONG(1)"), Double.class);
          put(Pair.of("3.0", "3.0"), Double.class);
          put(Pair.of("3.0", "TO_FLOAT(3)"), Double.class);
          put(Pair.of("3.0", "3"), Double.class);

          put(Pair.of("3", "TO_LONG(1)"), Long.class);
          put(Pair.of("3", "3.0"), Double.class);
          put(Pair.of("3", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("3", "3"), Integer.class);
        }};

    expectedReturnTypeMappings.forEach((pair, expectedClass) -> {
      assertTrue(run(pair.getLeft() + " * " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " + " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " - " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " / " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
    });
  }

  @Test
  public void happyPathFloatArithmetic() {
    Object run = run(".0f * 1", ImmutableMap.of());
    assertEquals(.0f * 1, run);
    assertEquals(Float.class, run.getClass());

    Object run1 = run("0.f / 1F", ImmutableMap.of());
    assertEquals(0.f / 1F, run1);
    assertEquals(Float.class, run1.getClass());

    Object run2 = run(".0F + 1.0f", ImmutableMap.of());
    assertEquals(.0F + 1.0f, run2);
    assertEquals(Float.class, run2.getClass());

    Object run3 = run("0.0f - 0.1f", ImmutableMap.of());
    assertEquals(0.0f - 0.1f, run3);
    assertEquals(Float.class, run2.getClass());
  }

  @SuppressWarnings("PointlessArithmeticExpression")
  @Test
  public void happyPathLongArithmetic() {
    assertEquals(0L * 1L, run("0L * 1L", ImmutableMap.of()));
    assertEquals(0l / 1L, run("0l / 1L", ImmutableMap.of()));
    assertEquals(1L - 1l, run("1L - 1l", ImmutableMap.of()));
    assertEquals(2147483648L + 1L, run("2147483648L + 1L", ImmutableMap.of()));
  }

  @SuppressWarnings("NumericOverflow")
  @Test
  public void checkInterestingCases() {
    assertEquals((((((1L) + .5d)))) * 6.f, run("(((((1L) + .5d)))) * 6.f", ImmutableMap.of()));
    assertEquals((((((1L) + .5d)))) * 6.f / 0.f, run("(((((1L) + .5d)))) * 6.f / 0.f", ImmutableMap.of()));
    assertEquals(Double.class, run("(((((1L) + .5d)))) * 6.f / 0.f", ImmutableMap.of()).getClass());
  }

  @Test
  public void makeSureStellarProperlyEvaluatesLiteralsToExpectedTypes() {
    {
      assertEquals(Float.class, run("6.f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run(".0f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("6.0F", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("6f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("6e-6f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("6e+6f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("6e6f", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("TO_FLOAT(1231)", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("TO_FLOAT(12.31)", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("TO_FLOAT(12.31f)", ImmutableMap.of()).getClass());
      assertEquals(Float.class, run("TO_FLOAT(12L)", ImmutableMap.of()).getClass());
    }
    {
      assertEquals(Double.class, run("6.d", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6.D", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6.0d", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6D", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6e5D", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6e-5D", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("6e+5D", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("TO_DOUBLE(1231)", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("TO_DOUBLE(12.31)", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("TO_DOUBLE(12.31f)", ImmutableMap.of()).getClass());
      assertEquals(Double.class, run("TO_DOUBLE(12L)", ImmutableMap.of()).getClass());
    }
    {
      assertEquals(Integer.class, run("6", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("60000000", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("-0", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("-60000000", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("TO_INTEGER(1231)", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("TO_INTEGER(12.31)", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("TO_INTEGER(12.31f)", ImmutableMap.of()).getClass());
      assertEquals(Integer.class, run("TO_INTEGER(12L)", ImmutableMap.of()).getClass());
    }
    {
      assertEquals(Long.class, run("12345678910l", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("0l", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("-0l", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("-60000000L", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("-60000000L", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("TO_LONG(1231)", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("TO_LONG(12.31)", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("TO_LONG(12.31f)", ImmutableMap.of()).getClass());
      assertEquals(Long.class, run("TO_LONG(12L)", ImmutableMap.of()).getClass());
    }
  }

  @Test
  public void parseExceptionMultipleLeadingZerosOnInteger() {
    assertThrows(ParseException.class, () -> run("000000", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingZerosOnLong() {
    assertThrows(ParseException.class, () -> run("000000l", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingZerosOnDouble() {
    assertThrows(ParseException.class, () -> run("000000d", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingZerosOnFloat() {
    assertThrows(ParseException.class, () -> run("000000f", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingNegativeSignsFloat() {
    assertThrows(ParseException.class, () -> run("--000000f", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingNegativeSignsDouble() {
    assertThrows(ParseException.class, () -> run("--000000D", ImmutableMap.of()));
  }

  @Test
  public void parseExceptionMultipleLeadingNegativeSignsLong() {
    assertThrows(ParseException.class, () -> run("--000000L", ImmutableMap.of()));
  }

  @Test
  public void unableToDivideByZeroWithIntegers() {
    assertThrows(ParseException.class, () -> run("0/0", ImmutableMap.of()));
  }

  @Test
  public void unableToDivideByZeroWithLongs() {
    assertThrows(ParseException.class, () -> run("0L/0L", ImmutableMap.of()));
  }

  @Test
  public void ableToDivideByZero() {
    assertEquals(0F/0F, run("0F/0F", ImmutableMap.of()));
    assertEquals(0D/0D, run("0D/0D", ImmutableMap.of()));
    assertEquals(0D/0F, run("0D/0F", ImmutableMap.of()));
    assertEquals(0F/0D, run("0F/0D", ImmutableMap.of()));
    assertEquals(0F/0, run("0F/0", ImmutableMap.of()));
    assertEquals(0D/0, run("0D/0", ImmutableMap.of()));
    assertEquals(0/0D, run("0/0D", ImmutableMap.of()));
    assertEquals(0/0F, run("0/0F", ImmutableMap.of()));
  }
}
