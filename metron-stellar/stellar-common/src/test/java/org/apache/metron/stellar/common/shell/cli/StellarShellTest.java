package org.apache.metron.stellar.common.shell.cli;

import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.operator.ControlOperator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class StellarShellTest {

  StellarShell stellarShell;
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  ByteArrayOutputStream err = new ByteArrayOutputStream();

  @Before
  public void setup() throws Exception {

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
   * @return The data written to stdout during the test.
   */
  private String stdout() {
    return out.toString().replace(System.lineSeparator(), "");
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
}
