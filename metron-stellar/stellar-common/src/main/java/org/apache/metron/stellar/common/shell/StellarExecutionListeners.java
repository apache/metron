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

import org.apache.metron.stellar.common.shell.specials.SpecialCommand;
import org.apache.metron.stellar.dsl.StellarFunctionInfo;

/**
 * A listener will be notified about events that occur during the
 * execution of Stellar expressions.
 */
public class StellarExecutionListeners {

  /**
   * A listener that is notified when a function is defined.
   */
  public interface FunctionDefinedListener {
    void whenFunctionDefined(StellarFunctionInfo functionInfo);
  }

  /**
   * A listener that is notified when a variable is defined or redefined.
   */
  public interface VariableDefinedListener {
    void whenVariableDefined(String variableName, VariableResult result);
  }

  /**
   * A listener that is notified when a special command is defined.
   */
  public interface SpecialDefinedListener {
    void whenSpecialDefined(SpecialCommand magic);
  }
}
