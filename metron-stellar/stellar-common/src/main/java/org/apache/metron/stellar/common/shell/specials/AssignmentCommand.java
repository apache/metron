/*
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
package org.apache.metron.stellar.common.shell.specials;

import org.apache.metron.stellar.common.StellarAssignment;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;

import java.util.Optional;
import java.util.function.Function;

import static org.apache.metron.stellar.common.shell.StellarResult.error;

/**
 * A special command that allows for variable assignment.  Variable
 * assignment is not implemented directly within Stellar.
 *
 *    x := 2 + 2
 */
public class AssignmentCommand implements SpecialCommand {

  public static final String ASSIGNMENT_OP = ":=";

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> StellarAssignment.isAssignment(input);
  }

  @Override
  public String getCommand() {
    return ASSIGNMENT_OP;
  }

  /**
   * Handles variable assignment.
   * @param input The assignment expression to execute.
   * @param executor A stellar execution environment.
   * @return
   */
  @Override
  public StellarResult execute(String input, StellarShellExecutor executor) {
    assert StellarAssignment.isAssignment(input);

    // extract the variable and assignment expression
    StellarAssignment assignment = StellarAssignment.from(input);
    String varName = assignment.getVariable();
    String varExpr = assignment.getStatement();

    // execute the stellar expression
    StellarResult result = executor.execute(varExpr);
    if(result.isSuccess()) {

      Object value = null;
      if(result.getValue().isPresent()) {
        value = result.getValue().get();

      } else if(result.isValueNull()) {
        value = null;
      }

      // variable assignment
      executor.assign(varName, value, Optional.of(varExpr));
      return result;

    } else {
      return result;
    }
  }
}
