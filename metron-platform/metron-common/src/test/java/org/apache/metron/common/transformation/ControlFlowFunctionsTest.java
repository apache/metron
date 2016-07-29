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

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the ControlFlowFunctions.
 */
public class ControlFlowFunctionsTest {

  private static Object run(String expression, Map<String, Object> variables) {
    VariableResolver resolver = new MapVariableResolver(variables);
    TransformationProcessor processor = new TransformationProcessor();
    return processor.parse(expression, resolver);
  }

  private static Object run(String expression) {
    return run(expression, new HashMap<>());
  }

  @Test
  public void testEqualStrings() {
    Assert.assertEquals(true, run("EQUAL('foo', 'foo')"));
    Assert.assertEquals(false, run("EQUAL('foo', 'bar')"));
  }

  @Test
  public void testEqualIntegers() {
    Assert.assertEquals(true, run("EQUAL(2, 2)"));
    Assert.assertEquals(false, run("EQUAL(2, 1)"));
  }

  @Test
  public void testEqualVariables() {
    Assert.assertEquals(true, run("EQUAL(two, two)", ImmutableMap.of("two", 2, "four", 4)));
    Assert.assertEquals(false, run("EQUAL(two, four)", ImmutableMap.of("two", 2, "four", 4)));
  }

  @Test
  public void testIfThenElse() {
    Assert.assertEquals(1, run("IF_THEN_ELSE( EQUAL(2,2), 1, 0)"));
    Assert.assertEquals(0, run("IF_THEN_ELSE( EQUAL(2,1), 1, 0)"));
  }

  @Test
  public void testIfThenElseStrings() {
    Assert.assertEquals("equal", run("IF_THEN_ELSE( EQUAL(2,2), 'equal', 'not')"));
    Assert.assertEquals("not", run("IF_THEN_ELSE( EQUAL(2,1), 'equal', 'not')"));
  }
}
