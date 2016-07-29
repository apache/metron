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

package org.apache.metron.common.transformation;

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.VariableResolver;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Tests the MathFunctions.
 */
public class MathFunctionsTest {

  private static Object run(String expression, Map<String, Object> variables) {
    VariableResolver resolver = new MapVariableResolver(variables);
    TransformationProcessor processor = new TransformationProcessor();
    return processor.parse(expression, resolver);
  }

  @Test
  public void testAdd() {
    Assert.assertEquals(10.0d, run("ADD(foo, foo)", ImmutableMap.of("foo", 5.0d)));
    Assert.assertEquals(10.0f, run("ADD(foo, foo)", ImmutableMap.of("foo", 5.0f)));
    Assert.assertEquals(10L, run("ADD(foo, foo)", ImmutableMap.of("foo", 5L)));
    Assert.assertEquals(10, run("ADD(foo, foo)", ImmutableMap.of("foo", 5)));

    // in java, short + short = integer
    Assert.assertEquals(10, run("ADD(foo, foo)", ImmutableMap.of("foo", (short) 5)));
  }

  @Test
  public void testSubtract() {
    Assert.assertEquals(0d, run("SUB(foo, foo)", ImmutableMap.of("foo", 5.0d)));
    Assert.assertEquals(0f, run("SUB(foo, foo)", ImmutableMap.of("foo", 5.0f)));
    Assert.assertEquals(0L, run("SUB(foo, foo)", ImmutableMap.of("foo", 5L)));
    Assert.assertEquals(0, run("SUB(foo, foo)", ImmutableMap.of("foo", 5)));

    // per JVM specification, short - short = integer
    Assert.assertEquals(0, run("SUB(foo, foo)", ImmutableMap.of("foo", (short) 5)));
  }

  @Test
  public void testDivide() {
    Assert.assertEquals(1d, run("DIV(foo, foo)", ImmutableMap.of("foo", 5.0d)));
    Assert.assertEquals(1f, run("DIV(foo, foo)", ImmutableMap.of("foo", 5.0f)));
    Assert.assertEquals(1L, run("DIV(foo, foo)", ImmutableMap.of("foo", 5L)));
    Assert.assertEquals(1, run("DIV(foo, foo)", ImmutableMap.of("foo", 5)));

    // per JVM specification, short / short = integer
    Assert.assertEquals(1, run("DIV(foo, foo)", ImmutableMap.of("foo", (short) 5)));
  }

  @Test
  public void testMultiply() {
    Assert.assertEquals(25.0d, run("MULT(foo, foo)", ImmutableMap.of("foo", 5.0d)));
    Assert.assertEquals(25.0f, run("MULT(foo, foo)", ImmutableMap.of("foo", 5.0f)));
    Assert.assertEquals(25L, run("MULT(foo, foo)", ImmutableMap.of("foo", 5L)));
    Assert.assertEquals(25, run("MULT(foo, foo)", ImmutableMap.of("foo", 5)));

    // in java, short * short = integer
    Assert.assertEquals(25, run("MULT(foo, foo)", ImmutableMap.of("foo", (short) 5)));
  }
}
