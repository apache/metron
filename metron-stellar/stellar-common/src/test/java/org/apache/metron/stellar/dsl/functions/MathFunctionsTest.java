/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MathFunctionsTest {

  public static final double EPSILON = 1e-7;
  public static Map<Double, Double> baseExpectations = new HashMap<Double, Double>() {{
    put(Double.NaN, Double.NaN);
  }};

  public static Object run(String rule, Map<String, Object> variables) {
    Context context = Context.EMPTY_CONTEXT();
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, new DefaultVariableResolver(v -> variables.get(v),v -> variables.containsKey(v)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testAbs() {
    assertValues("ABS",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(10.5d, 10.5d);
             put(-10.5d, 10.5d);
           }}
    );
  }

  @Test
  public void testSqrt() {
    assertValues("SQRT",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(25d, 5d);
             put(-10.5d, Double.NaN);
           }}
    );
  }

  @Test
  public void testCeiling() {
    assertValues("CEILING",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(10.5d, 11d);
             put(-10.5d, -10d);
           }}
    );
  }

  @Test
  public void testFloor() {
    assertValues("FLOOR",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(10.5d, 10d);
             put(-10.5d, -11d);
           }}
    );
  }

  @Test
  public void testSin() {
    assertValues("SIN",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(Math.PI/6, 0.5);
             put(Math.PI/4, Math.sqrt(2)/2.0);
             put(Math.PI/3, Math.sqrt(3)/2.0);
             put(Math.PI/2, 1d);
           }}
    );
  }

  @Test
  public void testCos() {
    assertValues("COS",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 1d);
             put(Math.PI/6, Math.sqrt(3)/2.0);
             put(Math.PI/4, Math.sqrt(2)/2.0);
             put(Math.PI/3, 0.5d);
             put(Math.PI/2, 0d);
           }}
    );
  }

  @Test
  public void testTan() {
    assertValues("TAN",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(Math.PI/6, Math.sqrt(3)/3.0);
             put(Math.PI/4, 1d);
             put(Math.PI/3, Math.sqrt(3));
             put(Math.PI/2, Math.sin(Math.PI/2)/Math.cos(Math.PI/2));
           }}
    );
  }

  @Test
  public void testExp() {
    assertValues("EXP",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 1d);
             put(0.5d, Math.sqrt(Math.E));
             put(-0.5d, 1/Math.sqrt(Math.E));
             put(1d, Math.E);
             put(2d, Math.E*Math.E);
           }}
    );
  }

  @Test
  public void testRound() {
    assertValues("ROUND",
           new HashMap<Double, Double>(baseExpectations) {{
             put(0d, 0d);
             put(0.5d, 1d);
             put(0.4d, 0d);
             put(-0.5d, 0d);
           }}
    );
  }

  @Test
  public void testNaturalLog() {
    testLog("LN", Math.E);
  }

  @Test
  public void testLog2() {
    testLog("LOG2", 2);
  }

  @Test
  public void testLog10() {
    testLog("LOG10", 10);
  }

  public void assertValues(String func, Map<Double, Double> expected) {
    for(Map.Entry<Double, Double> test : expected.entrySet()) {
      for(String expr : ImmutableList.of(func + "(value)"
                                        ,func + "(" + test.getKey() + ")"
                                        )
         )
      {
        if (Double.isNaN(test.getValue())) {
          Assert.assertTrue(expr + " != NaN, where value == " + test.getKey(), Double.isNaN(toDouble(run(expr, ImmutableMap.of("value", test.getKey(),"NaN",Double.NaN)))));
        } else {
          Assert.assertEquals(expr + " != " + test.getValue() + " (where value == " + test.getKey() + ")", test.getValue(), toDouble(run(expr, ImmutableMap.of("value", test.getKey()))), EPSILON);
        }
      }
    }
  }

  public Double toDouble(Object n) {
    return ((Number)n).doubleValue();
  }

  public void testLog(String logExpr, double base) {
    Map<Double, Double> expectedValues = new HashMap<Double, Double>(baseExpectations) {{
      put(base, 1d);
      put(0d, Double.NEGATIVE_INFINITY);
    }};
    for(int i = 1;i <= 10;++i) {
      expectedValues.put(Math.pow(base, i), (double)i);
    }
    assertValues(logExpr, expectedValues);
  }

}
