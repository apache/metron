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
package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.stellar.common.shell.VariableResult;
import org.apache.metron.stellar.common.shell.cli.PausableInput;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.Context.Capabilities;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;

public class ShellFunctionsTest {

  Map<String, VariableResult> variables = ImmutableMap.of(
          "var1" , VariableResult.withExpression("CASEY", "TO_UPPER('casey')"),
          "var2" , VariableResult.withValue("foo"),
          "var3" , VariableResult.withValue(null),
          "var4" , VariableResult.withExpression(null, "blah")
  );

  Context context = new Context.Builder()
            .with(Context.Capabilities.SHELL_VARIABLES , () -> variables).build();

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
    Map<String, VariableResult> variables = ImmutableMap.of(
            "foo", VariableResult.withExpression(2.0, "1 + 1"));

    Context context = new Context.Builder()
            .with(Context.Capabilities.SHELL_VARIABLES , () -> variables)
            .build();
    Object out = run("SHELL_LIST_VARS()", new HashMap<>(), context);
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
            .with(Context.Capabilities.SHELL_VARIABLES, () -> new HashMap<>())
            .build();
    Object out = run("SHELL_LIST_VARS()", new HashMap<>(), context);
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
    Object out = run("SHELL_MAP2TABLE(map_field)", variables, context);
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
    Map<String,Object> variables = new HashMap<String,Object>(){{
      put("map_field",null);
    }};
    Context context = Context.EMPTY_CONTEXT();
    Object out = run("SHELL_MAP2TABLE(map_field)", variables, context);
    Assert.assertEquals(expectedMap2TableNullInput, out);
  }

  @Test
  public void testMap2TableInsufficientArgs() {
    Map<String, Object> variables = new HashMap<>();
    Context context = Context.EMPTY_CONTEXT();
    Object out = run("SHELL_MAP2TABLE()", variables, context);
    Assert.assertNull(out);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVars2Map() {
    Object out = run("SHELL_VARS2MAP('var1', 'var2')", new HashMap<>(), context);
    Assert.assertTrue(out instanceof Map);
    Map<String, String> mapOut = (Map<String, String>)out;
    //second one is null, so we don't want it there.
    Assert.assertEquals(1, mapOut.size());
    Assert.assertEquals("TO_UPPER('casey')", mapOut.get("var1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVars2MapEmpty() {
    Object out = run("SHELL_VARS2MAP()", new HashMap<>(), context);
    Map<String, String> mapOut = (Map<String, String>)out;
    Assert.assertEquals(0, mapOut.size());
  }

  @Test
  public void testGetExpression() {
    Object out = run("SHELL_GET_EXPRESSION('var1')", new HashMap<>(), context);
    Assert.assertTrue(out instanceof String);
    String expression = (String)out;
    //second one is null, so we don't want it there.
    Assert.assertEquals("TO_UPPER('casey')", expression);
  }

  @Test
  public void testGetExpressionEmpty() {
    Object out = run("SHELL_GET_EXPRESSION()", new HashMap<>(), context);
    Assert.assertNull(out );
  }

  @Test
  public void testEdit() throws Exception {
    System.getProperties().put("EDITOR", "/bin/cat");
    Object out = run("TO_UPPER(SHELL_EDIT(foo))", ImmutableMap.of("foo", "foo"), context);
    Assert.assertEquals("FOO", out);
  }

}
