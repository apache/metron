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

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;

import java.util.Map;
import java.util.Optional;

/**
 * Responsible for executing Stellar in a shell-like environment.
 *
 * Provides the additional capabilities expected of executing Stellar
 * in a shell-like environment including maintaining state, variable assignment,
 * magic commands, doc strings, and comments.
 */
public interface StellarShellExecutor extends StellarExecutionNotifier {

  /**
   * Initialize the Stellar executor.
   */
  void init();

  /**
   * Execute the Stellar expression.
   * @param expression The Stellar expression to execute.
   * @return The result of executing the Stellar expression.
   */
  StellarResult execute(String expression);

  /**
   * Update the state of the executor by assign a value to a variable.
   * @param variable The name of the variable.
   * @param value The value to assign.
   * @param expression The expression that resulted in the given value.  Optional.
   */
  void assign(String variable, Object value, Optional<String> expression);

  /**
   * The current state of the Stellar execution environment.
   */
  Map<String, VariableResult> getState();

  /**
   * Returns the Context for the Stellar execution environment.
   * @return The execution context.
   */
  Context getContext();

  /**
   * Returns the global configuration of the Stellar execution environment.
   * @return A map of values defined in the global configuration.
   */
  Map<String, Object> getGlobalConfig();

  /**
   * Returns the function resolver of the Stellar execution environment.
   * @return The function resolver.
   */
  FunctionResolver getFunctionResolver();
}
