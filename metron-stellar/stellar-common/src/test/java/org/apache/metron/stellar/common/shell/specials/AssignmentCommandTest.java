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
package org.apache.metron.stellar.common.shell.specials;

import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;


/**
 * Tests the AssignmentCommand class.
 */
public class AssignmentCommandTest {

  AssignmentCommand command;
  StellarShellExecutor executor;

  @Before
  public void setup() throws Exception {
    command = new AssignmentCommand();

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testGetCommand() {
    assertEquals(":=", command.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "x := 2 + 2",
            "   x      :=      2     +  2   ",
            "  x    :=    2",
            " x := "
    );
    for(String in : inputs) {
      assertTrue("failed: " + in, command.getMatcher().apply(in));
    }
  }

  @Test
  public void testShouldNotMatch() {
    List<String> inputs = Arrays.asList(
            "2+2",
            " %define x := 2",
            "x"
    );
    for(String in : inputs) {
      assertFalse("failed: " + in, command.getMatcher().apply(in));
    }
  }

  @Test
  public void testAssignment() {
    StellarResult result = command.execute("x := 2 + 2", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(4, result.getValue().get());

    // validate assignment
    assertEquals(4, executor.getState().get("x").getResult());
  }

  @Test
  public void testAssignments() {

    // execute a series of assignments
    assertTrue(command.execute("x := 2 + 2", executor).isSuccess());
    assertTrue(command.execute("y := 2 + x", executor).isSuccess());
    assertTrue(command.execute("z := x + y", executor).isSuccess());

    // validate assignment
    assertEquals(4, executor.getState().get("x").getResult());
    assertEquals(6, executor.getState().get("y").getResult());
    assertEquals(10, executor.getState().get("z").getResult());
  }

  @Test
  public void testReassignment() {

    // execute a series of assignments
    assertTrue(command.execute("x := 2 + 2", executor).isSuccess());
    assertTrue(command.execute("x := 5 + 5", executor).isSuccess());

    // validate assignment
    assertEquals(10, executor.getState().get("x").getResult());
  }

  @Test
  public void testAssignmentOfEmptyVar() {

    // z is not defined
    StellarResult result = command.execute("x := z", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.isValueNull());
    assertFalse(result.getValue().isPresent());

    // the value of x is null
    assertNull(executor.getState().get("x").getResult());
  }

  @Test
  public void testBadAssignmentExpr() {
    StellarResult result = command.execute("x := 2 + ", executor);

    // validate the result
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());

    // no assignment should have happened
    assertFalse(executor.getState().containsKey("x"));
  }

  @Test
  public void testAssignNull() {
    StellarResult result = command.execute("x := NULL", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.isValueNull());

    // validate assignment
    assertNull(executor.getState().get("x").getResult());
  }

  /**
   * Assignment with no expression results in an empty string.  Is this
   * what we would expect?
   */
  @Test
  public void testNoAssignmentExpr() {
    StellarResult result = command.execute("x := ", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // validate assignment
    assertEquals("", executor.getState().get("x").getResult());
  }

  @Test
  public void testAssignmentWithVar() {

    // define a variable
    executor.assign("x", 10, Optional.empty());

    // execute the assignment expression
    StellarResult result = command.execute("y := x + 2", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(12, result.getValue().get());

    // validate assignment
    assertEquals(10, executor.getState().get("x").getResult());
    assertEquals(12, executor.getState().get("y").getResult());
  }

  @Test
  public void testAssignmentWithOddWhitespace() {

    StellarResult result = command.execute("        x   :=    2 +      2", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals(4, result.getValue().get());

    // validate assignment
    assertEquals(4, executor.getState().get("x").getResult());
  }
}
