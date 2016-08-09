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

package org.apache.metron.common.stellar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.dsl.MapVariableResolver;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.VariableResolver;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class StellarTest {
  @Test
  public void testIfThenElse() {
    {
      String query = "if 1 < 2 then 'one' else 'two'";
      Assert.assertEquals("one", run(query, ImmutableMap.of("foo", "casey ")));
    }
    {
      String query = "if 1 + 1 < 2 then 'one' else 'two'";
      Assert.assertEquals("two", run(query, ImmutableMap.of("foo", "casey ")));
    }
    {
      String query = "1 < 2 ? 'one' : 'two'";
      Assert.assertEquals("one", run(query, ImmutableMap.of("foo", "casey ")));
    }
    {
      String query = "if not(1 < 2) then 'one' else 'two'";
      Assert.assertEquals("two", run(query, ImmutableMap.of("foo", "casey ")));
    }
    {
      String query = "if 1 == 1.000001 then 'one' else 'two'";
      Assert.assertEquals("one", run(query, ImmutableMap.of("foo", "casey ")));
    }
    {
      String query = "if one < two then 'one' else 'two'";
      Assert.assertEquals("one", run(query, ImmutableMap.of("one", 1, "two", 2)));
    }
    {
      String query = "if one == very_nearly_one then 'one' else 'two'";
      Assert.assertEquals("one", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)));
    }
  }

  @Test
  public void testNumericOperations() {
    {
      String query = "TO_INTEGER(1 + 2*2 + 3 - 4 - 0.5)";
      Assert.assertEquals(3, (Integer)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2*2 + 3 - 4 - 0.5";
      Assert.assertEquals(3.5, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "2*one*(1 + 2*2 + 3 - 4)";
      Assert.assertEquals(8, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "2*(1 + 2 + 3 - 4)";
      Assert.assertEquals(4, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 - 4 - 2";
      Assert.assertEquals(0, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 + 4";
      Assert.assertEquals(10, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "(one + 2)*3";
      Assert.assertEquals(9, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "TO_INTEGER((one + 2)*3.5)";
      Assert.assertEquals(10, (Integer)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2*3";
      Assert.assertEquals(7, (Double)run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }

  }


  @Test
  public void testHappyPath() {
    String query = "TO_UPPER(TRIM(foo))";
    Assert.assertEquals("CASEY", run(query, ImmutableMap.of("foo", "casey ")));
  }

  @Test
  public void testJoin() {
    String query = "JOIN( [ TO_UPPER(TRIM(foo)), 'bar' ], ',')";
    Assert.assertEquals("CASEY,bar", run(query, ImmutableMap.of("foo", "casey ")));
  }

  @Test
  public void testSplit() {
    String query = "JOIN( SPLIT(foo, ':'), ',')";
    Assert.assertEquals("casey,bar", run(query, ImmutableMap.of("foo", "casey:bar")));
  }

  @Test
  public void testMapGet() {
    String query = "MAP_GET(dc, dc2tz, 'UTC')";
    Assert.assertEquals("UTC"
                       , run(query, ImmutableMap.of("dc", "nyc"
                                                   ,"dc2tz", ImmutableMap.of("la", "PST")
                                                   )
                            )
                       );
    Assert.assertEquals("EST"
                       , run(query, ImmutableMap.of("dc", "nyc"
                                                   ,"dc2tz", ImmutableMap.of("nyc", "EST")
                                                   )
                            )
                       );
  }
  @Test
  public void testTLDExtraction() {
    String query = "DOMAIN_TO_TLD(foo)";
    Assert.assertEquals("co.uk", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
  }

  @Test
  public void testTLDRemoval() {
    String query = "DOMAIN_REMOVE_TLD(foo)";
    Assert.assertEquals("www.google", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
  }

  @Test
  public void testSubdomainRemoval() {
    String query = "DOMAIN_REMOVE_SUBDOMAINS(foo)";
    Assert.assertEquals("google.co.uk", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
    Assert.assertEquals("google.com", run(query, ImmutableMap.of("foo", "www.google.com")));
  }
  @Test
  public void testURLToHost() {
    String query = "URL_TO_HOST(foo)";
    Assert.assertEquals("www.google.co.uk", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToPort() {
    String query = "URL_TO_PORT(foo)";
    Assert.assertEquals(80, run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToProtocol() {
    String query = "URL_TO_PROTOCOL(foo)";
    Assert.assertEquals("http", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToPath() {
    String query = "URL_TO_PATH(foo)";
    Assert.assertEquals("/my/path", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testProtocolToName() {
    String query = "PROTOCOL_TO_NAME(protocol)";
    Assert.assertEquals("TCP", run(query, ImmutableMap.of("protocol", "6")));
    Assert.assertEquals("TCP", run(query, ImmutableMap.of("protocol", 6)));
    Assert.assertEquals(null, run(query, ImmutableMap.of("foo", 6)));
    Assert.assertEquals("chicken", run(query, ImmutableMap.of("protocol", "chicken")));
  }
  @Test
  public void testDateConversion() {
    long expected =1452013350000L;
    {
      String query = "TO_EPOCH_TIMESTAMP(foo, 'yyyy-MM-dd HH:mm:ss', 'UTC')";
      Assert.assertEquals(expected, run(query, ImmutableMap.of("foo", "2016-01-05 17:02:30")));
    }
    {
      String query = "TO_EPOCH_TIMESTAMP(foo, 'yyyy-MM-dd HH:mm:ss')";
      Long ts = (Long) run(query, ImmutableMap.of("foo", "2016-01-05 17:02:30"));
      //is it within 24 hours of the UTC?
      Assert.assertTrue(Math.abs(ts - expected) < 8.64e+7);
    }
  }

  @Test
  public void testToString() {
    Assert.assertEquals("5", run("TO_STRING(foo)", ImmutableMap.of("foo", 5)));
  }

  @Test
  public void testToInteger() {
    Assert.assertEquals(5, run("TO_INTEGER(foo)", ImmutableMap.of("foo", "5")));
    Assert.assertEquals(5, run("TO_INTEGER(foo)", ImmutableMap.of("foo", 5)));
  }

  @Test
  public void testToDouble() {
    Assert.assertEquals(new Double(5.1), run("TO_DOUBLE(foo)", ImmutableMap.of("foo", 5.1d)));
    Assert.assertEquals(new Double(5.1), run("TO_DOUBLE(foo)", ImmutableMap.of("foo", "5.1")));
  }
  @Test
  public void testGet() {
    Map<String, Object> variables = ImmutableMap.of("foo", "www.google.co.uk");
    Assert.assertEquals("www", run("GET_FIRST(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'))", variables));
    Assert.assertEquals("www", run("GET(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'), 0)", variables));
    Assert.assertEquals("google", run("GET_LAST(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'))", variables));
    Assert.assertEquals("google", run("GET(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'), 1)", variables));
  }
  private static Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule));
    return processor.parse(rule, x -> variables.get(x));
  }@Test
  public void testValidation() throws Exception {
    StellarPredicateProcessor processor = new StellarPredicateProcessor();
    try {
      processor.validate("'foo'");
      Assert.fail("Invalid rule found to be valid - lone value.");
    }
    catch(ParseException e) {

    }
    try {
      processor.validate("enrichedField1 == 'enrichedValue1");
      Assert.fail("Invalid rule found to be valid - unclosed single quotes.");
    }
    catch(ParseException e) {

    }
  }

  public static boolean runPredicate(String rule, Map resolver) {
    return runPredicate(rule, new MapVariableResolver(resolver));
  }
  public static boolean runPredicate(String rule, VariableResolver resolver) {
    StellarPredicateProcessor processor = new StellarPredicateProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule));
    return processor.parse(rule, resolver);
  }

  @Test
  public void testSimpleOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
      put("foo.bar", "casey");
    }};
    Assert.assertTrue(runPredicate("'casey' == foo.bar", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("'casey' == foo", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("'casey' != foo", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("'stella' == 'stella'", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("'stella' == foo", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo== foo", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("empty== ''", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("spaced == 'metron is great'", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate(null, v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate(" ", v -> variableMap.get(v)));
  }

  @Test
  public void testBooleanOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertFalse(runPredicate("not('casey' == foo and true)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("not(not('casey' == foo and true))", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("('casey' == foo) && ( false != true )", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("('casey' == foo) and (FALSE == TRUE)", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("'casey' == foo and FALSE", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("'casey' == foo and true", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("true", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("TRUE", v -> variableMap.get(v)));
  }

  @Test
  public void testList() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("foo in [ 'casey', 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in [ foo, 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in [ 'casey', 'david' ] and 'casey' == foo", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in [ 'casey', 'david' ] and foo == 'casey'", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("foo in [ 'casey' ]", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("foo not in [ 'casey', 'david' ]", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("foo not in [ 'casey', 'david' ] and 'casey' == foo", v -> variableMap.get(v)));
  }

  @Test
  public void testExists() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("exists(foo)", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("exists(bar)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("exists(bar) or true", v -> variableMap.get(v)));
  }

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
  public void testMapFunctions_advanced() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
      put("myMap", ImmutableMap.of("casey", "apple"));
    }};
    Assert.assertTrue(runPredicate("MAP_EXISTS(foo, myMap)", v -> variableMap.get(v)));
  }

  @Test
  public void testNumericComparisonFunctions() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("num", 7);
      put("num2", 8.5);
      put("num3", 7);
      put("num4", "8.5");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("num == 7", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num < num2", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num < TO_DOUBLE(num2)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num < TO_DOUBLE(num4)", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num < 100", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num == num3", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("num == num2", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num == num2 || true", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("num > num2", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("num == 7 && num > 2", v -> variableMap.get(v)));
  }
  @Test
  public void testLogicalFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("ip_src_addr", "192.168.0.1");
      put("ip_dst_addr", "10.0.0.1");
      put("other_ip", "10.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(runPredicate("IN_SUBNET(ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("IN_SUBNET(ip, '192.168.0.0/24', '11.0.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("IN_SUBNET(ip_dst_addr, '192.168.0.0/24', '11.0.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("IN_SUBNET(other_ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(runPredicate("IN_SUBNET(blah, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("true and STARTS_WITH(foo, 'ca')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("true and STARTS_WITH(TO_UPPER(foo), 'CA')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("(true and STARTS_WITH(TO_UPPER(foo), 'CA')) || true", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("true and ENDS_WITH(foo, 'sey')", v -> variableMap.get(v)));
    Assert.assertTrue(runPredicate("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24') and IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
    Assert.assertTrue(runPredicate("IN_SUBNET(ip_src_addr, '192.168.0.0/24')", v-> variableMap.get(v)));
    Assert.assertFalse(runPredicate("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
    Assert.assertFalse(runPredicate("IN_SUBNET(ip_dst_addr, '192.168.0.0/24')", v-> variableMap.get(v)));
    Assert.assertTrue(runPredicate("not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
  }

}
