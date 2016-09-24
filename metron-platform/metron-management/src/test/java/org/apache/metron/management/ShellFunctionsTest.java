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
package org.apache.metron.management;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.FunctionResolverSingleton;
import org.apache.metron.common.dsl.StellarFunctionInfo;
import org.apache.metron.common.stellar.StellarTest;
import org.apache.metron.common.stellar.shell.StellarExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShellFunctionsTest {

  Map<String, StellarExecutor.VariableResult> variables = ImmutableMap.of(
          "var1" , new StellarExecutor.VariableResult("TO_UPPER('casey')", "CASEY"),
          "var2" , new StellarExecutor.VariableResult(null, "foo"),
          "var3" , new StellarExecutor.VariableResult(null, null),
          "var4" , new StellarExecutor.VariableResult("blah", null)
  );

  Context context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES , () -> variables)
            .build();
/**
╔══════════╤═══════╤════════════╗
║ VARIABLE │ VALUE │ EXPRESSION ║
╠══════════╪═══════╪════════════╣
║ foo      │ 2.0   │ 1 + 1      ║
╚══════════╧═══════╧════════════╝
 **/
  @Multiline
  static String expectedListWithFoo;

  @Test
  public void testListVarsWithVars() {
    Map<String, StellarExecutor.VariableResult> variables = ImmutableMap.of("foo", new StellarExecutor.VariableResult("1 + 1", 2.0));

    Context context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES , () -> variables)
            .build();
    Object out = StellarTest.run("SHELL_LIST_VARS()", new HashMap<>(), context);
    Assert.assertEquals(expectedListWithFoo, out);
  }

/**
╔══════════╤═══════╤════════════╗
║ VARIABLE │ VALUE │ EXPRESSION ║
╠══════════╧═══════╧════════════╣
║ (empty)                       ║
╚═══════════════════════════════╝
 **/
  @Multiline
  static String expectedEmptyList;

  @Test
  public void testListVarsWithoutVars() {
    Context context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES , () -> new HashMap<>())
            .build();
    Object out = StellarTest.run("SHELL_LIST_VARS()", new HashMap<>(), context);
    Assert.assertEquals(expectedEmptyList, out);
  }
/**
╔════════╤═══════╗
║ KEY    │ VALUE ║
╠════════╪═══════╣
║ field1 │ val1  ║
╟────────┼───────╢
║ field2 │ val2  ║
╚════════╧═══════╝
 **/
  @Multiline
  static String expectedMap2Table;

  @Test
  public void testMap2Table() {
    Map<String, Object> variables = ImmutableMap.of("map_field", ImmutableMap.of("field1", "val1", "field2", "val2"));
    Context context = Context.EMPTY_CONTEXT();
    Object out = StellarTest.run("SHELL_MAP2TABLE(map_field)", variables, context);
    Assert.assertEquals(expectedMap2Table, out);
  }
 /**
╔═════╤═══════╗
║ KEY │ VALUE ║
╠═════╧═══════╣
║ (empty)     ║
╚═════════════╝
 **/
  @Multiline
  static String expectedMap2TableNullInput;

  @Test
  public void testMap2TableNullInput() {
    Map<String, Object> variables = new HashMap<>();
    Context context = Context.EMPTY_CONTEXT();
    Object out = StellarTest.run("SHELL_MAP2TABLE(map_field)", variables, context);
    Assert.assertEquals(expectedMap2TableNullInput, out);
  }

  @Test
  public void testMap2TableInsufficientArgs() {
    Map<String, Object> variables = new HashMap<>();
    Context context = Context.EMPTY_CONTEXT();
    Object out = StellarTest.run("SHELL_MAP2TABLE()", variables, context);
    Assert.assertNull(out);
  }

  @Test
  public void testVars2Map() {
    Object out = StellarTest.run("SHELL_VARS2MAP('var1', 'var2')", new HashMap<>(), context);
    Assert.assertTrue(out instanceof Map);
    Map<String, String> mapOut = (Map<String, String>)out;
    //second one is null, so we don't want it there.
    Assert.assertEquals(1, mapOut.size());
    Assert.assertEquals("TO_UPPER('casey')", mapOut.get("var1"));
  }

  @Test
  public void testVars2MapEmpty() {
    Object out = StellarTest.run("SHELL_VARS2MAP()", new HashMap<>(), context);
    Map<String, String> mapOut = (Map<String, String>)out;
    Assert.assertEquals(0, mapOut.size());
  }

  @Test
  public void testGetExpression() {
    Object out = StellarTest.run("SHELL_GET_EXPRESSION('var1')", new HashMap<>(), context);
    Assert.assertTrue(out instanceof String);
    String expression = (String)out;
    //second one is null, so we don't want it there.
    Assert.assertEquals("TO_UPPER('casey')", expression);
  }

  @Test
  public void testGetExpressionEmpty() {
    Object out = StellarTest.run("SHELL_GET_EXPRESSION()", new HashMap<>(), context);
    Assert.assertNull(out );
  }

}
