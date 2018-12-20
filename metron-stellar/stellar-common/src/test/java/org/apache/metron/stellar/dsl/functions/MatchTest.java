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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class MatchTest {

  // Short Circuit

  @Test
  @SuppressWarnings("unchecked")
  public void testMissingVariableFalsey() {
    Assert.assertTrue(runPredicate(
        "match{NOT(is_alert) => true, foo > 5 => false, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
    Assert.assertFalse(runPredicate(
        "match{is_alert => true, foo > 5 => false, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
    Assert.assertFalse(runPredicate(
        "match{foo > 5 => false, is_alert => true, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEmptyListFalsey() {
    Assert.assertTrue(runPredicate(
        "match{NOT([]) => true, foo > 5 => false, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
    Assert.assertFalse(runPredicate(
        "match{[] => true, foo > 5 => false, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testThreeTrueClausesFirstOnlyFires() {
    Assert.assertTrue(runPredicate(
        "match{foo > 0 => true, foo > 5 => false, foo > 10 => false, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testTwoClausesSecondFires() {
    Assert.assertTrue(runPredicate("match{foo < 0 => false, foo < 500 => true, default => false}",
        new HashMap() {{
          put("foo", 100);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testThreeClausesFirstFires() {
    List<String> list = (List<String>) run(
        "match{ foo > 100 => ['oops'], foo > 200 => ['oh no'], foo >= 500 => MAP(['ok', 'haha'], (a) -> TO_UPPER(a)), default => ['a']}",
        new HashMap() {{
          put("foo", 500);
        }});

    Assert.assertTrue(list.size() == 1);
    Assert.assertTrue(list.contains("oops"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testShortCircuitWithThrows() {

    Assert.assertEquals("ok",
        run("match{ foo > 100 => THROW('oops'), foo > 200 => THROW('oh no'), default => 'ok' }",
            new HashMap() {{
              put("foo", 50);
            }}));
  }


  // LAMBDAS
  @Test
  @SuppressWarnings("unchecked")
  public void testMatchLambda() {
    Assert.assertTrue(
        runPredicate("match { 1 >= 0 => ()-> true, default => ()->false }", new HashMap() {{
          put("foo", 0);
        }}));
    Assert.assertTrue(
        runPredicate("match { foo == 0 => ()-> true, default => ()-> false }", new HashMap() {{
          put("foo", 0);
        }}));

    Assert.assertFalse(
        runPredicate("match { foo == 0 => ()-> true, default => ()-> false }", new HashMap() {{
          put("foo", 1);
        }}));

    Assert.assertTrue(runPredicate(
        "match { foo == 0 => ()-> false, foo == 1 => ()-> true, default => ()-> false }",
        new HashMap() {{
          put("foo", 1);
        }}));

    Assert.assertTrue(runPredicate(
        "match { foo == 0 => ()-> bFalse, foo == 1 => ()-> bTrue, default => ()-> bFalse }",
        new HashMap() {{
          put("foo", 1);
          put("bFalse", false);
          put("bTrue", true);
        }}));

    Assert.assertTrue(runPredicate(
        "match { foo == 0 => ()-> bFalse, foo == 1 => ()-> bTrue, default => ()-> bFalse }",
        new HashMap() {{
          put("foo", 1);
          put("bFalse", false);
          put("bTrue", true);
        }}));

  }

  // GENERAL WITH MAP EVAL
  @Test
  @SuppressWarnings("unchecked")
  public void testMatchMAPEvaluation() {

    String expr = "match{ var1 =>  MAP(['foo', 'bar'], (x) -> TO_UPPER(x)), default => null }";

    Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar", "var1", true));

    Assert.assertTrue(o instanceof List);

    List<String> result = (List<String>) o;

    Assert.assertEquals(2, result.size());
    Assert.assertEquals("FOO", result.get(0));
    Assert.assertEquals("BAR", result.get(1));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void workingMatchWithMap() {
    Assert.assertEquals(Arrays.asList("OK", "HAHA"),
        run("match{ foo > 100 => THROW('oops'), foo > 200 => THROW('oh no'), foo >= 50 => MAP(['ok', 'haha'], (a) -> TO_UPPER(a)), default=> 'a' }",
            new HashMap() {{
              put("foo", 50);
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMapSmall() {
    List<String> ret = (List<String>) run(
        "match{ foo < 100 => ['oops'], default => MAP(['ok', 'haha'], (a) -> TO_UPPER(a))}",
        new HashMap() {{
          put("foo", 500);
        }});
    Assert.assertTrue(ret.size() == 2);
    Assert.assertTrue(ret.contains("OK"));
    Assert.assertTrue(ret.contains("HAHA"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMultiClauseMap() {
    run("match{ foo < 100 => ['oops'], foo < 200 => ['oh no'], foo >= 500 => MAP(['ok', 'haha'], (a) -> TO_UPPER(a)), default => ['a']}",
        new HashMap() {{
          put("foo", 500);
        }});
  }


  // REGEX
  @Test
  @SuppressWarnings("unchecked")
  public void testMatchRegexMatch() {
    final Map<String, String> variables = new HashMap<String, String>() {{
      put("numbers", "12345");
      put("numberPattern", "\\d(\\d)(\\d).*");
      put("letters", "abcde");
      put("empty", "");
    }};

    Assert.assertTrue(
        runPredicate("match{ REGEXP_MATCH(numbers,numberPattern)=> true, default => false}",
            new DefaultVariableResolver(variables::get, variables::containsKey)));
    Assert.assertFalse(
        runPredicate("match{ REGEXP_MATCH(letters,numberPattern) => true, default =>false}",
            new DefaultVariableResolver(variables::get, variables::containsKey)));
  }

  // BARE STATEMENTS
  @Test
  @SuppressWarnings("unchecked")
  public void testMatchBareStatements() {

    Assert.assertTrue(
        runPredicate("match { foo == 0 => bFalse, foo == 1 => bTrue, default => false }",
            new HashMap() {{
              put("foo", 1);
              put("bFalse", false);
              put("bTrue", true);
            }}));

    Assert.assertEquals("warning",
        run("match{ threat.triage.level < 10 => 'info', threat.triage.level < 20 => 'warning', default => 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
            }}));
  }

  // FUNCTIONS
  @Test
  @SuppressWarnings("unchecked")
  public void testWithFunction() {
    Assert.assertEquals("WARNING",
        run("match{ threat.triage.level < 10 => 'info', threat.triage.level < 20 => TO_UPPER('warning'), default => 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testWithFunctionMultiArgs() {
    Assert.assertEquals("false",
        run("match{ threat.triage.level < 10 => 'info', threat.triage.level < 20 => TO_STRING(IS_ENCODING(other,'BASE32')), default => 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
              put("other", "value");
            }}));

    Assert.assertEquals(false,
        run("match{ threat.triage.level < 10 => 'info', threat.triage.level < 20 => IS_ENCODING(other,'BASE32'), default => 'critical' }",
            new HashMap() {{
              put("threat.triage.level", 15);
              put("other", "value");
            }}));

  }


  // LOGICAL EXPRESSIONS IN CHECKS
  @Test
  @SuppressWarnings("unchecked")
  public void testLogical() {

    Assert.assertTrue(
        runPredicate("match { foo == 0  OR bar == 'yes' => ()-> true, default => ()-> false }",
            new HashMap() {{
              put("foo", 1);
              put("bar", "yes");
            }}));

    Assert.assertTrue(
        runPredicate("match { foo == 0  AND bar == 'yes' => ()-> true, default => ()-> false }",
            new HashMap() {{
              put("foo", 0);
              put("bar", "yes");
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testTernaryFuncWithoutIfCheck() {
    Assert.assertEquals("a",
        run("match{ foo == 5 ? true : false => 'a', default => 'ok' }", new HashMap() {{
          put("foo", 5);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testTernaryFuncAsMatchAction() {
    Assert.assertEquals(false, run("match{ threat.triage.level < 10 => 'info', threat.triage.level < 20 => IS_ENCODING(other,'BASE32')? true : false, default => 'critical' }",
        new HashMap() {{
          put("threat.triage.level", 15);
          put("other", "value");
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVariableIFCheck() {
    Assert.assertEquals("a",
        run("match{ IF foo == 5 THEN true ELSE false => 'a', default => 'ok' }", new HashMap() {{
          put("foo", 5);
        }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testIfThenElseAction() {
    Assert.assertEquals(2,run("match{ foo == true => IF bar THEN 1 ELSE 2, default => 0}", new HashMap(){{
      put("foo",true);
      put("bar",false);
    }}));
  }
  @Test
  @SuppressWarnings("unchecked")
  public void testVariableOnly() {
    Assert.assertEquals("a", run("match{ foo => 'a', default => null}", new HashMap() {{
      put("foo", true);
    }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVariableEqualsCheck() {
    Assert.assertEquals("a", run("match{ foo == 5 => 'a', default => 'ok' }", new HashMap() {{
      put("foo", 5);
    }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testVariableOnlyCheckWithDefault() {
    Assert.assertEquals("a", run("match{ foo => 'a', default => 'b' }", new HashMap() {{
      put("foo", true);
    }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHandleVariableEqualsCheckWithDefault() {
    Assert.assertEquals("a", run("match{ foo == true => 'a', default=> 'b' }", new HashMap() {{
      put("foo", true);
    }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testNullInCheckedReturnNull() {
    Assert.assertNull(
        run(
            "match{ foo == null => null, foo == true => 'not that null', default => 'really not that null'}",
            new HashMap(){{
              put("foo",null);
            }}));
  }

  // SYNTAX ERRORS

  @Test(expected = ParseException.class)
  @SuppressWarnings("unchecked")
  public void testMatchErrorNoDefault() {

    run("match{ foo > 100 => 'greater than 100', foo > 200 => 'greater than 200' }",
        new HashMap() {{
          put("foo", 50);
        }});

  }


  @Test(expected = ParseException.class)
  @SuppressWarnings("unchecked")
  public void testNestedMatchNotSupportted() {
    // we cannot nest short circuit types in stellar
    Assert.assertEquals(false,
        run("match{  x == 0 => match{ y == 10 => false, default => true}, default => true}",
            new HashMap() {{
              put("x", 0);
              put("y", 10);
            }}));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testReturnList() {
    Object o = run("match{ foo > 100 => ['oops'],default => ['a']}", new HashMap() {{
      put("foo", 500);
    }});
    List l = (List) o;
    Assert.assertTrue(l.size() == 1);
  }
}
