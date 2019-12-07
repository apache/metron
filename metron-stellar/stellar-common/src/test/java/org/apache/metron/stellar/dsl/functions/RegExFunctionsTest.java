/**
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

package org.apache.metron.stellar.dsl.functions;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.HashMap;
import java.util.Map;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.Test;

public class RegExFunctionsTest {

  // test RegExMatch
  @Test
  public void testRegExMatch() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("letterPattern", "[a-zA-Z]+");
      put("empty", "");
    }};

    assertTrue(runPredicate("REGEXP_MATCH(numbers,numberPattern)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(letters,numberPattern)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_MATCH(letters,[numberPattern,letterPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(letters,[numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(letters,[numberPattern,numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(null,[numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(letters,null)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("REGEXP_MATCH(letters,[null])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testRegExGroupVal() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("numberPatternNoCaptures", "\\d\\d\\d.*");
      put("letters", "abcde");
      put("empty", "");
    }};
    assertTrue(runPredicate("REGEXP_GROUP_VAL(numbers,numberPattern,2) == '3'", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_GROUP_VAL(letters,numberPattern,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_GROUP_VAL(empty,numberPattern,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_GROUP_VAL(numbers,numberPatternNoCaptures,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));

    assertThrows(ParseException.class, () -> runPredicate("REGEXP_GROUP_VAL(2) == null",
        new DefaultVariableResolver(v -> variableMap.get(v), v -> variableMap.containsKey(v))),
        "Did not fail on wrong number of parameters");
  }

  @Test
  public void testRegExReplace() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("empty", "");
    }};

    assertTrue(runPredicate("REGEXP_REPLACE(empty, numberPattern, letters) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_REPLACE(numbers, empty, empty) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_REPLACE(numbers, empty, letters) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_REPLACE(numbers, numberPattern, empty) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_REPLACE(numbers, numberPattern, letters) == letters", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("REGEXP_REPLACE(letters, numberPattern, numbers) == letters", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }


}
