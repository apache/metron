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

import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.shell.VariableResult;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.metron.stellar.common.shell.StellarResult.success;

/**
 * A MagicCommand that lists the variables available within
 * the Stellar execution environment.
 *
 *    %vars
 */
public class MagicListVariables implements SpecialCommand {

  public static final String MAGIC_VARS = "%vars";

  @Override
  public String getCommand() {
    return MAGIC_VARS;
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> startsWith(trimToEmpty(input), MAGIC_VARS);
  }

  /**
   * Lists each variable, its value, and if available, the expression that resulted in that value.
   *
   *    x = 4 via `2 + 2`
   *
   * @param command The command to execute.
   * @param executor A stellar execution environment.
   * @return
   */
  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {

    // format a string containing each variable and it's value
    String vars = executor
            .getState()
            .entrySet()
            .stream()
            .map(e -> format(e))
            .collect(Collectors.joining(", "));

    return success(vars);
  }

  private String format(Map.Entry<String, VariableResult> var) {

    // 'varName = varValue'
    String out = String.format("%s = %s", var.getKey(), var.getValue().getResult());

    // 'via varExpression', if the expression is known
    if(var.getValue().getExpression().isPresent()) {
      out += String.format(" via `%s`", var.getValue().getExpression().get());
    }

    return out;
  }
}
