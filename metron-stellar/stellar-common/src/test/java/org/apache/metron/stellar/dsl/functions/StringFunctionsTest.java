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
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;
import static org.junit.jupiter.api.Assertions.*;

public class StringFunctionsTest {

  @Test
  public void testStringFunctions() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    assertTrue(runPredicate("true and TO_UPPER(foo) == 'CASEY'", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in [ TO_LOWER('CASEY'), 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("TO_UPPER(foo) in [ TO_UPPER('casey'), 'david' ] and IN_SUBNET(ip, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("TO_LOWER(foo) in [ TO_UPPER('casey'), 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testStringFunctions_advanced() {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
      put("myList", ImmutableList.of("casey", "apple", "orange"));
    }};
    assertTrue(runPredicate("foo in SPLIT(bar, '.')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo in SPLIT(ip, '.')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in myList", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo not in myList", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testLeftRightFills() {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", null);
      put("bar", null);
      put("notInt", "oh my");
    }};

    //LEFT
    Object left = run("FILL_LEFT('123','X', 10)", new HashedMap());
    assertNotNull(left);
    assertEquals(10, ((String) left).length());
    assertEquals("XXXXXXX123", (String) left);

    //RIGHT
    Object right = run("FILL_RIGHT('123','X', 10)", new HashedMap());
    assertNotNull(right);
    assertEquals(10, ((String) right).length());
    assertEquals("123XXXXXXX", (String) right);

    //INPUT ALREADY LENGTH
    Object same = run("FILL_RIGHT('123','X', 3)", new HashedMap());
    assertEquals(3, ((String) same).length());
    assertEquals("123", (String) same);

    //INPUT BIGGER THAN LENGTH
    Object tooBig = run("FILL_RIGHT('1234567890','X', 3)", new HashedMap());
    assertEquals(10, ((String) tooBig).length());
    assertEquals("1234567890", (String) tooBig);

    //NULL VARIABLES
    boolean thrown = false;
    try {
      run("FILL_RIGHT('123',foo,bar)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("are both required"));
    }
    assertTrue(thrown);
    thrown = false;

    // NULL LENGTH
    try {
      run("FILL_RIGHT('123','X',bar)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("are both required"));
    }
    assertTrue(thrown);
    thrown = false;

    // NULL FILL
    try {
      run("FILL_RIGHT('123',foo, 7)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("are both required"));
    }
    assertTrue(thrown);
    thrown = false;

    // NON INTEGER LENGTH
    try {
      run("FILL_RIGHT('123','X', 'z' )", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("not a valid Integer"));
    }
    assertTrue(thrown);
    thrown = false;

    // EMPTY STRING PAD
    try {
      Object returnValue = run("FILL_RIGHT('123','', 10 )", new HashedMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("cannot be an empty"));
    }
    assertTrue(thrown);
    thrown = false;

    //MISSING LENGTH PARAMETER
    try {
      run("FILL_RIGHT('123',foo)", variableMap);
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("expects three"));
    }
    assertTrue(thrown);
  }

  @Test
  public void shannonEntropyTest() {
    //test empty string
    assertEquals(0.0, (Double) run("STRING_ENTROPY('')", new HashMap<>()), 0.0);
    assertEquals(0.0, (Double) run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "")), 0.0);

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
    assertEquals(1.5, (Double) run("STRING_ENTROPY(foo)", ImmutableMap.of("foo", "aaaaaaaaaabbbbbccccc")), 0.0);
  }

  @Test
  public void testFormat() {

    Map<String, Object> vars = ImmutableMap.of(
            "cal", new Calendar.Builder().setDate(2017, 02, 02).build(),
            "x", 234,
            "y", 3);

    assertEquals("no args",        run("FORMAT('no args')", vars));
    assertEquals("234.0",          run("FORMAT('%.1f', TO_DOUBLE(234))", vars));
    assertEquals("000234",         run("FORMAT('%06d', 234)", vars));
    assertEquals("03 2,2017",      run("FORMAT('%1$tm %1$te,%1$tY', cal)", vars));
    assertEquals("234 > 3",        run("FORMAT('%d > %d', x, y)", vars));

    boolean thrown = false;
    try {
      run("FORMAT('missing: %d', missing)", vars);
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);
  }

  /**
   * FORMAT - Not passing a format string will throw an exception
   */
  @Test
  public void testFormatWithNoArguments() {
    assertThrows(ParseException.class, () -> run("FORMAT()", Collections.emptyMap()));
  }

  /**
   * FORMAT - Forgetting to pass an argument required by the format string will throw an exception.
   */
  @Test
  public void testFormatWithMissingArguments() {
    assertThrows(ParseException.class, () -> run("FORMAT('missing arg: %d')", Collections.emptyMap()));
  }


  /**
   * CHOMP StringFunction
   *
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testChomp() {
    assertEquals("abc",  run("CHOMP('abc')", new HashedMap()));
    assertEquals("abc",  run("CHOMP(msg)", ImmutableMap.of("msg", "abc\r\n")));
    assertEquals("",     run("CHOMP(msg)", ImmutableMap.of("msg", "\n")));
    assertEquals("",     run("CHOMP('')", new HashedMap()));
    assertNull(run("CHOMP(null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("CHOMP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("missing argument"));
    }
    assertTrue(thrown);
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
      assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    assertTrue(thrown);

  }

  /**
   * CHOP StringFunction
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testChop() throws Exception {
    assertEquals("ab",   run("CHOP('abc')", new HashedMap()));
    assertNull(run("CHOP(null)", new HashedMap()));
    assertEquals("abc",  run("CHOP(msg)", ImmutableMap.of("msg", "abc\r\n")));
    assertEquals("",     run("CHOP(msg)", ImmutableMap.of("msg", "")));
    assertEquals("",     run("CHOP(msg)", ImmutableMap.of("msg", "\n")));
    assertEquals("",     run("CHOP('')", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("CHOP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("missing argument"));
    }
    assertTrue(thrown);
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
      assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    assertTrue(thrown);

  }

  /**
   * PREPEND_IF_MISSING StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testPrependIfMissing() throws Exception {
    assertEquals("xyzabc",     run("PREPEND_IF_MISSING('abc', 'xyz')", new HashedMap()));
    assertEquals("xyzXYZabc",  run("PREPEND_IF_MISSING('XYZabc', 'xyz', 'mno')", new HashedMap()));
    assertEquals("mnoXYZabc",  run("PREPEND_IF_MISSING('mnoXYZabc', 'xyz', 'mno')", new HashedMap()));
    assertNull(run("PREPEND_IF_MISSING(null, null, null)", new HashedMap()));
    assertEquals("xyz",        run("PREPEND_IF_MISSING('', 'xyz', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("PREPEND_IF_MISSING()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("PREPEND_IF_MISSING('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 2
    try {
      run("PREPEND_IF_MISSING('abc', 'def', 'ghi', 'jkl')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("PREPEND_IF_MISSING(123, 'abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    assertTrue(thrown);

  }

  /**
   * APPEND_IF_MISSING StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testAppendIfMissing() {
    assertEquals("apachemetron",   run("APPEND_IF_MISSING('apache', 'metron')", new HashedMap()));
    assertEquals("abcXYZxyz",      run("APPEND_IF_MISSING('abcXYZ', 'xyz', 'mno')", new HashedMap()));
    assertNull(run("APPEND_IF_MISSING(null, null, null)", new HashedMap()));
    assertEquals("xyz",            run("APPEND_IF_MISSING('', 'xyz', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("APPEND_IF_MISSING()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("APPEND_IF_MISSING('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 2
    try {
      run("APPEND_IF_MISSING('abc', 'def', 'ghi', 'jkl')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("APPEND_IF_MISSING(123, 'abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    assertTrue(thrown);

  }

  @Test
  public void testSubstring() {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    assertEquals("metron", run("SUBSTRING(s, 7)", variables));
    assertEquals("me", run("SUBSTRING(s, 7, 9)", variables));
    assertNull(run("SUBSTRING(null, 7, 9)", new HashMap<>()));
    assertNull(run("SUBSTRING(null, null, 9)", new HashMap<>()));
    assertNull(run("SUBSTRING(s, null, 9)", variables));
    assertNull(run("SUBSTRING(null, null, null)", new HashMap<>()));
    assertEquals("metron", run("SUBSTRING(s, 7, null)", variables));
  }

  @Test
  public void testSubstring_invalidEmpty() {
    assertThrows(ParseException.class, () -> run("SUBSTRING()", new HashMap<>()));
  }

  @Test
  public void testSubstring_invalidWrongTypeStart() {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    assertThrows(ParseException.class, () -> run("SUBSTRING(s, '7')", variables));
  }

  @Test
  public void testSubstring_invalidWrongTypeEnd() {
    Map<String, Object> variables = ImmutableMap.of("s", "apache metron");
    assertThrows(ParseException.class, () -> run("SUBSTRING(s, 7, '9')", variables));
  }

  @Test
  public void testSubstring_invalidWrongTypeInput() {
    Map<String, Object> variables = ImmutableMap.of("s", 7);
    assertThrows(ParseException.class, () -> run("SUBSTRING(s, 7, '9')", variables));
  }

  /**
   * COUNT_MATCHES StringFunction
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testCountMatches() throws Exception {
    assertEquals(0, (int) run("COUNT_MATCHES(null, '*')", new HashedMap()));
    assertEquals(2, (int) run("COUNT_MATCHES('apachemetron', 'e')", new HashedMap()));
    assertEquals(2, (int) run("COUNT_MATCHES('anand', 'an')", new HashedMap()));
    assertEquals(0, (int) run("COUNT_MATCHES('abcd', null)", new HashedMap()));

    // No input
    boolean thrown = false;
    try {
      run("COUNT_MATCHES()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Incorrect number of arguments - 1
    try {
      run("COUNT_MATCHES('abc')", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("incorrect arguments"));
    }
    assertTrue(thrown);
    thrown = false;

    // Integer input
    try {
      run("COUNT_MATCHES(123, 456)", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("cannot be cast"));
    }
    assertTrue(thrown);

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
  public void testToJsonObject() {
    //JSON Object
    Object ret1 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string1));
    assertNotNull(ret1);
    assertTrue (ret1 instanceof HashMap);

    Object ret2 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string2));
    assertNotNull(ret2);
    assertTrue (ret2 instanceof HashMap);
    assertEquals("def", run("MAP_GET( 'bar', returnval)", ImmutableMap.of("returnval", ret2)));

    //Simple Arrays
    Object ret3 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string3));
    assertNotNull(ret3);
    assertTrue (ret3 instanceof ArrayList);
    List<Object> result3 = (List<Object>) ret3;
    assertEquals(2, result3.get(1));

    Object ret4 = run("TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string4));
    assertNotNull(ret4);
    assertTrue (ret4 instanceof ArrayList);
    List<Object> result4 = (List<Object>) ret4;
    assertEquals("car", result4.get(2));

    //JSON Array
    Object ret5 = run( "TO_JSON_OBJECT(msg)", ImmutableMap.of("msg", string5));
    assertNotNull(ret5);
    assertTrue (ret5 instanceof ArrayList);
    List<List<Object>> result5 = (List<List<Object>>) ret5;
    HashMap<String,String> results5Map1 = (HashMap) result5.get(0);
    assertEquals("def", results5Map1.get("bar1"));
    HashMap<String,String> results5Map2 = (HashMap) result5.get(1);
    assertEquals("ghi", results5Map2.get("foo2"));

    // No input
    boolean thrown = false;
    try {
      run("TO_JSON_OBJECT()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    assertTrue(thrown);
    thrown = false;

    // Invalid input
    try {
      run("TO_JSON_OBJECT('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("Valid JSON string not supplied"));
    }
    assertTrue(thrown);
    thrown = false;

    // Malformed JSON String
    try {
      run("TO_JSON_OBJECT('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("Valid JSON string not supplied"));
    }
    assertTrue(thrown);
    thrown = false;
  }

  @Test
  public void testToJsonMap() {
    //JSON Object
    Object ret1 = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string1));
    assertNotNull(ret1);
    assertTrue (ret1 instanceof HashMap);

    Object ret2 = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string2));
    assertNotNull(ret2);
    assertTrue (ret2 instanceof HashMap);
    assertEquals("def", run("MAP_GET( 'bar', returnval)", ImmutableMap.of("returnval", ret2)));

    //Simple Arrays
    boolean thrown = false;
    try {
      Object o = run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string3));
      System.out.println(string3 + " == " + o);
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);

    thrown = false;
    try {
      run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string4));
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue (thrown);

    //JSON Array
    thrown = false;
    try {
      run("TO_JSON_MAP(msg)", ImmutableMap.of("msg", string5));
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);


    // No input
    try {
      run("TO_JSON_MAP()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    assertTrue(thrown);
    thrown = false;

    // Invalid input
    try {
      run("TO_JSON_MAP('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    assertTrue(thrown);
    thrown = false;

    // Malformed JSON String
    try {
      run("TO_JSON_MAP('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    assertTrue(thrown);
    thrown = false;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testToJsonList() {
    //Simple Arrays
    Object ret3 = run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string3));
    assertNotNull(ret3);
    assertTrue (ret3 instanceof ArrayList);
    List<Object> result3 = (List<Object>) ret3;
    assertEquals(2, result3.get(1));

    Object ret4 = run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string4));
    assertNotNull(ret4);
    assertTrue (ret4 instanceof ArrayList);
    List<Object> result4 = (List<Object>) ret4;
    assertEquals("car", result4.get(2));

    //JSON Array
    Object ret5 = run( "TO_JSON_LIST(msg)", ImmutableMap.of("msg", string5));
    assertNotNull(ret5);
    assertTrue (ret5 instanceof ArrayList);
    List<List<Object>> result5 = (List<List<Object>>) ret5;
    HashMap<String,String> results5Map1 = (HashMap) result5.get(0);
    assertEquals("def", results5Map1.get("bar1"));
    HashMap<String,String> results5Map2 = (HashMap) result5.get(1);
    assertEquals("ghi", results5Map2.get("foo2"));

    //JSON Object - throws exception
    boolean thrown = false;
    try {
      run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string1));
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);

    thrown = false;
    try {
      run("TO_JSON_LIST(msg)", ImmutableMap.of("msg", string2));
    } catch (ParseException pe) {
      thrown = true;
    }
    assertTrue (thrown);

    // No input
    thrown = false;
    try {
      run("TO_JSON_LIST()", Collections.emptyMap());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("Unable to parse"));
    }
    assertTrue(thrown);

    // Invalid input
    thrown = false;
    try {
      run("TO_JSON_LIST('123, 456')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    assertTrue(thrown);

    // Malformed JSON String
    thrown = false;
    try {
      run("TO_JSON_LIST('{\"foo\" : 2')", new HashedMap<>());
    } catch (ParseException pe) {
      thrown = true;
      assertTrue(pe.getMessage().contains("is not a valid JSON string"));
    }
    assertTrue(thrown);
  }

}
