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

package org.apache.metron.common.dsl.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.metron.common.dsl.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.common.utils.StellarProcessorUtils.runPredicate;

public class StringFunctionsTest {

  @Test
  public void testStringFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("true and TO_UPPER(foo) == 'CASEY'", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in [ TO_LOWER('CASEY'), 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("TO_UPPER(foo) in [ TO_UPPER('casey'), 'david' ] and IN_SUBNET(ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("TO_LOWER(foo) in [ TO_UPPER('casey'), 'david' ]", v -> variableMap.get(v)));
  }

  @Test
  public void testStringFunctions_advanced() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
      put("myList", ImmutableList.of("casey", "apple", "orange"));
    }};
    Assert.assertTrue(runPredicate("foo in SPLIT(bar, '.')", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("foo in SPLIT(ip, '.')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in myList", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("foo not in myList", v -> variableMap.get(v)));
  }

  @Test
  public void testLeftRightFills() throws Exception{
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", null);
      put("bar", null);
      put("notInt","oh my");
    }};

    //LEFT
    Object left = run("FILL_LEFT('123','X', 10)",new HashedMap());
    Assert.assertNotNull(left);
    Assert.assertEquals(10,((String)left).length());
    Assert.assertEquals("XXXXXXX123",(String)left);

    //RIGHT
    Object right = run("FILL_RIGHT('123','X', 10)", new HashedMap());
    Assert.assertNotNull(right);
    Assert.assertEquals(10,((String)right).length());
    Assert.assertEquals("123XXXXXXX",(String)right);

    //INPUT ALREADY LENGTH
    Object same = run("FILL_RIGHT('123','X', 3)", new HashedMap());
    Assert.assertEquals(3,((String)same).length());
    Assert.assertEquals("123",(String)same);

    //INPUT BIGGER THAN LENGTH
    Object tooBig = run("FILL_RIGHT('1234567890','X', 3)", new HashedMap());
    Assert.assertEquals(10,((String)tooBig).length());
    Assert.assertEquals("1234567890",(String)tooBig);

    //NULL VARIABLES
    boolean thrown = false;
    try{
      run("FILL_RIGHT('123',foo,bar)", variableMap);
    }catch(ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NULL LENGTH
    try{
      run("FILL_RIGHT('123','X',bar)", variableMap);
    }catch(ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NULL FILL
    try{
      run("FILL_RIGHT('123',foo, 7)", variableMap);
    }catch(ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NON INTEGER LENGTH
    try {
      run("FILL_RIGHT('123','X', 'z' )", new HashedMap());
    }catch(ParseException pe){
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("not a valid Integer"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // EMPTY STRING PAD
    try {
      Object returnValue = run("FILL_RIGHT('123','', 10 )", new HashedMap());
    }catch(ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be an empty"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    //MISSING LENGTH PARAMETER
    try {
      run("FILL_RIGHT('123',foo)", variableMap);
    }catch(ParseException pe){
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("expects three"));
    }
    Assert.assertTrue(thrown);
  }

  @Test
  public void shannonEntropyTest() throws Exception {
    //test empty string
    Assert.assertEquals(0.0, (Double)run("STRING_ENTROPY('')", new HashMap<>()), 0.0);
    Assert.assertEquals(0.0, (Double)run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "")), 0.0);

    /*
    Now consider the string aaaaaaaaaabbbbbccccc or 10 a's followed by 5 b's and 5 c's.
    The probabilities of each character is as follows:
    p(a) = 1/2
    p(b) = 1/4
    p(c) = 1/4
    so the shannon entropy should be
      -p(a)*log_2(p(a)) - p(b)*log_2(p(b)) - p(c)*log_2(p(c)) =
      -0.5*-1 - 0.25*-2 - 0.25*-2 = 1.5
     */
    Assert.assertEquals(1.5, (Double)run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "aaaaaaaaaabbbbbccccc")), 0.0);
  }

  @Test
  public void testFormat() throws Exception {

    Map<String, Object> vars = ImmutableMap.of(
            "cal", new Calendar.Builder().setDate(2017, 02, 02).build(),
            "x", 234,
            "y", 3);

    Assert.assertEquals("no args",        run("FORMAT('no args')", vars));
    Assert.assertEquals("234.0",          run("FORMAT('%.1f', TO_DOUBLE(234))", vars));
    Assert.assertEquals("000234",         run("FORMAT('%06d', 234)", vars));
    Assert.assertEquals("03 2,2017",      run("FORMAT('%1$tm %1$te,%1$tY', cal)", vars));
    Assert.assertEquals("234 > 3",        run("FORMAT('%d > %d', x, y)", vars));
    Assert.assertEquals("missing: null",  run("FORMAT('missing: %d', missing)", vars));
  }

  /**
   * FORMAT - Not passing a format string will throw an exception
   */
  @Test(expected = ParseException.class)
  public void testFormatWithNoArguments() throws Exception {
    run("FORMAT()", Collections.emptyMap());
  }

  /**
   * FORMAT - Forgetting to pass an argument required by the format string will throw an exception.
   */
  @Test(expected = ParseException.class)
  public void testFormatWithMissingArguments() throws Exception {
    run("FORMAT('missing arg: %d')", Collections.emptyMap());
  }
}
