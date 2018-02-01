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

import java.util.function.Function;

import static org.apache.metron.stellar.common.shell.StellarResult.terminate;

/**
 * A special command that allows the user to 'quit' their REPL session.
 *
 *    quit
 */
public class QuitCommand implements SpecialCommand {

  public static final String QUIT_COMMAND = "quit";

  @Override
  public String getCommand() {
    return QUIT_COMMAND;
  }

  @Override
  public Function<String, Boolean> getMatcher() {
    return (input) -> QUIT_COMMAND.equals(input);
  }

  @Override
  public StellarResult execute(String command, StellarShellExecutor executor) {
    return terminate();
  }
}
