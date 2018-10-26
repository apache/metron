/**
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

package org.apache.metron.stellar.dsl;


/**
 * VariableResolver implementors provide support variable operations.
 * <ul>
 *   <li>Verifying the exists of a variable by name</li>
 *   <li>Returning the value of a variable by name</li>
 *   <li>Updating the value of a variable by name</li>
 * </ul>
 */
public interface VariableResolver {
  public static final String ALL_FIELDS = "_";

  /**
   * Returns the value of a variable.
   * @param variable the variable name
   * @return the value Object
   */
  Object resolve(String variable);

  /**
   * Returns the existance of the variable.
   * @param variable the variable name
   * @return true if the variable exists, false otherwise
   */
  boolean exists(String variable);

  /**
   * Updates the value of a variable.
   * @param variable the variable name
   * @param value the value to update with
   */
  void update(String variable, Object value);
}
