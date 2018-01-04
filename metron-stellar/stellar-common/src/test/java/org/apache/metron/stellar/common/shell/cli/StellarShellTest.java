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
package org.apache.metron.stellar.common.shell.cli;

import com.google.common.collect.Iterables;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.AeshContext;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.operator.ControlOperator;
import org.jboss.aesh.console.settings.DefaultAeshContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests the StellarShell class.
 */
public class StellarShellTest {

  private StellarShell stellarShell;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @Before
  public void setup() throws Exception {

    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();

    // setup streams so that we can capture stdout
    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));

    String[] args = new String[0];
    stellarShell = new StellarShell(args);
  }

  @After
  public void cleanUp() {
    System.setOut(null);
    System.setErr(null);
  }

  /**
   * @return The data written to stdout during the test (with newlines stripped out to simplify comparisons.)
   */
  private String stdout() {
    return out.toString().replace(System.lineSeparator(), "");
  }

  /**
   * @return The data written to stdout during the test.
   */
  private String stdoutWithNewlines() {
    return out.toString();
  }


  /**
   * @return The data written to stderr during the test.
   */
  private String stderr() {
    return err.toString().replace(System.lineSeparator(), "");
  }

  /**
   * @param buffer
   * @return A ConsoleOperation that that StellarShell uses to drive input.
   */
  private ConsoleOperation createOp(String buffer) {
    return new ConsoleOperation(ControlOperator.APPEND_OUT, buffer);
  }

  @Test
  public void testExecuteStellar() throws Exception {
    stellarShell.execute(createOp("2 + 2"));
    assertEquals("4", stdout());
  }

  /**
   * Ensure that Stellar lists are displayed correctly in the REPL.
   */
  @Test
  public void testExecuteWithStellarList() throws Exception {
    stellarShell.execute(createOp("[1,2,3,4,5]"));
    assertEquals("[1, 2, 3, 4, 5]", stdout());
  }

  /**
   * Ensure that Stellar maps are displayed correctly in the REPL.
   */
  @Test
  public void testExecuteWithStellarMap() throws Exception {
    stellarShell.execute(createOp("{ 'foo':2, 'key':'val' }"));
    assertEquals("{foo=2, key=val}", stdout());
  }

  /**
   * Ensure that 'bad' Stellar code is handled correctly by the REPL.
   */
  @Test
  public void testExecuteBadStellar() throws Exception {
    stellarShell.execute(createOp("2 + "));
    final String expected = "[!] Unable to parse: 2 + ";
    assertTrue(stdout().startsWith(expected));
  }

  /**
   * The REPL should handle if no value is returned.  Some Stellar expressions
   * will result in no value.
   */
  @Test
  public void testExecuteNoop() throws Exception {
    stellarShell.execute(createOp("x"));
    assertEquals("", stdout());
  }

  /**
   * The REPL should handle if the user chooses to quit.
   */
  @Test
  public void testQuit() throws Exception {
    stellarShell.execute(createOp("quit"));

    // the console should not be running
    assertFalse(stellarShell.getConsole().isRunning());
  }

  /**
   * The REPL should handle if the user chooses to quit.
   */
  @Test
  public void testStart() throws Exception {

    StellarShell.main(new String[0]);

    // we should see the welcome prompt
    assertTrue(stdoutWithNewlines().contains(StellarShell.WELCOME));
  }

  /**
   * The REPL should support auto-completion.
   */
  @Test
  public void testAutoComplete() throws Exception {

    // the user's input that needs auto-completed
    final String buffer = "TO_";

    // the cursor is at the end of the buffer
    int cursor = buffer.length();

    // ask the shell to auto-complete
    AeshContext context = new DefaultAeshContext();
    CompleteOperation op = new CompleteOperation(context, buffer, cursor);
    stellarShell.complete(op);

    // we should have some auto-complete candidates
    List<String> candidates = op.getFormattedCompletionCandidates();
    assertTrue(candidates.size() > 0);

    // validate each candidate
    for(String candidate: candidates) {
      String completion = buffer + candidate;

      // the auto-complete should include an open paren
      assertEquals("(", completion.substring(completion.length() - 1));

      // the candidate should be a valid, defined function
      String function = completion.substring(0, completion.length() - 1);
      Iterable<String> allFunctions = stellarShell.getExecutor().getFunctionResolver().getFunctions();
      String definedFunction = Iterables.find(allFunctions, (fn) -> fn.equals(function));
      assertEquals(function, definedFunction);
    }
  }
}
