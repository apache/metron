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
import org.apache.metron.stellar.dsl.functions.StringFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MagicListFunctionsTest {

  MagicListFunctions magic;
  DefaultStellarShellExecutor executor;

  @Before
  public void setup() throws Exception {

    // setup the %magic
    magic = new MagicListFunctions();

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
  public void testGetCommand() {
    assertEquals("%functions", magic.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "%functions",
            "   %functions   ",
            "%functions FOO",
            "    %functions    FOO "
    );
    for(String in : inputs) {
      assertTrue("failed: " + in, magic.getMatcher().apply(in));
    }
  }

  @Test
  public void testShouldNotMatch() {
    List<String> inputs = Arrays.asList(
            "foo",
            "  functions ",
            "bar",
            "%define"
    );
    for(String in : inputs) {
      assertFalse("failed: " + in, magic.getMatcher().apply(in));
    }
  }

  @Test
  public void testFunctions() {
    StellarResult result = magic.execute("%functions", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // there are 3 functions that should be returned
    String value = ConversionUtils.convert(result.getValue().get(), String.class);
    String[] functions = value.split(", ");
    assertEquals(3, functions.length);
  }

  @Test
  public void testFunctionsWithMatch() {
    StellarResult result = magic.execute("%functions UPPER", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // only 1 function; TO_UPPER should be returned
    String value = ConversionUtils.convert(result.getValue().get(), String.class);
    String[] functions = value.split(", ");
    assertEquals(1, functions.length);
    assertEquals("TO_UPPER", functions[0]);
  }

  @Test
  public void testFunctionsWithNoMatch() {
    StellarResult result = magic.execute("%functions NOMATCH", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // no functions should be returned
    String value = ConversionUtils.convert(result.getValue().get(), String.class);
    String[] functions = value.trim().split(", ");
    assertEquals(1, functions.length);
    assertEquals("", functions[0]);
  }
}
