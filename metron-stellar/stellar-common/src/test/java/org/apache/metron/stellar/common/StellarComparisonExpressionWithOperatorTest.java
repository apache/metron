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
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.*;

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
    boolean thrown = false;
    try {
      run("foo < 3.0f", ImmutableMap.of());
    }catch(ParseException pe){
      thrown = true;
    }
    assertTrue(thrown);
    thrown = false;
    try {
      run("foo < foo", ImmutableMap.of());
    }catch(ParseException pe){
      thrown = true;
    }
    assertTrue(thrown);

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

    assertTrue(runPredicate("'casey' == foo.bar", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'casey' == foo", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'casey' != foo",new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'stella' == 'stella'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'stella' == foo", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("foo== foo", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("empty== ''", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("spaced == 'metron is great'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate(null, new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate(" ",(new DefaultVariableResolver(variableMap::get,variableMap::containsKey))));
  }

  @Test
  public void compareNumberAndStringWithSameValueShouldBeFalse() throws Exception {
    Map<String,String> variableMap = new HashMap<>();
    assertFalse(runPredicate("1 == '1'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'1' == 1", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void comparingNullShouldNotCauseNullPointer() throws Exception {
    Map<String,String> variableMap = new HashMap<>();
    assertFalse(runPredicate("null == '1'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("\"1\" == null", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("null == null", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void makeSureSingleQuotesAndDoubleQuotesAreEqual() throws Exception {
    Map<String,String> variableMap = new HashMap<>();
    assertTrue(runPredicate("\"1\" == '1'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'1' == \"1\"", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'1' == \"1\"", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void makeSureSingleQuoteStringsAreEvaluatedAsStrings() throws Exception {
    Map<String,String> variableMap = new HashMap<>();
    assertFalse(runPredicate("55 == '7'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("97 == 'a'", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
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
    assertTrue(runPredicate("num == 7", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num < num2", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num < TO_DOUBLE(num2)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num < TO_DOUBLE(num4)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num < 100", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num == num3", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("num == num2", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num == num2 || true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("num > num2", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("num == 7 && num > 2", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void positiveAndNegativeZeroAreEqual() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("num", -0);
    }};

    Arrays.asList("!=", "==").forEach(op -> {
      assertEquals("==".equals(op), runPredicate("num " + op + " 0", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("0 " + op + " -0", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("0 " + op + " -0d", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("-0 " + op + " 0", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("-0F " + op + " 0D", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("-0.F " + op + " 0", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("-0.F " + op + " 0F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("==".equals(op), runPredicate("-0.D " + op + " 0D", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    });
  }

  @Test
  public void naNIsNotEqualToNaN() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    Arrays.asList("!=", "==").forEach(op -> {
      assertEquals("!=".equals(op), runPredicate("(0f/0f) " + op + " (0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0f/0f) " + op + " (0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (-0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0f/-0f) " + op + " (-0f/-0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (-0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0f/-0f) " + op + " (-0f/-0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0f/0f) " + op + " (-0f/0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0f/0d) " + op + " (-0f/-0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0d/-0f) " + op + " (0f/-0f)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0f/0f) " + op + " (0f/-0d)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(-0d/-0d) " + op + " (0d/-0d)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals("!=".equals(op), runPredicate("(0d/0d) " + op + " (0d/0d)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    });
  }

  @Test
  public void booleanComparisonTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("t", true);
      put("f", false);
    }};

    assertTrue(runPredicate("t != f", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("f != t", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("true != false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("true != true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("false != true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("false != false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));

    assertFalse(runPredicate("t == f", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("f == t", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("true == false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("true == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("false == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("false == false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));

    assertFalse(runPredicate("null == false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("null == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("true == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("false == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void nullComparisonTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();

    assertFalse(runPredicate("null == false", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("null == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("true == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("false == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("1 == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'null' == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'' == NULL", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("null == ''", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));

    assertTrue(runPredicate("NULL == null", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void precisionEqualityTests() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertEquals(0.1 + 0.2 == 0.3, runPredicate("0.1 + 0.2 == 0.3", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingLT() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertThrows(
        ParseException.class,
        () ->
            runPredicate(
                "1 < '1'",
                new DefaultVariableResolver(variableMap::get, variableMap::containsKey)));
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingLTE() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertThrows(
        ParseException.class,
        () ->
            runPredicate(
                "'1' <= 1",
                new DefaultVariableResolver(variableMap::get, variableMap::containsKey)));
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingGT() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertThrows(
        ParseException.class,
        () ->
            runPredicate(
                "1 > '1'",
                new DefaultVariableResolver(variableMap::get, variableMap::containsKey)));
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingGTE() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    assertThrows(
        ParseException.class,
        () ->
            runPredicate(
                "'1' >= 1",
                new DefaultVariableResolver(variableMap::get, variableMap::containsKey)));
  }

  @Test
  public void differentTypesShouldThrowErrorWhenUsingComparisons() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    final Integer[] result = {0};

    Stream.of("<", "<=", ">", ">=").forEach(op -> {
      assertFalse(runPredicate("'1' " + op + " null", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    });
  }

  @Test
  public void makeSurePrecisionIsProperlyHandled() throws Exception {
    final Map<String, Object> variableMap = new HashMap<>();
    {
      assertEquals(1 == 1.00000001, runPredicate("1 == 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 < 1.00000001, runPredicate("1 < 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 <= 1.00000001, runPredicate("1 <= 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 > 1.00000001, runPredicate("1 > 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 >= 1.00000001, runPredicate("1 >= 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    }
    {
      assertEquals(1 == 1.00000001F, runPredicate("1 == 1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 < 1.00000001F, runPredicate("1 < 1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 <= 1.00000001F, runPredicate("1 <= 1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 > 1.00000001F, runPredicate("1 > 1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1 >= 1.00000001F, runPredicate("1 >= 1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    }
    {
      assertEquals(1.00000001F == 1.00000001, runPredicate("1.00000001F == 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1.00000001F < 1.00000001, runPredicate("1.00000001F < 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1.00000001F <= 1.00000001, runPredicate("1.00000001F <= 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1.00000001F > 1.00000001, runPredicate("1.00000001F > 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(1.00000001F >= 1.00000001, runPredicate("1.00000001F >= 1.00000001", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    }
    {
      assertEquals(-1L == -1.00000001F, runPredicate("-1L == -1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(-1L < -1.00000001F, runPredicate("-1L < -1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(-1L <= -1.00000001F, runPredicate("-1L <= -1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(-1L > -1.00000001F, runPredicate("-1L > -1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
      assertEquals(-1L >= -1.00000001F, runPredicate("-1L >= -1.00000001F", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    }
  }
}
