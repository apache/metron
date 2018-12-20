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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;

public class StringFunctionsTest {

  @Test
  public void testStringFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("true and TO_UPPER(foo) == 'CASEY'", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("foo in [ TO_LOWER('CASEY'), 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("TO_UPPER(foo) in [ TO_UPPER('casey'), 'david' ] and IN_SUBNET(ip, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("TO_LOWER(foo) in [ TO_UPPER('casey'), 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
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
    Assert.assertTrue(runPredicate("foo in SPLIT(bar, '.')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("foo in SPLIT(ip, '.')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertTrue(runPredicate("foo in myList", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("foo not in myList", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testLeftRightFills() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", null);
      put("bar", null);
      put("notInt", "oh my");
    }};

    //LEFT
    Object left = run("FILL_LEFT('123','X', 10)", new HashedMap());
    Assert.assertNotNull(left);
    Assert.assertEquals(10, ((String) left).length());
    Assert.assertEquals("XXXXXXX123", (String) left);

    //RIGHT
    Object right = run("FILL_RIGHT('123','X', 10)", new HashedMap());
    Assert.assertNotNull(right);
    Assert.assertEquals(10, ((String) right).length());
    Assert.assertEquals("123XXXXXXX", (String) right);

    //INPUT ALREADY LENGTH
    Object same = run("FILL_RIGHT('123','X', 3)", new HashedMap());
    Assert.assertEquals(3, ((String) same).length());
    Assert.assertEquals("123", (String) same);

    //INPUT BIGGER THAN LENGTH
    Object tooBig = run("FILL_RIGHT('1234567890','X', 3)", new HashedMap());
    Assert.assertEquals(10, ((String) tooBig).length());
    Assert.assertEquals("1234567890", (String) tooBig);

    //NULL VARIABLES
    boolean thrown = false;
    try {
      run("FILL_RIGHT('123',foo,bar)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NULL LENGTH
    try {
      run("FILL_RIGHT('123','X',bar)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NULL FILL
    try {
      run("FILL_RIGHT('123',foo, 7)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("are both required"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // NON INTEGER LENGTH
    try {
      run("FILL_RIGHT('123','X', 'z' )", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("not a valid Integer"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // EMPTY STRING PAD
    try {
      Object returnValue = run("FILL_RIGHT('123','', 10 )", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be an empty"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    //MISSING LENGTH PARAMETER
    try {
      run("FILL_RIGHT('123',foo)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("expects three"));
    }
    Assert.assertTrue(thrown);
  }

  @Test
  public void shannonEntropyTest() throws Exception {
    //test empty string
    Assert.assertEquals(0.0, (Double) run("STRING_ENTROPY('')", new HashMap<>()), 0.0);
    Assert.assertEquals(0.0, (Double) run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "")), 0.0);

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
    Assert.assertEquals(1.5, (Double) run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "aaaaaaaaaabbbbbccccc")), 0.0);
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

    boolean thrown = false;
    try {
      run("FORMAT('missing: %d', missing)", vars);
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue(thrown);
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


  /**
   * CHOMP StringFunction
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testChomp() throws Exception {
    Assert.assertEquals("abc",  run("CHOMP('abc')", new HashedMap()));
    Assert.assertEquals("abc",  run("CHOMP(msg)", ImmutableMap.of("msg", "abc\r\n")));
    Assert.assertEquals("",     run("CHOMP(msg)", ImmutableMap.of("msg", "\n")));
    Assert.assertEquals("",     run("CHOMP('')", new HashedMap()));
    Assert.assertEquals(null,   run("CHOMP(null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("CHOMP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("missing argument"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Variable missing
    try{
      run("CHOMP(msg)", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
    }
    thrown = false;

    // Integer input
    try {
      run("CHOMP(123)", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    Assert.assertTrue(thrown);

  }

  /**
   * CHOP StringFunction
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testChop() throws Exception {
    Assert.assertEquals("ab",   run("CHOP('abc')", new HashedMap()));
    Assert.assertEquals(null,   run("CHOP(null)", new HashedMap()));
    Assert.assertEquals("abc",  run("CHOP(msg)", ImmutableMap.of("msg", "abc\r\n")));
    Assert.assertEquals("",     run("CHOP(msg)", ImmutableMap.of("msg", "")));
    Assert.assertEquals("",     run("CHOP(msg)", ImmutableMap.of("msg", "\n")));
    Assert.assertEquals("",     run("CHOP('')", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("CHOP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("missing argument"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Variable missing
    try{
      run("CHOMP(msg)", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
    }
    thrown = false;

    // Integer input
    try {
      run("CHOP(123)", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    Assert.assertTrue(thrown);

  }

  /**
   * PREPEND_IF_MISSING StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testPrependIfMissing() throws Exception {
    Assert.assertEquals("xyzabc",     run("PREPEND_IF_MISSING('abc', 'xyz')", new HashedMap()));
    Assert.assertEquals("xyzXYZabc",  run("PREPEND_IF_MISSING('XYZabc', 'xyz', 'mno')", new HashedMap()));
    Assert.assertEquals("mnoXYZabc",  run("PREPEND_IF_MISSING('mnoXYZabc', 'xyz', 'mno')", new HashedMap()));
    Assert.assertEquals(null,         run("PREPEND_IF_MISSING(null, null, null)", new HashedMap()));
    Assert.assertEquals("xyz",        run("PREPEND_IF_MISSING('', 'xyz', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("PREPEND_IF_MISSING()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("PREPEND_IF_MISSING('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 2
    try {
      run("PREPEND_IF_MISSING('abc', 'def', 'ghi', 'jkl')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("PREPEND_IF_MISSING(123, 'abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    Assert.assertTrue(thrown);

  }

  /**
   * APPEND_IF_MISSING StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testAppendIfMissing() throws Exception {
    Assert.assertEquals("apachemetron",   run("APPEND_IF_MISSING('apache', 'metron')", new HashedMap()));
    Assert.assertEquals("abcXYZxyz",      run("APPEND_IF_MISSING('abcXYZ', 'xyz', 'mno')", new HashedMap()));
    Assert.assertEquals(null,             run("APPEND_IF_MISSING(null, null, null)", new HashedMap()));
    Assert.assertEquals("xyz",            run("APPEND_IF_MISSING('', 'xyz', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("APPEND_IF_MISSING()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("APPEND_IF_MISSING('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 2
    try {
      run("APPEND_IF_MISSING('abc', 'def', 'ghi', 'jkl')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("APPEND_IF_MISSING(123, 'abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    Assert.assertTrue(thrown);

  }

  @Test
  public void testSubstring() throws Exception {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    Assert.assertEquals("metron", run("SUBSTRING(s, 7)", variables));
    Assert.assertEquals("me", run("SUBSTRING(s, 7, 9)", variables));
    Assert.assertNull(run("SUBSTRING(null, 7, 9)", new HashMap<>()));
    Assert.assertNull(run("SUBSTRING(null, null, 9)", new HashMap<>()));
    Assert.assertNull(run("SUBSTRING(s, null, 9)", variables));
    Assert.assertNull(run("SUBSTRING(null, null, null)", new HashMap<>()));
    Assert.assertEquals("metron", run("SUBSTRING(s, 7, null)", variables));
  }

  @Test(expected=ParseException.class)
  public void testSubstring_invalidEmpty() throws Exception {
    Assert.assertEquals("metron", run("SUBSTRING()", new HashMap<>()));
  }

  @Test(expected=ParseException.class)
  public void testSubstring_invalidWrongTypeStart() throws Exception {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    Assert.assertEquals("metron", (String) run("SUBSTRING(s, '7')", variables));
  }

  @Test(expected=ParseException.class)
  public void testSubstring_invalidWrongTypeEnd() throws Exception {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    Assert.assertEquals("metron", (String) run("SUBSTRING(s, 7, '9')", variables));
  }

  @Test(expected=ParseException.class)
  public void testSubstring_invalidWrongTypeInput() throws Exception {
    Map<String, Object> variables = ImmutableMap.of("s", 7);
    Assert.assertEquals("metron", (String) run("SUBSTRING(s, 7, '9')", variables));
  }

  /**
   * COUNT_MATCHES StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testCountMatches() throws Exception {
    Assert.assertEquals(0, (int) run("COUNT_MATCHES(null, '*')", new HashedMap()));
    Assert.assertEquals(2, (int) run("COUNT_MATCHES('apachemetron', 'e')", new HashedMap()));
    Assert.assertEquals(2, (int) run("COUNT_MATCHES('anand', 'an')", new HashedMap()));
    Assert.assertEquals(0, (int) run("COUNT_MATCHES('abcd', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("COUNT_MATCHES()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("COUNT_MATCHES('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("COUNT_MATCHES(123, 456)", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    Assert.assertTrue(thrown);

  }

  /**
   * TO_JSON_OBJECT StringFunction
   */

  // Input strings to be used
  /**
   { "foo" : 2 }
   */
  @Multiline
  private String string1;

  /**
   {
     "foo" : "abc",
     "bar" : "def"
   }
   */
  @Multiline
  private String string2;

  /**
   [ "foo", 2 ]
   */
  @Multiline
  private String string3;

  /**
   [ "foo", "bar", "car" ]
   */
  @Multiline
  private String string4;

  /**
   [
     {
       "foo1":"abc",
       "bar1":"def"
     },
     {
       "foo2":"ghi",
       "bar2":"jkl"
     }
   ]
   */
  @Multiline
  private String string5;

  @Test
  @SuppressWarnings("unchecked")
  public void testToJsonObject() throws Exception {
    //JSON Object
    Object ret1 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string1));
    Assert.assertNotNull(ret1);
    Assert.assertTrue (ret1 instanceof HashMap);

    Object ret2 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string2));
    Assert.assertNotNull(ret2);
    Assert.assertTrue (ret2 instanceof HashMap);
    Assert.assertEquals("def", run("MAP_GET( 'bar', returnval)", ImmutableMap.of("returnval", ret2)));

    //Simple Arrays
    Object ret3 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string3));
    Assert.assertNotNull(ret3);
    Assert.assertTrue (ret3 instanceof ArrayList);
    List<Object> result3 = (List<Object>) ret3;
    Assert.assertEquals(2, result3.get(1));

    Object ret4 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string4));
    Assert.assertNotNull(ret4);
    Assert.assertTrue (ret4 instanceof ArrayList);
    List<Object> result4 = (List<Object>) ret4;
    Assert.assertEquals("car", result4.get(2));

    //JSON Array
    Object ret5 = run( "TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string5));
    Assert.assertNotNull(ret5);
    Assert.assertTrue (ret5 instanceof ArrayList);
    List<List<Object>> result5 = (List<List<Object>>) ret5;
    HashMap<String,String> results5Map1 = (HashMap) result5.get(0);
    Assert.assertEquals("def", results5Map1.get("bar1"));
    HashMap<String,String> results5Map2 = (HashMap) result5.get(1);
    Assert.assertEquals("ghi", results5Map2.get("foo2"));

    // No input
    boolean thrown = false;
    try {
      run("TO_JSON_OBJECT()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Invalid input
    try {
      run("TO_JSON_OBJECT('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("Valid JSON string not supplied"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Malformed JSON String
    try {
      run("TO_JSON_OBJECT('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("Valid JSON string not supplied"));
    }
    Assert.assertTrue(thrown);
    thrown = false;
  }

  @Test
  public void testToJsonMap() throws Exception {
    //JSON Object
    Object ret1 = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string1));
    Assert.assertNotNull(ret1);
    Assert.assertTrue (ret1 instanceof HashMap);

    Object ret2 = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string2));
    Assert.assertNotNull(ret2);
    Assert.assertTrue (ret2 instanceof HashMap);
    Assert.assertEquals("def", run("MAP_GET( 'bar', returnval)", ImmutableMap.of("returnval", ret2)));

    //Simple Arrays
    boolean thrown = false;
    try {
      Object o = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string3));
      System.out.println(string3 + " == " + o);
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue(thrown);

    thrown = false;
    try {
      run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string4));
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue (thrown);

    //JSON Array
    thrown = false;
    try {
      run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string5));
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue(thrown);


    // No input
    try {
      run("TO_JSON_MAP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Invalid input
    try {
      run("TO_JSON_MAP('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    Assert.assertTrue(thrown);
    thrown = false;

    // Malformed JSON String
    try {
      run("TO_JSON_MAP('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    Assert.assertTrue(thrown);
    thrown = false;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testToJsonList() throws Exception {
    //Simple Arrays
    Object ret3 = run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string3));
    Assert.assertNotNull(ret3);
    Assert.assertTrue (ret3 instanceof ArrayList);
    List<Object> result3 = (List<Object>) ret3;
    Assert.assertEquals(2, result3.get(1));

    Object ret4 = run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string4));
    Assert.assertNotNull(ret4);
    Assert.assertTrue (ret4 instanceof ArrayList);
    List<Object> result4 = (List<Object>) ret4;
    Assert.assertEquals("car", result4.get(2));

    //JSON Array
    Object ret5 = run( "TO_JSON_LIST(msg)", ImmutableMap.of("msg", string5));
    Assert.assertNotNull(ret5);
    Assert.assertTrue (ret5 instanceof ArrayList);
    List<List<Object>> result5 = (List<List<Object>>) ret5;
    HashMap<String,String> results5Map1 = (HashMap) result5.get(0);
    Assert.assertEquals("def", results5Map1.get("bar1"));
    HashMap<String,String> results5Map2 = (HashMap) result5.get(1);
    Assert.assertEquals("ghi", results5Map2.get("foo2"));

    //JSON Object - throws exception
    boolean thrown = false;
    try {
      run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string1));
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue(thrown);

    thrown = false;
    try {
      run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string2));
    } catch (ParseException pe) {
      thrown = true;
    }
    Assert.assertTrue (thrown);

    // No input
    thrown = false;
    try {
      run("TO_JSON_LIST()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    Assert.assertTrue(thrown);

    // Invalid input
    thrown = false;
    try {
      run("TO_JSON_LIST('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    Assert.assertTrue(thrown);

    // Malformed JSON String
    thrown = false;
    try {
      run("TO_JSON_LIST('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      Assert.assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    Assert.assertTrue(thrown);
  }

}
