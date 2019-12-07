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
package org.apache.metron.stellar.common.shell.specials;

import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.dsl.functions.StringFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocCommandTest {

  DocCommand command;
  DefaultStellarShellExecutor executor;

  @BeforeEach
  public void setup() throws Exception {

    // setup the command
    command = new DocCommand();

    // setup a function resolver - only 3 functions have been defined
    SimpleFunctionResolver functionResolver = new SimpleFunctionResolver()
            .withClass(StringFunctions.ToString.class)
            .withClass(StringFunctions.ToLower.class)
            .withClass(StringFunctions.ToUpper.class);

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(functionResolver, props, Optional.empty());
    executor.init();
  }

  @Test
  public void testWithFunction() {
    StellarResult result = command.execute("?TO_STRING", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // validate that we have some sort of doc string
    assertTrue(result.getValue().toString().length() > 0);
  }

  @Test
  public void testFunctionNotDefined() {
    StellarResult result = command.execute("?INVALID", executor);

    // validate the result
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }

  @Test
  public void testNoFunction() {
    StellarResult result = command.execute("?", executor);

    // validate the result
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());
  }
}
