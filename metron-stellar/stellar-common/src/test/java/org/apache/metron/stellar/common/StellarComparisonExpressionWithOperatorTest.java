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
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ALL")
public class StellarComparisonExpressionWithOperatorTest {
  @SuppressWarnings({"RedundantConditionalExpression", "ConstantConditions"})
  @Test
  public void checkLessThanComparisonOperators() throws Exception {
    assertEquals(1 < 2, run("1 < 2", ImmutableMap.of()));
    assertEquals(1f < 2, run("1f < 2", ImmutableMap.of()));
    assertEquals(1f < 2d, run("1f < 2d", ImmutableMap.of()));
    assertEquals(1f < 2e-4d, run("1f < 2e-4d", ImmutableMap.of()));
    assertEquals(1L < 2e-4d, run("1L < 2e-4d", ImmutableMap.of()));
    assertEquals(1 < 2e-4d, run("1 < 2e-4d", ImmutableMap.of()));
    assertEquals(1 < 2L, run("1 < 2L", ImmutableMap.of()));
    assertEquals(1.0f < 2.0f, run("1.0f < 2.0f", ImmutableMap.of()));
    assertEquals(1L < 3.0f, run("1L < 3.0f", ImmutableMap.of()));
    assertEquals(1 < 3.0f, run("1 < 3.0f", ImmutableMap.of()));
    assertEquals(1.0 < 3.0f, run("1.0 < 3.0f", ImmutableMap.of()));
    assertEquals(false, run("foo < 3.0f", ImmutableMap.of()));
    assertEquals(false, run("foo < foo", ImmutableMap.of()));
    assertEquals(1L < 3.0f ? true : false, run("if 1L < 3.0f then true else false", ImmutableMap.of()));
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void checkComparisonOperationsWithFunctions() throws Exception {
    assertEquals(1f >= 2, run("TO_FLOAT(1) >= 2", ImmutableMap.of()));
    assertEquals(1f <= 2, run("TO_FLOAT(1) <= TO_FLOAT(2)", ImmutableMap.of()));
    assertEquals(1f == 2, run("TO_FLOAT(1) == TO_LONG(2)", ImmutableMap.of()));
    assertEquals(12.31f == 10.2f, run("TO_FLOAT(12.31) < 10.2f", ImmutableMap.of()));
  }

  @Test
  public void testSimpleOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
      put("foo.bar", "casey");
    }};

    assertTrue(runPredicate("'casey' == foo.bar", variableMap::get));
    assertTrue(runPredicate("'casey' == foo", variableMap::get));
    assertFalse(runPredicate("'casey' != foo", variableMap::get));
    assertTrue(runPredicate("'stella' == 'stella'", variableMap::get));
    assertFalse(runPredicate("'stella' == foo", variableMap::get));
    assertTrue(runPredicate("foo== foo", variableMap::get));
    assertTrue(runPredicate("empty== ''", variableMap::get));
    assertTrue(runPredicate("spaced == 'metron is great'", variableMap::get));
    assertTrue(runPredicate(null, variableMap::get));
    assertTrue(runPredicate("", variableMap::get));
    assertTrue(runPredicate(" ", variableMap::get));
  }

  @Test
  public void compareNumberAndStringWithSameValueShouldBeFalse() throws Exception {
    assertFalse(runPredicate("1 == '1'", new HashMap<>()::get));
    assertFalse(runPredicate("'1' == 1", new HashMap<>()::get));
  }

  @Test
  public void comparingNullShouldNotCauseNullPointer() throws Exception {
    assertFalse(runPredicate("null == '1'", new HashMap<>()::get));
    assertFalse(runPredicate("\"1\" == null", new HashMap<>()::get));
    assertTrue(runPredicate("null == null", new HashMap<>()::get));
  }

  @Test
  public void makeSureSingleQuotesAndDoubleQuotesAreEqual() throws Exception {
    assertTrue(runPredicate("\"1\" == '1'", new HashMap<>()::get));
    assertTrue(runPredicate("'1' == \"1\"", new HashMap<>()::get));
    assertTrue(runPredicate("'1' == \"1\"", new HashMap<>()::get));
  }

  @Test
  public void makeSureSingleQuoteStringsAreEvaluatedAsStrings() throws Exception {
    assertFalse(runPredicate("55 == '7'", new HashMap<>()::get));
    assertFalse(runPredicate("97 == 'a'", new HashMap<>()::get));
  }

  @Test
  public void testNumericComparisonFunctions() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("num", 7);
      put("num2", 8.5);
      put("num3", 7);
      put("num4", "8.5");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    assertTrue(runPredicate("num == 7", variableMap::get));
    assertTrue(runPredicate("num < num2", variableMap::get));
    assertTrue(runPredicate("num < TO_DOUBLE(num2)", variableMap::get));
    assertTrue(runPredicate("num < TO_DOUBLE(num4)", variableMap::get));
    assertTrue(runPredicate("num < 100", variableMap::get));
    assertTrue(runPredicate("num == num3", variableMap::get));
    assertFalse(runPredicate("num == num2", variableMap::get));
    assertTrue(runPredicate("num == num2 || true", variableMap::get));
    assertFalse(runPredicate("num > num2", variableMap::get));
    assertTrue(runPredicate("num == 7 && num > 2", variableMap::get));
  }

  @Test
  public void positiveAndNegativeZeroAreEqual() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("num", -0);
    }};

    Arrays.asList("!=", "==").forEach(op -> {
      assertEquals("==".equals(op), runPredicate("num " + op + " 0", variableMap::get));
      assertEquals("==".equals(op), runPredicate("0 " + op + " -0", variableMap::get));
      assertEquals("==".equals(op), runPredicate("0 " + op + " -0d", variableMap::get));
      assertEquals("==".equals(op), runPredicate("-0 " + op + " 0", variableMap::get));
      assertEquals("==".equals(op), runPredicate("-0F " + op + " 0D", variableMap::get));
      assertEquals("==".equals(op), runPredicate("-0.F " + op + " 0", variableMap::get));
      assertEquals("==".equals(op), runPredicate("-0.F " + op + " 0F", variableMap::get));
      assertEquals("==".equals(op), runPredicate("-0.D " + op + " 0D", variableMap::get));
    });
  }

  @Test
  public void naNIsNotEqualToNaN() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    Arrays.asList("!=", "==").forEach(op -> {
      assertEquals("!=".equals(op), runPredicate("(0f/0f) " + op + " (0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0f/0f) " + op + " (0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (-0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (-0f/-0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (-0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (-0f/-0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0f/0f) " + op + " (-0f/0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0f/0d) " + op + " (-0f/-0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0d/-0f) " + op + " (0f/-0f)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0f/0f) " + op + " (0f/-0d)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(-0d/-0d) " + op + " (0d/-0d)", variableMap::get));
      assertEquals("!=".equals(op), runPredicate("(0d/0d) " + op + " (0d/0d)", variableMap::get));
    });
  }

  @Test
  public void booleanComparisonTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("t", true);
      put("f", false);
    }};

    assertTrue(runPredicate("t != f", variableMap::get));
    assertTrue(runPredicate("f != t", variableMap::get));
    assertTrue(runPredicate("true != false", variableMap::get));
    assertFalse(runPredicate("true != true", variableMap::get));
    assertTrue(runPredicate("false != true", variableMap::get));
    assertFalse(runPredicate("false != false", variableMap::get));

    assertFalse(runPredicate("t == f", variableMap::get));
    assertFalse(runPredicate("f == t", variableMap::get));
    assertFalse(runPredicate("true == false", variableMap::get));
    assertTrue(runPredicate("true == true", variableMap::get));
    assertFalse(runPredicate("false == true", variableMap::get));
    assertTrue(runPredicate("false == false", variableMap::get));

    assertFalse(runPredicate("null == false", variableMap::get));
    assertFalse(runPredicate("null == true", variableMap::get));
    assertFalse(runPredicate("true == NULL", variableMap::get));
    assertFalse(runPredicate("false == NULL", variableMap::get));
  }

  @Test
  public void nullComparisonTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();

    assertFalse(runPredicate("null == false", variableMap::get));
    assertFalse(runPredicate("null == true", variableMap::get));
    assertFalse(runPredicate("true == NULL", variableMap::get));
    assertFalse(runPredicate("false == NULL", variableMap::get));
    assertFalse(runPredicate("1 == NULL", variableMap::get));
    assertFalse(runPredicate("'null' == NULL", variableMap::get));
    assertFalse(runPredicate("'' == NULL", variableMap::get));
    assertFalse(runPredicate("null == ''", variableMap::get));

    assertTrue(runPredicate("NULL == null", variableMap::get));
  }

  @Test
  public void precisionEqualityTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertEquals(0.1 + 0.2 == 0.3, runPredicate("0.1 + 0.2 == 0.3", variableMap::get));
  }

  @Test(expected = ParseException.class)
  public void differentTypesShouldThrowErrorWhenUsingLT() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    runPredicate("1 < '1'", variableMap::get);
  }

  @Test(expected = ParseException.class)
  public void differentTypesShouldThrowErrorWhenUsingLTE() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    runPredicate("'1' <= 1", variableMap::get);
  }

  @Test(expected = ParseException.class)
  public void differentTypesShouldThrowErrorWhenUsingGT() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    runPredicate("1 > '1'", variableMap::get);
  }

  @Test(expected = ParseException.class)
  public void differentTypesShouldThrowErrorWhenUsingGTE() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    runPredicate("'1' >= 1", variableMap::get);
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingComparisons() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    final Integer[] result = {0};

    Stream.of("<", "<=", ">", ">=").forEach(op -> {
      assertFalse(runPredicate("'1' " + op + " null", variableMap::get));
    });
  }

  @Test
  public void makeSurePrecisionIsProperlyHandled() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    {
      assertEquals(1 == 1.00000001, runPredicate("1 == 1.00000001", variableMap::get));
      assertEquals(1 < 1.00000001, runPredicate("1 < 1.00000001", variableMap::get));
      assertEquals(1 <= 1.00000001, runPredicate("1 <= 1.00000001", variableMap::get));
      assertEquals(1 > 1.00000001, runPredicate("1 > 1.00000001", variableMap::get));
      assertEquals(1 >= 1.00000001, runPredicate("1 >= 1.00000001", variableMap::get));
    }
    {
      assertEquals(1 == 1.00000001F, runPredicate("1 == 1.00000001F", variableMap::get));
      assertEquals(1 < 1.00000001F, runPredicate("1 < 1.00000001F", variableMap::get));
      assertEquals(1 <= 1.00000001F, runPredicate("1 <= 1.00000001F", variableMap::get));
      assertEquals(1 > 1.00000001F, runPredicate("1 > 1.00000001F", variableMap::get));
      assertEquals(1 >= 1.00000001F, runPredicate("1 >= 1.00000001F", variableMap::get));
    }
    {
      assertEquals(1.00000001F == 1.00000001, runPredicate("1.00000001F == 1.00000001", variableMap::get));
      assertEquals(1.00000001F < 1.00000001, runPredicate("1.00000001F < 1.00000001", variableMap::get));
      assertEquals(1.00000001F <= 1.00000001, runPredicate("1.00000001F <= 1.00000001", variableMap::get));
      assertEquals(1.00000001F > 1.00000001, runPredicate("1.00000001F > 1.00000001", variableMap::get));
      assertEquals(1.00000001F >= 1.00000001, runPredicate("1.00000001F >= 1.00000001", variableMap::get));
    }
    {
      assertEquals(-1L == -1.00000001F, runPredicate("-1L == -1.00000001F", variableMap::get));
      assertEquals(-1L < -1.00000001F, runPredicate("-1L < -1.00000001F", variableMap::get));
      assertEquals(-1L <= -1.00000001F, runPredicate("-1L <= -1.00000001F", variableMap::get));
      assertEquals(-1L > -1.00000001F, runPredicate("-1L > -1.00000001F", variableMap::get));
      assertEquals(-1L >= -1.00000001F, runPredicate("-1L >= -1.00000001F", variableMap::get));
    }
  }
}
