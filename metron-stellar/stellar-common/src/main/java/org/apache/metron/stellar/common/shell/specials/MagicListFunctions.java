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
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * A MagicCommand that lists the functions available within
 * a Stellar execution environment.
 *
 *    %functions
 */
public class MagicListFunctions implements SpecialCommand {

  public static final String MAGIC_FUNCTIONS = "%functions";

  @Override
  public String getCommand() {
    return MAGIC_FUNCTIONS;
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> startsWith(trimToEmpty(input), MAGIC_FUNCTIONS);
  }

  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {

    // if '%functions FOO' then show only functions that contain 'FOO'
    String startsWith = StringUtils.trimToEmpty(command.substring(MAGIC_FUNCTIONS.length()));
    Predicate<String> nameFilter = (name -> true);
    if (StringUtils.isNotBlank(startsWith)) {
      nameFilter = (name -> name.contains(startsWith));
    }

    // '%functions' -> list all functions in scope
    String functions = StreamSupport
            .stream(executor.getFunctionResolver().getFunctionInfo().spliterator(), false)
            .map(info -> String.format("%s", info.getName()))
            .filter(nameFilter)
            .sorted()
            .collect(Collectors.joining(", "));

    return StellarResult.success(functions);
  }
}
