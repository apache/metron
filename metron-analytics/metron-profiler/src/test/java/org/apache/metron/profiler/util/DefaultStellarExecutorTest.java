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

package org.apache.metron.profiler.util;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests the DefaultStellarExecutor.
 */
public class DefaultStellarExecutorTest {

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20"
   * }
   */
  @Multiline
  private String input;

  private JSONObject message;
  private DefaultStellarExecutor executor;

  @Before
  public void setup() throws ParseException {

    // parse the input message
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(input);

    // create the executor to test
    executor = new DefaultStellarExecutor();
  }

  /**
   * Ensure that a value can be assigned to a variable.
   */
  @Test
  public void testAssign() {
    executor.assign("foo", "2", message, Context.EMPTY_CONTEXT());

    // verify
    Object var = executor.getState().get("foo");
    assertThat(var, instanceOf(Integer.class));
    assertThat(var, equalTo(2));
  }

  /**
   * Ensure that a variable can be resolved from a message field.
   */
  @Test
  public void testAssignWithVariableResolution() {
    executor.assign("foo", "ip_src_addr", message, Context.EMPTY_CONTEXT());

    // verify
    Object var = executor.getState().get("foo");
    assertThat(var, instanceOf(String.class));
    assertThat(var, equalTo("10.0.0.1"));
  }

  /**
   * Ensure that state is maintained correctly in the execution environment.
   */
  @Test
  public void testState() {
    executor.assign("two", "2", message, Context.EMPTY_CONTEXT());
    executor.assign("four", "4", message, Context.EMPTY_CONTEXT());
    executor.assign("sum", "two + four", message, Context.EMPTY_CONTEXT());

    // verify
    Object var = executor.getState().get("sum");
    assertEquals(6.0, var);
  }

  /**
   * Ensure that state is maintained correctly in the execution environment.
   */
  @Test
  public void testClearState() {
    executor.assign("two", "2", message, Context.EMPTY_CONTEXT());
    executor.clearState();

    // verify
    assertThat(executor.getState().containsKey("two"), equalTo(false));
  }

  /**
   * Ensure that a Transformation function can be executed.
   *
   * There are two sets of functions in Stellar currently.  One can be executed with
   * a PredicateProcessor and the other a TransformationProcessor.  The StellarExecutor
   * abstracts away that complication.
   */
  @Test
  public void testExecuteTransformation() {
    String actual = executor.execute("TO_UPPER('lowercase')", message, String.class, Context.EMPTY_CONTEXT());
    assertThat(actual, equalTo("LOWERCASE"));
  }

  /**
   * Ensure that a Predicate function can be executed.
   *
   * There are two sets of functions in Stellar currently.  One can be executed with
   * a PredicateProcessor and the other a TransformationProcessor.  The StellarExecutor
   * abstracts away that complication.
   */
  @Test
  public void testExecutePredicate() {
    boolean actual = executor.execute("IS_INTEGER(2)", message, Boolean.class, Context.EMPTY_CONTEXT());
    assertThat(actual, equalTo(true));
  }

  /**
   * An exception is expected if an expression results in an unexpected type.
   */
  @Test(expected = RuntimeException.class)
  public void testExecuteWithWrongType() {
    executor.execute("2 + 2", message, Boolean.class, Context.EMPTY_CONTEXT());
  }

  /**
   * A best effort should be made to do sensible type conversions.
   */
  @Test
  public void testExecuteWithTypeConversion() {
    executor.execute("2", message, Double.class, Context.EMPTY_CONTEXT());
    executor.execute("2", message, Float.class, Context.EMPTY_CONTEXT());
    executor.execute("2", message, Short.class, Context.EMPTY_CONTEXT());
    executor.execute("2", message, Long.class, Context.EMPTY_CONTEXT());
  }
}
