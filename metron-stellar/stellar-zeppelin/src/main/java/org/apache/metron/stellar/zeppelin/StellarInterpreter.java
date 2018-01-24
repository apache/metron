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

import static org.apache.zeppelin.interpreter.InterpreterResult.Code.ERROR;
import static org.apache.zeppelin.interpreter.InterpreterResult.Code.SUCCESS;
import static org.apache.zeppelin.interpreter.InterpreterResult.Type.TEXT;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.stellar.common.shell.DefaultStellarAutoCompleter;
import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarAutoCompleter;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Zeppelin Interpreter for Stellar.
 */
public class StellarInterpreter extends Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Executes the Stellar expressions.
   *
   * <p>Zeppelin will handle isolation and how the same executor is or is not used across
   * multiple notebooks.  This is configurable by the user.
   *
   * <p>See https://zeppelin.apache.org/docs/latest/manual/interpreters.html#interpreter-binding-mode.
   */
  private StellarShellExecutor executor;

  /**
   * Handles auto-completion for Stellar expressions.
   */
  private StellarAutoCompleter autoCompleter;

  public StellarInterpreter(Properties properties) {
    super(properties);
    this.autoCompleter = new DefaultStellarAutoCompleter();
  }

  @Override
  public void open() {
    try {
      executor = createExecutor();

    } catch (Exception e) {
      LOG.error("Unable to create a StellarShellExecutor", e);
    }
  }

  @Override
  public void close() {
    // nothing to do
  }

  @Override
  public InterpreterResult interpret(final String input, InterpreterContext context) {
    InterpreterResult result;
    try {

      // execute the input
      StellarResult stellarResult = executor.execute(input);
      if(stellarResult.isSuccess()) {

        // on success - if no result, use a blank value
        Object value = stellarResult.getValue().orElse("");
        String text = value.toString();
        result = new InterpreterResult(SUCCESS, TEXT, text);

      } else if(stellarResult.isError()) {

        // an error occurred
        Optional<Throwable> e = stellarResult.getException();
        String message = getErrorMessage(e, input);
        result = new InterpreterResult(ERROR, TEXT, message);

      } else {

        // should never happen
        throw new IllegalStateException("Unexpected error. result=" + stellarResult);
      }

    } catch(Throwable t) {

      // unexpected exception
      String message = getErrorMessage(Optional.of(t), input);
      result = new InterpreterResult(ERROR, TEXT, message);
    }

    return result;
  }

  @Override
  public void cancel(InterpreterContext context) {
    // there is no way to cancel the execution of a Stellar expression
  }

  @Override
  public FormType getFormType() {
    return FormType.SIMPLE;
  }

  @Override
  public int getProgress(InterpreterContext context) {
    // unable to provide progress
    return 0;
  }

  @Override
  public List<InterpreterCompletion> completion(String buf, int cursor) {

    // use the autoCompleter to return a list of completes to Zeppelin
    List<InterpreterCompletion> completes = new ArrayList<>();
    for(String candidate : autoCompleter.autoComplete(buf)) {
      completes.add(new InterpreterCompletion(candidate, candidate));
    }

    return completes;
  }

  /**
   * Generates an error message that is shown to the user.
   *
   * @param e An optional exception that occurred.
   * @param input The user input that led to the error condition.
   * @return An error message for the user.
   */
  private String getErrorMessage(Optional<Throwable> e, String input) {
    String message;
    if(e.isPresent()) {

      // base the error message on the exception
      String error = ExceptionUtils.getRootCauseMessage(e.get());
      String trace = ExceptionUtils.getStackTrace(e.get());
      message = error + System.lineSeparator() + trace;

    } else {
      // no exception provided; create generic error message
      message = "Invalid expression: " + input;
    }

    return message;
  }

  /**
   * Create an executor that will run the Stellar code for the Zeppelin Notebook.
   * @return The stellar executor.
   */
  private StellarShellExecutor createExecutor() throws Exception {

    Properties props = getProperty();
    StellarShellExecutor executor = new DefaultStellarShellExecutor(props, Optional.empty());

    // register the auto-completer to be notified
    executor.addSpecialListener((magic) -> autoCompleter.addCandidateFunction(magic.getCommand()));
    executor.addFunctionListener((fn) -> autoCompleter.addCandidateFunction(fn.getName()));
    executor.addVariableListener((name, val) -> autoCompleter.addCandidateVariable(name));

    executor.init();
    return executor;
  }

  /**
   * Returns the executor used to execute Stellar expressions.
   * @return The executor of Stellar expressions.
   */
  public StellarShellExecutor getExecutor() {
    return executor;
  }
}
