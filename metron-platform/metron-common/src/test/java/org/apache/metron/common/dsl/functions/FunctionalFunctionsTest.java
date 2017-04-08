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
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.apache.metron.common.utils.StellarProcessorUtils.run;

public class FunctionalFunctionsTest {

  @Test
  public void testRecursive() {
    for (String expr : ImmutableList.of( "MAP(list, inner_list -> REDUCE(inner_list, (x, y) -> x + y, 0) )"
                                       , "MAP(list, (inner_list) -> REDUCE(inner_list, (x, y) -> x + y, 0) )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("list", ImmutableList.of(ImmutableList.of(1, 2, 3), ImmutableList.of(4, 5, 6))));
      Assert.assertTrue(o instanceof List);
      List<Number> result = (List<Number>) o;
      Assert.assertEquals(2, result.size());
      Assert.assertEquals(6, result.get(0));
      Assert.assertEquals(15, result.get(1));
    }
  }

  @Test
  public void testMap_null() {
    for (String expr : ImmutableList.of( "MAP([ 1, 2, null], x -> if x == null then 0 else 2*x )"
                                       , "MAP([ 1, 2, null], x -> x == null ? 0 : 2*x )"
                                       , "MAP([ 1, foo, baz], x -> x == null ? 0 : 2*x )"
    )
            )
    {
      Object o = run(expr, ImmutableMap.of("foo", 2, "bar", 3));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(3, result.size());
      Assert.assertEquals(2, result.get(0));
      Assert.assertEquals(4, result.get(1));
      Assert.assertEquals(0, result.get(2));
    }
  }


  @Test
  public void testMap() {
    for (String expr : ImmutableList.of( "MAP([ 'foo', 'bar'], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, 'bar'], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, bar], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, bar], x -> TO_UPPER(x) )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(2, result.size());
      Assert.assertEquals("FOO", result.get(0));
      Assert.assertEquals("BAR", result.get(1));
    }
  }


  @Test
  public void testMap_conditional() {
    for (String expr : ImmutableList.of("MAP([ 'foo', 'bar'], (item) -> item == 'foo' )"
                                       ,"MAP([ foo, bar], (item) -> item == 'foo' )"
                                       ,"MAP([ foo, bar], (item) -> item == foo )"
                                       ,"MAP([ foo, bar], item -> item == foo )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<Boolean> result = (List<Boolean>) o;
      Assert.assertEquals(2, result.size());
      Assert.assertEquals(true, result.get(0));
      Assert.assertEquals(false, result.get(1));
    }
  }

  @Test
  public void testFilter() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', 'bar'], (item) -> item == 'foo' )"
                                       ,"FILTER([ 'foo', bar], (item) -> item == 'foo' )"
                                       ,"FILTER([ foo, bar], (item) -> item == 'foo' )"
                                       ,"FILTER([ foo, bar], (item) -> (item == 'foo' && true) )"
                                       ,"FILTER([ foo, bar], (item) -> if item == 'foo' then true else false )"
                                       ,"FILTER([ foo, bar], item -> if item == 'foo' then true else false )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(1, result.size());
      Assert.assertEquals("foo", result.get(0));
    }
  }


  @Test
  public void testFilter_null() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', null], item -> item == null )"
                                       ,"FILTER([ 'foo', baz], (item) -> item == null )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(1, result.size());
      Assert.assertEquals(null, result.get(0));
    }
  }

  @Test
  public void testFilter_notnull() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', null], item -> item != null )"
                                       ,"FILTER([ 'foo', baz], (item) -> item != null )"
                                       ,"FILTER([ foo, baz], (item) -> item != null )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(1, result.size());
      Assert.assertEquals("foo", result.get(0));
    }
  }

  @Test
  public void testFilter_none() {
    for (String expr : ImmutableList.of( "FILTER([ foo, bar], () -> false  )"
                                       , "FILTER([ 'foo', 'bar'], (item)-> false )"
                                       ,"FILTER([ 'foo', bar], (item ) -> false )"
                                       ,"FILTER([ foo, bar], (item) -> false )"
                                       ,"FILTER([ foo, bar], item -> false )"

                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(0, result.size());
    }
  }

  @Test
  public void testFilter_all() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', 'bar'], (item) -> true )"
                                       ,"FILTER([ 'foo', bar], (item) -> true )"
                                       ,"FILTER([ foo, bar], (item) -> true )"
                                       ,"FILTER([ foo, bar], item -> true )"
                                       ,"FILTER([ foo, bar], ()-> true )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", "foo", "bar", "bar"));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(2, result.size());
      Assert.assertEquals("foo", result.get(0));
      Assert.assertEquals("bar", result.get(1));
    }
  }

  @Test
  public void testReduce_null() {
    for (String expr : ImmutableList.of("REDUCE([ 1, 2, 3, null], (x, y) -> if y != null then x + y else x , 0 )"
                                       ,"REDUCE([ foo, bar, 3, baz], (sum, y) -> if y != null then sum + y else sum, 0 )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", 1, "bar", 2));
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(6, result.intValue());
    }
  }

  @Test
  public void testReduce() {
    for (String expr : ImmutableList.of("REDUCE([ 1, 2, 3], (x, y) -> x + y , 0 )"
                                       ,"REDUCE([ foo, bar, 3], (x, y) -> x + y , 0 )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", 1, "bar", 2));
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(6, result.intValue());
    }
  }

  @Test
  public void testReduce_NonNumeric() {
    for (String expr : ImmutableList.of("REDUCE([ 'foo', 'bar', 'grok'], (x, y) -> LIST_ADD(x, y), [] )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", 1, "bar", 2));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("foo", result.get(0));
      Assert.assertEquals("bar", result.get(1));
      Assert.assertEquals("grok", result.get(2));
    }
  }
}
