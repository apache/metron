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

package org.apache.metron.common.stellar.shell;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.dsl.Context;

import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

/**
 * A REPL environment for Stellar.
 *
 * Useful for debugging Stellar expressions.
 */
public class StellarShell {

  private static final String WELCOME = "Stellar, Go!";
  private static final String EXPRESSION_PROMPT = ">>> ";
  private static final String RESULT_PROMPT = "[=] ";
  private static final String ERROR_PROMPT = "[!] ";
  private static final String ASSIGN_PROMPT = "[?] save as: ";
  private static final String MAGIC_PREFIX = "%";
  private static final String MAGIC_FUNCTIONS = "%functions";
  private static final String MAGIC_VARS = "%vars";

  private StellarExecutor executor;

  /**
   * Execute the Stellar REPL.
   */
  public static void main(String[] args) throws Exception {
    StellarShell shell = new StellarShell(args);
    shell.execute();
  }

  /**
   * Create a Stellar REPL.
   * @param args The commmand-line arguments.
   */
  public StellarShell(String[] args) throws Exception {

    // define valid command-line options
    Options options = new Options();
    options.addOption("z", "zookeeper", true, "Zookeeper URL");
    options.addOption("h", "help", false, "Print help");

    CommandLineParser parser = new PosixParser();
    CommandLine commandLine = parser.parse(options, args);

    // print help
    if(commandLine.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("stellar", options);
      System.exit(0);
    }

    // create the executor
    if(commandLine.hasOption("z")) {
      String zookeeperUrl = commandLine.getOptionValue("z");
      executor = new StellarExecutor(zookeeperUrl);

    } else {
      executor = new StellarExecutor();
    }
  }

  /**
   * Handles the main loop for the REPL.
   */
  public void execute() {

    // welcome message
    writeLine(WELCOME);
    executor.getContext()
            .getCapability(Context.Capabilities.GLOBAL_CONFIG)
            .ifPresent(conf -> writeLine(conf.toString()));

    Scanner scanner = new Scanner(System.in);
    boolean done = false;
    while(!done) {

      // prompt the user for an expression
      write(EXPRESSION_PROMPT);
      String expression = scanner.nextLine();
      if(StringUtils.isNotBlank(expression)) {

        if(isMagic(expression)) {
          handleMagic(scanner, expression);

        } else {
          handleStellar(scanner, expression);
        }

      } else {
        // if no expression, show all variables
        writeLine(format("%d variable(s) defined", executor.getVariables().size()));
        executor.getVariables()
                .forEach((k,v) -> writeLine(format("%s = %s", k, v)));
      }
    }
  }

  /**
   * Handles user interaction when executing a Stellar expression.
   * @param scanner The scanner used to read user input.
   * @param expression The expression to execute.
   */
  private void handleStellar(Scanner scanner, String expression) {

    Object result = executeStellar(expression);
    if(result != null) {

      // echo the result
      write(RESULT_PROMPT);
      writeLine(result.toString());

      // assign the result to a variable?
      write(ASSIGN_PROMPT);
      String variable = scanner.nextLine();
      if(StringUtils.isNotBlank(variable)) {
        executor.assign(variable, result);
      }
    }
  }

  /**
   * Handles user interaction when executing a Magic command.
   * @param scanner The scanner used to read user input.
   * @param expression The expression to execute.
   */
  private void handleMagic(Scanner scanner, String expression) {

    if(MAGIC_FUNCTIONS.equals(expression)) {

      // list all functions
      String functions = StreamSupport
              .stream(executor.getFunctionResolver().getFunctions().spliterator(), false)
              .sorted()
              .collect(Collectors.joining(", "));
      writeLine(functions);

    } else if(MAGIC_VARS.equals(expression)) {

      // list all variables
      executor.getVariables()
              .forEach((k,v) -> writeLine(format("%s = %s", k, v)));

    } else {
      writeLine(ERROR_PROMPT + "undefined magic command: " + expression);
    }
  }

  /**
   * Is a given expression a built-in magic?
   * @param expression The expression.
   * @return
   */
  private boolean isMagic(String expression) {
    return StringUtils.startsWith(expression, MAGIC_PREFIX);
  }

  /**
   * Executes a Stellar expression.
   * @param expression The expression to execute.
   * @return The result of the expression.
   */
  private Object executeStellar(String expression) {
    Object result = null;

    try {
      result = executor.execute(expression);

    } catch(Throwable t) {
      writeLine(ERROR_PROMPT + t.getMessage());
      t.printStackTrace();
    }

    return result;
  }

  private void write(String out) {
    System.out.print(out);
  }

  private void writeLine(String out) {
    System.out.println(out);
  }
}
