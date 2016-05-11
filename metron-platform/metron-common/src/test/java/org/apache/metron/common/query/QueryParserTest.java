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

package org.apache.metron.common.query;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class QueryParserTest {

  @Test
  public void testValidation() throws Exception {
    PredicateProcessor processor = new PredicateProcessor();
    try {
      processor.validate("'foo'");
      Assert.fail("Invalid rule found to be valid.");
    }
    catch(ParseException e) {

    }
    try {
      processor.validate("enrichedField1 == 'enrichedValue1");
      Assert.fail("Invalid rule found to be valid.");
    }
    catch(ParseException e) {

    }
  }

  private static boolean run(String rule, VariableResolver resolver) {
    PredicateProcessor processor = new PredicateProcessor();
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
    Assert.assertTrue(run("'casey' == foo.bar", v -> variableMap.get(v)));
    Assert.assertTrue(run("'casey' == foo", v -> variableMap.get(v)));
    Assert.assertFalse(run("'casey' != foo", v -> variableMap.get(v)));
    Assert.assertTrue(run("'stella' == 'stella'", v -> variableMap.get(v)));
    Assert.assertFalse(run("'stella' == foo", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo== foo", v -> variableMap.get(v)));
    Assert.assertTrue(run("empty== ''", v -> variableMap.get(v)));
    Assert.assertTrue(run("spaced == 'metron is great'", v -> variableMap.get(v)));
  }

  @Test
  public void testBooleanOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertFalse(run("not('casey' == foo and true)", v -> variableMap.get(v)));
    Assert.assertTrue(run("not(not('casey' == foo and true))", v -> variableMap.get(v)));
    Assert.assertTrue(run("('casey' == foo) && ( false != true )", v -> variableMap.get(v)));
    Assert.assertFalse(run("('casey' == foo) and (FALSE == TRUE)", v -> variableMap.get(v)));
    Assert.assertFalse(run("'casey' == foo and FALSE", v -> variableMap.get(v)));
    Assert.assertTrue(run("'casey' == foo and true", v -> variableMap.get(v)));
  }
  @Test
  public void testList() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(run("foo in [ 'casey', 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo in [ foo, 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo in [ 'casey', 'david' ] and 'casey' == foo", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo in [ 'casey', 'david' ] and foo == 'casey'", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo in [ 'casey' ]", v -> variableMap.get(v)));
    Assert.assertFalse(run("foo not in [ 'casey', 'david' ]", v -> variableMap.get(v)));
    Assert.assertFalse(run("foo not in [ 'casey', 'david' ] and 'casey' == foo", v -> variableMap.get(v)));
  }
  @Test
  public void testExists() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(run("exists(foo)", v -> variableMap.get(v)));
    Assert.assertFalse(run("exists(bar)", v -> variableMap.get(v)));
    Assert.assertTrue(run("exists(bar) or true", v -> variableMap.get(v)));
  }

  @Test
  public void testStringFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    Assert.assertTrue(run("true and TO_UPPER(foo) == 'CASEY'", v -> variableMap.get(v)));
    Assert.assertTrue(run("foo in [ TO_LOWER('CASEY'), 'david' ]", v -> variableMap.get(v)));
    Assert.assertTrue(run("TO_UPPER(foo) in [ TO_UPPER('casey'), 'david' ] and IN_SUBNET(ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(run("TO_LOWER(foo) in [ TO_UPPER('casey'), 'david' ]", v -> variableMap.get(v)));
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
    Assert.assertTrue(run("IN_SUBNET(ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertTrue(run("IN_SUBNET(ip, '192.168.0.0/24', '11.0.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(run("IN_SUBNET(ip_dst_addr, '192.168.0.0/24', '11.0.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(run("IN_SUBNET(other_ip, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertFalse(run("IN_SUBNET(blah, '192.168.0.0/24')", v -> variableMap.get(v)));
    Assert.assertTrue(run("true and STARTS_WITH(foo, 'ca')", v -> variableMap.get(v)));
    Assert.assertTrue(run("true and STARTS_WITH(TO_UPPER(foo), 'CA')", v -> variableMap.get(v)));
    Assert.assertTrue(run("(true and STARTS_WITH(TO_UPPER(foo), 'CA')) || true", v -> variableMap.get(v)));
    Assert.assertTrue(run("true and ENDS_WITH(foo, 'sey')", v -> variableMap.get(v)));
    Assert.assertTrue(run("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24') and IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
    Assert.assertTrue(run("IN_SUBNET(ip_src_addr, '192.168.0.0/24')", v-> variableMap.get(v)));
    Assert.assertFalse(run("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
    Assert.assertFalse(run("IN_SUBNET(ip_dst_addr, '192.168.0.0/24')", v-> variableMap.get(v)));
    Assert.assertTrue(run("not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", v-> variableMap.get(v)));
  }
}
