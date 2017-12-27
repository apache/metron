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
import org.apache.metron.stellar.dsl.StellarFunctionInfo;

import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static org.apache.metron.stellar.common.shell.StellarResult.error;
import static org.apache.metron.stellar.common.shell.StellarResult.success;

/**
 * A special command that allows a user to request doc string
 * about a Stellar function.
 *
 * For example `?TO_STRING` will output the docs for the function `TO_STRING`
 */
public class DocCommand implements SpecialCommand {

  public static final String DOC_PREFIX = "?";

  @Override
  public String getCommand() {
    return DOC_PREFIX;
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> StringUtils.startsWith(input, DOC_PREFIX);
  }

  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {
    StellarResult result;

    // expect ?functionName
    String functionName = StringUtils.substring(command, 1);

    // grab any docs for the given function
    Spliterator<StellarFunctionInfo> fnIterator = executor.getFunctionResolver().getFunctionInfo().spliterator();
    Optional<StellarFunctionInfo> functionInfo = StreamSupport
            .stream(fnIterator, false)
            .filter(info -> StringUtils.equals(functionName, info.getName()))
            .findFirst();

    if(functionInfo.isPresent()) {
      result = success(docFormat(functionInfo.get()));
    } else {
      result = error(String.format("No docs available for function '%s'", functionName));
    }

    return result;
  }

  /**
   * Formats the Stellar function info object into a readable string.
   * @param info The stellar function info object.
   * @return A readable string.
   */
  private String docFormat(StellarFunctionInfo info) {
    StringBuffer docString = new StringBuffer();

    // name
    docString.append(info.getName() + "\n");

    // description
    docString.append(String.format("Description: %-60s\n\n", info.getDescription()));

    // params
    if(info.getParams().length > 0) {
      docString.append("Arguments:\n");
      for(String param : info.getParams()) {
        docString.append(String.format("\t%-60s\n", param));
      }
      docString.append("\n");
    }

    // returns
    docString.append(String.format("Returns: %-60s\n", info.getReturns()));

    return docString.toString();
  }
}
