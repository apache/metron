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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.metron.stellar.common.shell.DefaultStellarAutoCompleter;
import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarAutoCompleter;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.apache.zeppelin.interpreter.InterpreterResult.Code.ERROR;
import static org.apache.zeppelin.interpreter.InterpreterResult.Code.SUCCESS;
import static org.apache.zeppelin.interpreter.InterpreterResult.Type.TEXT;

/**
 * A Zeppelin Interpreter for Stellar.
 */
public class StellarInterpreter extends Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Executes the Stellar expressions.
   *
   * Zeppelin will handle isolation and how the same executor is or is not used across
   * multiple notebooks.  This is configurable by the user.
   *
   * See https://zeppelin.apache.org/docs/latest/manual/interpreters.html#interpreter-binding-mode.
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

  public void open() {
    try {
      executor = createExecutor();

    } catch (Exception e) {
      LOG.error("Unable to create a StellarShellExecutor", e);
    }
  }

  public void close() {
    // nothing to do
  }

  public StellarShellExecutor createExecutor() throws Exception {

    Properties props = getProperty();
    StellarShellExecutor executor = new DefaultStellarShellExecutor(props, Optional.empty());

    // register the auto-completer to be notified
    executor.addSpecialListener((magic) -> autoCompleter.addCandidateFunction(magic.getCommand()));
    executor.addFunctionListener((fn) -> autoCompleter.addCandidateFunction(fn.getName()));
    executor.addVariableListener((name, val) -> autoCompleter.addCandidateVariable(name));

    executor.init();
    return executor;
  }

  public InterpreterResult interpret(String input, InterpreterContext context) {
    InterpreterResult result;

    try {
      // execute the input
      StellarResult stellarResult = executor.execute(input);

      if(stellarResult.isSuccess()) {
        // on success - if no result, use a blank value
        Object value = stellarResult.getValue().orElse("");
        String text = ConversionUtils.convert(value, String.class);
        result = new InterpreterResult(SUCCESS, TEXT, text);

      } else if(stellarResult.isError()) {
        // on error
        Throwable e = stellarResult.getException().get();
        String error = ExceptionUtils.getRootCauseMessage(e);
        String stack = ExceptionUtils.getStackTrace(e);
        result = new InterpreterResult(ERROR, TEXT, error + System.lineSeparator() + stack);

      } else {
        // should not happen
        throw new IllegalStateException("Unexpected Stellar result status. Please file a bug report.");
      }

    } catch(Throwable t) {
      // unexpected exception
      String message = ExceptionUtils.getRootCauseMessage(t);
      String stack = ExceptionUtils.getStackTrace(t);
      result = new InterpreterResult(ERROR, TEXT, message + System.lineSeparator() + stack);
    }

    return result;
  }

  public void cancel(InterpreterContext context) {
    // there is no way to cancel the execution of a Stellar expression
  }

  public FormType getFormType() {
    return FormType.SIMPLE;
  }

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
}
