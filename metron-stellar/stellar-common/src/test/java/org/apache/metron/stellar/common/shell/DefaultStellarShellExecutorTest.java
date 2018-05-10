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
package org.apache.metron.stellar.common.shell;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the DefaultStellarShellExecutor class.
 */
public class DefaultStellarShellExecutorTest {

  DefaultStellarShellExecutor executor;
  boolean notified;

  @Before
  public void setup() throws Exception {
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testAssignment() {

    // x = 2 + 2
    {
      StellarResult result = executor.execute("x := 2 + 2");
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());
      assertEquals(4, result.getValue().get());
      assertEquals(4, executor.getVariables().get("x"));
    }

    // y = x + 2
    {
      StellarResult result = executor.execute("y := x + 2");
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());
      assertEquals(6, result.getValue().get());
      assertEquals(6, executor.getVariables().get("y"));
    }

    // z = x + y
    {
      StellarResult result = executor.execute("z := x + y");
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());
      assertEquals(10, result.getValue().get());
      assertEquals(10, executor.getVariables().get("z"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAssignmentOfLists() {
    List<Integer> expected = Arrays.asList(1,2,3,4,5);

    // assign a list to a variable
    StellarResult result = executor.execute("x := [1,2,3,4,5]");

    // the result should be a list
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(expected, result.getValue().get());

    // the variable should also be the same list
    List<Integer> variable = (List<Integer>) executor.getVariables().get("x");
    assertEquals(expected, variable);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAssignmentOfMaps() {
    Map<String, Integer> expected = ImmutableMap.<String, Integer>builder()
            .put("a", 10)
            .put("b", 20)
            .build();

    // assign a list to a variable
    StellarResult result = executor.execute("x := {'a':10, 'b':20}");

    // the result should be a map
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(expected, result.getValue().get());

    // the variable should also be the same list
    Map<String, Integer> variable = (Map<String, Integer>) executor.getVariables().get("x");
    assertEquals(expected, variable);
  }

  @Test
  public void testAssignmentWithOddWhitespace() {
    StellarResult result = executor.execute("   x   :=    2 +      2      ");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(4, result.getValue().get());
    assertEquals(4, executor.getVariables().get("x"));
  }

  @Test
  public void testBadAssignment() {
    StellarResult result = executor.execute("x := 2 + ");
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }

  @Test
  public void testExpression() {
    StellarResult result = executor.execute("2 + 2");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(4, result.getValue().get());
  }

  @Test
  public void testExpressionWithOddWhitespace() {
    StellarResult result = executor.execute("    2    +    2");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(4, result.getValue().get());
  }

  @Test
  public void testBadExpression() {
    StellarResult result = executor.execute("2 + ");
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }

  @Test
  public void testMagicCommand() {
    // create a var that we can see with the magic command
    executor.execute("x := 2 + 2");

    // just testing that we can execute the magic, not the actual result
    StellarResult result = executor.execute("%vars");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertNotNull(result.getValue().get());
  }

  @Test
  public void testDefineGlobal() {

    // create a global var
    executor.execute("%define x := 2");

    assertFalse(executor.getVariables().containsKey("x"));

    // the global should have been defined
    assertEquals(2, executor.getGlobalConfig().get("x"));
  }

  @Test
  public void testBadMagicCommand() {
    StellarResult result = executor.execute("%invalid");
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }

  @Test
  public void testDocCommand() {
    // just testing that we can execute the doc, not the actual result
    StellarResult result = executor.execute("?TO_STRING");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertNotNull(result.getValue().get());
  }

  @Test
  public void testBadDocCommand() {
    StellarResult result = executor.execute("?INVALID");
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }

  @Test
  public void testQuit() {
    StellarResult result = executor.execute("quit");
    assertTrue(result.isTerminate());
  }

  @Test
  public void testAssign() {
    {
      // this is how variables get loaded into the REPL directly from a file
      executor.assign("x", 10, Optional.empty());
    }
    {
      StellarResult result = executor.execute("x + 2");
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());
      assertEquals(12, result.getValue().get());
    }
  }

  @Test
  public void testNotifyVariableListeners() {
    notified = false;
    executor.addVariableListener((var, value) -> {
      assertEquals("x", var);
      assertEquals(4, value.getResult());
      notified = true;
    });

    executor.execute("x := 2 + 2");
    assertTrue(notified);
  }

  @Test
  public void testNotifySpecialListeners() throws Exception {

    // setup an executor
    notified = false;
    Properties props = new Properties();
    DefaultStellarShellExecutor executor = new DefaultStellarShellExecutor(props, Optional.empty());

    // setup listener
    notified = false;
    executor.addSpecialListener((magic) -> {
      assertNotNull(magic);
      assertNotNull(magic.getCommand());
      notified = true;
    });

    // initialize... magics should be setup during initialization
    executor.init();
    assertTrue(notified);
  }

  @Test
  public void testNotifyFunctionListeners() throws Exception {
    // setup an executor
    notified = false;
    Properties props = new Properties();
    DefaultStellarShellExecutor executor = new DefaultStellarShellExecutor(props, Optional.empty());

    // setup listener
    notified = false;
    executor.addFunctionListener((fn) -> {
      assertNotNull(fn);
      assertNotNull(fn.getName());
      assertNotNull(fn.getFunction());
      notified = true;
    });

    // initialize... magics should be setup during initialization
    executor.init();
    assertTrue(notified);
  }

  @Test
  public void testEmptyInput() {
    StellarResult result = executor.execute("");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals("", result.getValue().get());
  }

  @Test
  public void testComment() {
    StellarResult result = executor.execute("# this is a comment");
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals("", result.getValue().get());
  }

  /**
   * If the executor is initialized without a connection to Zookeeper, the globals should be
   * defined, but empty.  This allows a user to '%define' their own with magic commands even
   * without Zookeeper.
   */
  @Test
  public void testEmptyGlobalsWithNoZookeeper() {
    assertNotNull(executor.getGlobalConfig());
    assertEquals(0, executor.getGlobalConfig().size());
  }
}
