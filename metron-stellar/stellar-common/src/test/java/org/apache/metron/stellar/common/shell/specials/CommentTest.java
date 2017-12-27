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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommentTest {

  Comment magic;
  DefaultStellarShellExecutor executor;

  @Before
  public void setup() throws Exception {

    // setup the %magic
    magic = new Comment();

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testGetCommand() {
    assertEquals("#", magic.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "#comment",
            "   #   comment   ",
            "      #comment"
    );
    for(String in : inputs) {
      assertTrue("failed: " + in, magic.getMatcher().apply(in));
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
      assertFalse("failed: " + in, magic.getMatcher().apply(in));
    }
  }

  @Test
  public void testComment() {
    StellarResult result = magic.execute("#  this is a comment ", executor);

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());
    assertEquals("", result.getValue().get());
  }
}
