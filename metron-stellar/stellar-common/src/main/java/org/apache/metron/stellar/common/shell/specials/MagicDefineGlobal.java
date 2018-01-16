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

import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.StellarAssignment;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;

import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.metron.stellar.common.shell.StellarResult.error;
import static org.apache.metron.stellar.common.shell.StellarResult.success;

/**
 * Allows a variable to be defined (or redefined) a within the global configuration.
 *
 *   %define newVar := newValue
 */
public class MagicDefineGlobal implements SpecialCommand {

  public static final String MAGIC_DEFINE = "%define";

  @Override
  public String getCommand() {
    return MAGIC_DEFINE;
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> startsWith(trimToEmpty(input), MAGIC_DEFINE);
  }

  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {

    // grab the expression in '%define <assign-expression>'
    String assignExpr = StringUtils.trimToEmpty(command.substring(MAGIC_DEFINE.length()));
    if(StringUtils.length(assignExpr) < 1) {
      return error(MAGIC_DEFINE + " missing assignment expression");
    }

    // the expression must be an assignment
    if(!StellarAssignment.isAssignment(assignExpr)) {
      return error(MAGIC_DEFINE + " expected assignment expression");
    }

    // execute the expression
    StellarAssignment expr = StellarAssignment.from(assignExpr);
    StellarResult result = executor.execute(expr.getStatement());

    // execution must be successful
    if(!result.isSuccess()) {
      return error(MAGIC_DEFINE + " expression execution failed");
    }

    // expression should have a result
    if(!result.getValue().isPresent()) {
      return error(MAGIC_DEFINE + " expression produced no result");
    }

    // alter the global configuration
    Object value = result.getValue().get();
    executor.getGlobalConfig().put(expr.getVariable(), value);

    return success(value);
  }
}
