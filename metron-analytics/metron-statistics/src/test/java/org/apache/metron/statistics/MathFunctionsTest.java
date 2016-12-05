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

import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.stellar.StellarProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class MathFunctionsTest {
  public static Object run(String rule, Map<String, Object> variables) {
    Context context = Context.EMPTY_CONTEXT();
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, x -> variables.get(x), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testAbs() {
    Assert.assertEquals((Double)run("ABS(value)", ImmutableMap.of("value", 0)), 0, 1e-7);
    Assert.assertTrue(Double.isNaN((Double)run("ABS(value)", ImmutableMap.of("value", Double.NaN))));
    Assert.assertEquals((Double)run("ABS(value)", ImmutableMap.of("value", 10.5)), 10.5, 1e-7);
    Assert.assertEquals((Double)run("ABS(value)", ImmutableMap.of("value", -10.5)), 10.5, 1e-7);
  }
}
