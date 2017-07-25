/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;

public class TextFunctionsTest {

  @Test
  public void testStringFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("metron", "metron");
      put("sentence", "metron is great");
      put("empty", "");
      put("english", "en");
      put("klingon", "Kling");
      put("asf", "Apache Software Foundation");
    }};
    Assert
        .assertTrue(runPredicate("0 == FUZZY_SCORE(metron,'z',english)", v -> variableMap.get(v)));
    Assert
        .assertTrue(runPredicate("0 == FUZZY_SCORE(metron,'z',klingon)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("0 == FUZZY_SCORE(empty,'z',english)", v -> variableMap.get(v)));
    Assert
        .assertTrue(runPredicate("0 == FUZZY_SCORE(empty,empty,english)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("0 == FUZZY_SCORE(empty,empty,empty)", v -> variableMap.get(v)));
    boolean caught = false;
    try {
      runPredicate("0 == FUZZY_SCORE()", v -> variableMap.get(v));
    } catch (ParseException pe) {
      caught = true;
    }
    Assert.assertTrue(caught);

    Assert
        .assertTrue(runPredicate("1 == FUZZY_SCORE(metron,'m',english)", v -> variableMap.get(v)));
    Assert.assertTrue(
        runPredicate("16 == FUZZY_SCORE(metron,'metron',english)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("3 == FUZZY_SCORE(asf,'asf',english)", v -> variableMap.get(v)));
  }
}
