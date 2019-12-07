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
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class MagicDefineGlobalTest {

  MagicDefineGlobal magic;
  DefaultStellarShellExecutor executor;

  @BeforeEach
  public void setup() throws Exception {

    // setup the %magic
    magic = new MagicDefineGlobal();

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testGetCommand() {
    assertEquals("%define", magic.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "%define",
            "   %define   ",
            "%define x := 2",
            "    %define   x := 2 "
    );
    for(String in : inputs) {
      assertTrue(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void testShouldNotMatch() {
    List<String> inputs = Arrays.asList(
            "foo",
            "  define ",
            "bar"
    );
    for(String in : inputs) {
      assertFalse(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void testDefine() {
    final int expected = 4;

    {
      StellarResult result = magic.execute("%define global := 2 + 2", executor);

      // validate the result
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());
      assertEquals(expected, result.getValue().get());

      // ensure global config was updated
      assertTrue(executor.getGlobalConfig().containsKey("global"));
      assertEquals(expected, executor.getGlobalConfig().get("global"));
    }
    //
    {
      // get all globals
      StellarResult result = executor.execute("%globals");

      // validate the result
      assertTrue(result.isSuccess());
      assertTrue(result.getValue().isPresent());

      String out = ConversionUtils.convert(result.getValue().get(), String.class);
      assertEquals("{global=4}", out);
    }
  }

  @Test
  public void testNotAssignmentExpression() {
    StellarResult result = magic.execute("%define 2 + 2", executor);

    // validate the result
    assertTrue(result.isError());
    assertFalse(result.getValue().isPresent());
    assertTrue(result.getException().isPresent());

    // the global config should not have changed
    assertEquals(0, executor.getGlobalConfig().size());
  }

  @Test
  public void testMissingExpression() {
    StellarResult result = magic.execute("%define", executor);

    // validate the result
    assertTrue(result.isError());
    assertFalse(result.getValue().isPresent());
    assertTrue(result.getException().isPresent());

    // the global config should not have changed
    assertEquals(0, executor.getGlobalConfig().size());
  }
}
