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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class MagicUndefineGlobalTest {

  MagicUndefineGlobal magic;
  DefaultStellarShellExecutor executor;

  @BeforeEach
  public void setup() throws Exception {

    // setup the %magic
    magic = new MagicUndefineGlobal();

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testGetCommand() {
    assertEquals("%undefine", magic.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "%undefine",
            "   %undefine   ",
            "%undefine   FOO",
            "    %undefine    FOO "
    );
    for(String in : inputs) {
      assertTrue(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void testShouldNotMatch() {
    List<String> inputs = Arrays.asList(
            "foo",
            "  undefine ",
            "bar",
            "%define"
    );
    for(String in : inputs) {
      assertFalse(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void testUndefine() {
    // define a global
    executor.getGlobalConfig().put("global", 22);
    assertEquals(1, executor.getGlobalConfig().size());

    // use the magic to undefine the global variable
    StellarResult result = magic.execute("%undefine global", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // ensure that the global variable does not exist
    assertEquals(0, executor.getGlobalConfig().size());
  }

  @Test
  public void testWithNoVariable() {
    // define a global
    executor.getGlobalConfig().put("global", 22);
    assertEquals(1, executor.getGlobalConfig().size());

    // no arg specifying the var to undefine
    StellarResult result = magic.execute("%undefine", executor);

    // validate the result
    assertTrue(result.isError());
    assertTrue(result.getException().isPresent());

    // ensure that the global variables were not changed
    assertEquals(1, executor.getGlobalConfig().size());
  }
}
