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
package org.apache.metron.stellar.dsl.functions;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.runPredicate;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MatchTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testMatchLambda() {
    Assert.assertTrue(runPredicate("match { 1 >= 0 : ()-> true }", new HashMap() {{
      put("foo", 0);
    }}));
    Assert.assertTrue(
        runPredicate("match { foo == 0 : ()-> true, default : ()-> false }", new HashMap() {{
          put("foo", 0);
        }}));

    Assert.assertFalse(
        runPredicate("match { foo == 0 : ()-> true, default : ()-> false }", new HashMap() {{
          put("foo", 1);
        }}));

    Assert.assertTrue(
        runPredicate("match { foo == 0 : ()-> false, foo == 1 : ()-> true, default : ()-> false }",
            new HashMap() {{
              put("foo", 1);
            }}));

    Assert.assertTrue(runPredicate(
        "match { foo == 0 : ()-> bFalse, foo == 1 : ()-> bTrue, default : ()-> bFalse }",
        new HashMap() {{
          put("foo", 1);
          put("bFalse", false);
          put("bTrue", true);
        }}));

    Assert.assertTrue(runPredicate(
        "match { foo == 0 : ()-> bFalse, foo == 1 : ()-> bTrue, default : ()-> bFalse }",
        new HashMap() {{
          put("foo", 1);
          put("bFalse", false);
          put("bTrue", true);
        }}));

  }

  @Test
  @SuppressWarnings("unchecked")
  @Ignore
  public void testMatchMAPEvaluation() {

    // NOTE: THIS IS BROKEN RIGHT NOW.

    String expr = "match{ var1 :  MAP(['foo', 'bar'], (x) -> TO_UPPER(x)) }";

    Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar", "var1", true));

    Assert.assertTrue(o instanceof List);

    List<String> result = (List<String>) o;

    Assert.assertEquals(2, result.size());
    Assert.assertEquals("FOO", result.get(0));
    Assert.assertEquals("BAR", result.get(1));
}

  @Test
  @SuppressWarnings("unchecked")
  public void testMatchRegexMatch() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("empty", "");
    }};

    Assert.assertTrue(runPredicate("match{ REGEXP_MATCH(numbers,numberPattern): true, default : false}", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    Assert.assertFalse(runPredicate("match{ REGEXP_MATCH(letters,numberPattern) : true, default :false}", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMatchBareStatements() {

    Assert.assertTrue(runPredicate("match { foo == 0 : bFalse, foo == 1 : bTrue, default : false }",
        new HashMap() {{
          put("foo", 1);
          put("bFalse", false);
          put("bTrue", true);
        }}));

    Assert.assertEquals("warning",
        run("match{ threat.triage.level < 10 : 'info', threat.triage.level < 20 : 'warning', default : 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWithFunction() {
    Assert.assertEquals("WARNING",
        run("match{ threat.triage.level < 10 : 'info', threat.triage.level < 20 : TO_UPPER('warning'), default : 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWithFunctionMultiArgs() {
    Assert.assertEquals("false",
        run("match{ threat.triage.level < 10 : 'info', threat.triage.level < 20 : TO_STRING(IS_ENCODING(other,'BASE32')), default : 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
              put("other", "value");
            }}));

    Assert.assertEquals(false,
        run("match{ threat.triage.level < 10 : 'info', threat.triage.level < 20 : IS_ENCODING(other,'BASE32'), default : 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
              put("other", "value");
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testLogical() {

    Assert.assertTrue(
        runPredicate("match { foo == 0  OR bar == 'yes' : ()-> true, default : ()-> false }",
            new HashMap() {{
              put("foo", 1);
              put("bar", "yes");
            }}));

    Assert.assertTrue(
        runPredicate("match { foo == 0  AND bar == 'yes' : ()-> true, default : ()-> false }",
            new HashMap() {{
              put("foo", 0);
              put("bar", "yes");
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMatchErrorNoDefault() {

    boolean caught = false;
    try {
      run("match{ foo > 100 : 'greater than 100', foo > 200 : 'greater than 200' }",
          new HashMap() {{
            put("foo", 50);
          }});
    } catch (ParseException pe) {
      caught = true;
    }
    Assert.assertTrue(caught);

  }


  @Test
  @SuppressWarnings("unchecked")
  public void testShortCircut() {

   Assert.assertEquals("ok",  run("match{ foo > 100 : THROW('oops'), foo > 200 : THROW('oh no'), default : 'ok' }",
          new HashMap() {{
            put("foo", 50);
          }}));
  }

}
