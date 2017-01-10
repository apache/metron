/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.stellar;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class StellarComparisonExpressionWithOperatorTest {
  @SuppressWarnings({"RedundantConditionalExpression", "ConstantConditions"})
  @Test
  public void checkLessThanComparisonOperators() throws Exception {
    assertEquals(1 < 2, run("1 < 2", ImmutableMap.of()));
    assertEquals(1f < 2, run("1f < 2", ImmutableMap.of()));
    assertEquals(1f < 2d, run("1f < 2d", ImmutableMap.of()));
    assertEquals(1f < 2e-4d, run("1f < 2e-4d", ImmutableMap.of()));
    assertEquals(1L < 2e-4d, run("1L < 2e-4d", ImmutableMap.of()));
    assertEquals(1 < 2e-4d, run("1 < 2e-4d", ImmutableMap.of()));
    assertEquals(1 < 2L, run("1 < 2L", ImmutableMap.of()));
    assertEquals(1.0f < 2.0f, run("1.0f < 2.0f", ImmutableMap.of()));
    assertEquals(1L < 3.0f, run("1L < 3.0f", ImmutableMap.of()));
    assertEquals(1 < 3.0f, run("1 < 3.0f", ImmutableMap.of()));
    assertEquals(1.0 < 3.0f, run("1.0 < 3.0f", ImmutableMap.of()));
    assertEquals(1L < 3.0f ? true : false, run("if 1L < 3.0f then true else false", ImmutableMap.of()));
  }

  @Test
  public void checkComparisonOperationsWithFunctions() throws Exception {
    assertEquals(1f >= 2, run("TO_FLOAT(1) >= 2", ImmutableMap.of()));
    assertEquals(1f <= 2, run("TO_FLOAT(1) <= TO_FLOAT(2)", ImmutableMap.of()));
    assertEquals(1f == 2, run("TO_FLOAT(1) == TO_LONG(2)", ImmutableMap.of()));
    assertEquals(12.31f == 10.2f, run("TO_FLOAT(12.31) < 10.2f", ImmutableMap.of()));
  }

  @Test
  public void testSimpleOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
      put("foo.bar", "casey");
    }};

    assertTrue(runPredicate("'casey' == foo.bar", variableMap::get));
    assertTrue(runPredicate("'casey' == foo", variableMap::get));
    assertFalse(runPredicate("'casey' != foo", variableMap::get));
    assertTrue(runPredicate("'stella' == 'stella'", variableMap::get));
    assertFalse(runPredicate("'stella' == foo", variableMap::get));
    assertTrue(runPredicate("foo== foo", variableMap::get));
    assertTrue(runPredicate("empty== ''", variableMap::get));
    assertTrue(runPredicate("spaced == 'metron is great'", variableMap::get));
    assertTrue(runPredicate(null, variableMap::get));
    assertTrue(runPredicate("", variableMap::get));
    assertTrue(runPredicate(" ", variableMap::get));
  }
}
