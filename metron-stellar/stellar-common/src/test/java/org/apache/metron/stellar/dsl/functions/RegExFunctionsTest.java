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

import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;

public class RegExFunctionsTest {

  // test RegExMatch
  @Test
  public void testRegExMatch() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("letterPattern", "[a-zA-Z]+");
      put("empty", "");
    }};

    Assert.assertTrue(runPredicate("REGEXP_MATCH(numbers,numberPattern)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(letters,numberPattern)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_MATCH(letters,[numberPattern,letterPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(letters,[numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(letters,[numberPattern,numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(null,[numberPattern])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(letters,null)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("REGEXP_MATCH(letters,[null])", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testRegExGroupVal() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("numberPatternNoCaptures", "\\d\\d\\d.*");
      put("letters", "abcde");
      put("empty", "");
    }};
    Assert.assertTrue(runPredicate("REGEXP_GROUP_VAL(numbers,numberPattern,2) == '3'", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_GROUP_VAL(letters,numberPattern,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_GROUP_VAL(empty,numberPattern,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_GROUP_VAL(numbers,numberPatternNoCaptures,2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));

    boolean thrown = false;
    try{
      runPredicate("REGEXP_GROUP_VAL(2) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v)));
    }catch(ParseException | IllegalStateException ise){
      thrown = true;
    }
    if(!thrown){
      Assert.assertTrue("Did not fail on wrong number of parameters",false);
    }
  }

  @Test
  public void testRegExReplace() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("empty", "");
    }};

    Assert.assertTrue(runPredicate("REGEXP_REPLACE(empty, numberPattern, letters) == null", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_REPLACE(numbers, empty, empty) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_REPLACE(numbers, empty, letters) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_REPLACE(numbers, numberPattern, empty) == numbers", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_REPLACE(numbers, numberPattern, letters) == letters", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("REGEXP_REPLACE(letters, numberPattern, numbers) == letters", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }


}
