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

/**
 * Notifies listeners when events occur during the execution of Stellar expressions.
 */
public interface StellarExecutionNotifier {

  /**
   * Add a listener that will be notified when a magic command is defined.
   * @param listener The listener to notify.
   */
  void addSpecialListener(StellarExecutionListeners.SpecialDefinedListener listener);

  /**
   * Add a listener that will be notified when a function is defined.
   * @param listener The listener to notify.
   */
  void addFunctionListener(StellarExecutionListeners.FunctionDefinedListener listener);

  /**
   * Add a listener that will be notified when a variable is defined.
   * @param listener The listener to notify.
   */
  void addVariableListener(StellarExecutionListeners.VariableDefinedListener listener);
}
