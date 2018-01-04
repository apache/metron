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
package org.apache.metron.stellar.zeppelin;

import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the StellarInterpreter.
 */
public class StellarInterpreterTest {

  StellarInterpreter interpreter;
  InterpreterContext context;

  @Before
  public void setup() {
    Properties props = new Properties();
    interpreter = new StellarInterpreter(props);
    interpreter.open();

    context = mock(InterpreterContext.class);
  }

  /**
   * Ensure that we can run Stellar code in the interpreter.
   */
  @Test
  public void testExecuteStellar() {
    InterpreterResult result = interpreter.interpret("2 + 2", context);

    // validate the result
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(1, result.message().size());

    // validate the message
    InterpreterResultMessage message = result.message().get(0);
    assertEquals("4", message.getData());
    assertEquals(InterpreterResult.Type.TEXT, message.getType());
  }

  /**
   * Ensure that Stellar lists are displayed correctly in Zeppelin.
   */
  @Test
  public void testExecuteWithStellarList() {
    final String expected = "[1, 2, 3, 4, 5]";
    InterpreterResult result = interpreter.interpret("[1,2,3,4,5]", context);

    // validate the result
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(1, result.message().size());

    // validate the message
    InterpreterResultMessage message = result.message().get(0);
    assertEquals(expected, message.getData());
    assertEquals(InterpreterResult.Type.TEXT, message.getType());
  }

  /**
   * Ensure that Stellar maps are displayed correctly in Zeppelin.
   */
  @Test
  public void testExecuteWithStellarMap() {
    final String expected = "{foo=2, key=val}";
    InterpreterResult result = interpreter.interpret("{ 'foo':2, 'key':'val' }", context);

    // validate the result
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(1, result.message().size());

    // validate the message
    InterpreterResultMessage message = result.message().get(0);
    assertEquals(expected, message.getData());
    assertEquals(InterpreterResult.Type.TEXT, message.getType());
  }

  /**
   * Ensure that 'bad' Stellar code is handled correctly by the interpreter.
   */
  @Test
  public void testExecuteBadStellar() {
    InterpreterResult result = interpreter.interpret("2 + ", context);

    // validate the result
    assertEquals(InterpreterResult.Code.ERROR, result.code());
    assertEquals(1, result.message().size());

    // validate the message
    InterpreterResultMessage message = result.message().get(0);
    assertTrue(message.getData().length() > 0);
    assertEquals(InterpreterResult.Type.TEXT, message.getType());
  }

  /**
   * The interpreter should handle if no value is returned.  Some Stellar expressions
   * will result in no value.
   */
  @Test
  public void testExecuteNoop() {

    // x is undefined and will have no result
    InterpreterResult result = interpreter.interpret("x", context);

    // validate the result
    assertEquals(InterpreterResult.Code.SUCCESS, result.code());
    assertEquals(1, result.message().size());

    // validate the message
    InterpreterResultMessage message = result.message().get(0);
    assertEquals(0, message.getData().length());
    assertEquals(InterpreterResult.Type.TEXT, message.getType());
  }

}
