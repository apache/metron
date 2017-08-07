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
package org.apache.metron.statistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class BinFunctionsTest {
  public static Object run(String rule, Map<String, Object> variables) {
    Context context = Context.EMPTY_CONTEXT();
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testBin() {
    Assert.assertEquals(run("BIN(value, bounds)", ImmutableMap.of("value", 0, "bounds", ImmutableList.of(10, 20, 30))), 0);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 0)), 0);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 9)), 0);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 10)), 0);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 11)), 1);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 19)), 1);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 21)), 2);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 29)), 2);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 31)), 3);
    Assert.assertEquals(run("BIN(value, [ 10, 20, 30 ])", ImmutableMap.of("value", 1000)), 3);
  }
}
