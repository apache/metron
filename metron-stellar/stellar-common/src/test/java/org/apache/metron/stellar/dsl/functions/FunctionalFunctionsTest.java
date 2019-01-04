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
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;

public class FunctionalFunctionsTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testZipLongest_boundary() {
    for (String expr : ImmutableList.of( "ZIP_LONGEST()"
            , "ZIP_LONGEST( null, null )"
            , "ZIP_LONGEST( [], null )"
            , "ZIP_LONGEST( [], [] )"
            , "ZIP_LONGEST( null, [] )"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, new HashMap<>());
      Assert.assertEquals(0, o.size());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testZip_longest() {
    Map<String, Object> variables = ImmutableMap.of(
            "list1" , ImmutableList.of(1, 2, 3)
            ,"list2", ImmutableList.of(4, 5, 6, 7)
    );
    for (String expr : ImmutableList.of( "ZIP_LONGEST(list1)"
            , "ZIP_LONGEST( [1, 2, 3])"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, variables);
      Assert.assertEquals(3, o.size());
      for (int i = 0; i < 3; ++i) {
        List l = o.get(i);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals(i+1, l.get(0));
      }

    }

    for (String expr : ImmutableList.of( "ZIP_LONGEST(list1, list2)"
            , "ZIP_LONGEST( [1, 2, 3], [4, 5, 6, 7] )"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, variables);
      Assert.assertEquals(4, o.size());
      for (int i = 0; i < 3; ++i) {
        List l = o.get(i);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals(i+1, l.get(0));
        Assert.assertEquals(i+4, l.get(1));
      }
      {
        int i = 3;
        List l = o.get(i);
        Assert.assertEquals(2, l.size());
        Assert.assertNull(l.get(0));
        Assert.assertEquals(i+4, l.get(1));
      }
    }


    for (String expr : ImmutableList.of(
             "REDUCE(ZIP_LONGEST(list2, list1), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)"
            , "REDUCE(ZIP_LONGEST( [1, 2, 3], [4, 5, 6, 7] ), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)"
            , "REDUCE(ZIP_LONGEST(list1, list2), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)" //this works because stellar treats nulls as 0 in arithmetic operations.
            , "REDUCE(ZIP_LONGEST(list1, list2), (s, x) -> s + (GET_FIRST(x) == null?0:GET_FIRST(x)) * (GET_LAST(x) == null?0:GET_LAST(x)), 0)" //with proper guarding NOT assuming stellar peculiarities
    )
            )
    {
      int o = (int) run(expr, variables);
      Assert.assertEquals(1*4 + 2*5 + 3*6, o, 1e-7);
    }

  }

  @Test
  @SuppressWarnings("unchecked")
  public void testZip_boundary() {
    for (String expr : ImmutableList.of( "ZIP()"
            , "ZIP( null, null )"
            , "ZIP( [], null )"
            , "ZIP( [], [] )"
            , "ZIP( null, [] )"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, new HashMap<>());
      Assert.assertEquals(0, o.size());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testZip() {
    Map<String, Object> variables = ImmutableMap.of(
            "list1" , ImmutableList.of(1, 2, 3)
            ,"list2", ImmutableList.of(4, 5, 6)
    );

    for (String expr : ImmutableList.of( "ZIP(list1)"
            , "ZIP( [1, 2, 3])"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, variables);
      Assert.assertEquals(3, o.size());
      for (int i = 0; i < 3; ++i) {
        List l = o.get(i);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals(i+1, l.get(0));
      }

    }
    for (String expr : ImmutableList.of( "ZIP(list1, list2)"
            , "ZIP( [1, 2, 3], [4, 5, 6] )"
            , "ZIP( [1, 2, 3], [4, 5, 6, 7] )"
    )
            )
    {
      List<List<Object>> o = (List<List<Object>>) run(expr, variables);
      Assert.assertEquals(3, o.size());
      for (int i = 0; i < 3; ++i) {
        List l = o.get(i);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals(i+1, l.get(0));
        Assert.assertEquals(i+4, l.get(1));
      }
    }

    for (String expr : ImmutableList.of(
            "REDUCE(ZIP(list1, list2), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)"
            , "REDUCE(ZIP( [1, 2, 3], [4, 5, 6] ), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)"
            , "REDUCE(ZIP( [1, 2, 3], [4, 5, 6, 7] ), (s, x) -> s + GET_FIRST(x) * GET_LAST(x), 0)"
    )
            )
    {
      int o = (int) run(expr, variables);
      Assert.assertEquals(1*4 + 2*5 + 3*6, o, 1e-7);
    }

  }

  @Test
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
  public void testMap_null() {
    for (String expr : ImmutableList.of( "MAP([ 1, 2, null], x -> if x == null then 0 else 2*x )"
                                       , "MAP([ 1, 2, null], x -> x == null ? 0 : 2*x )"
                                       , "MAP([ 1, foo, baz], x -> x == null ? 0 : 2*x )"
    )
            )
    {
      Map<String,Object> variableMap = new HashMap<String,Object>(){{
        put("foo",2);
        put("bar", 3);
        put("baz",null);
      }};
      Object o = run(expr,variableMap);
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(3, result.size());
      Assert.assertEquals(2, result.get(0));
      Assert.assertEquals(4, result.get(1));
      Assert.assertEquals(0, result.get(2));
    }
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testMap() {
    for (String expr : ImmutableList.of( "MAP([ 'foo', 'bar'], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, 'bar'], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, bar], (x) -> TO_UPPER(x) )"
                                       , "MAP([ foo, bar], x -> TO_UPPER(x) )"
                                       , "MAP([ foo, bar], x -> true?TO_UPPER(x):THROW('error') )"
                                       , "MAP([ foo, bar], x -> false?THROW('error'):TO_UPPER(x) )"
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
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
  public void testFilter_shortcircuit() {
    for (String expr : ImmutableList.of("FILTER([ 'foo'], item -> item == 'foo' or THROW('exception') )"
                                       ,"FILTER([ 'foo'], (item) -> item == 'foo' or THROW('exception') )"
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
  @SuppressWarnings("unchecked")
  public void testFilter_null() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', null], item -> item == null )"
                                       ,"FILTER([ 'foo', baz], (item) -> item == null )"
                                       )
        )
    {
      Map<String,Object> variableMap = new HashMap<String,Object>(){{
        put("foo","foo");
        put("bar","bar");
        put("baz",null);
      }};
      Object o = run(expr,variableMap);
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(1, result.size());
      Assert.assertEquals(null, result.get(0));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFilter_notnull() {
    for (String expr : ImmutableList.of("FILTER([ 'foo', null], item -> item != null )"
                                       ,"FILTER([ 'foo', baz], (item) -> item != null )"
                                       ,"FILTER([ foo, baz], (item) -> item != null )"
                                       )
        )
    {
      Map<String,Object> variableMap = new HashMap<String,Object>(){{
        put("foo","foo");
        put("bar","bar");
        put("baz",null);
      }};
      Object o = run(expr,variableMap);
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(1, result.size());
      Assert.assertEquals("foo", result.get(0));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
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
  @SuppressWarnings("unchecked")
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
      Map<String,Object> variableMap = new HashMap<String,Object>(){{
        put("foo",1);
        put("bar", 2);
        put("baz",null);
      }};
      Object o = run(expr,variableMap);
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(6, result.intValue());
    }
  }

  @Test
  public void testReduce() {
    for (String expr : ImmutableList.of("REDUCE([ 1, 2, 3 ], (x, y) -> x + y , 0 )"
                                       ,"REDUCE([ foo, bar, 3 ], (x, y) -> x + y , 0 )"
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
  @SuppressWarnings("unchecked")
  public void testReduce_on_various_list_sizes() {
    {
      String expr = "REDUCE([ 1, 2, 3, 4 ], (x, y) -> x + y , 0 )";
      Object o = run(expr, ImmutableMap.of());
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(10, result.intValue());
    }
    {
      String expr = "REDUCE([ 1, 2 ], (x, y) -> x + y , 0 )";
      Object o = run(expr, ImmutableMap.of());
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(3, result.intValue());
    }
    {
      String expr = "REDUCE([ 1 ], (x, y) -> x + y , 0 )";
      Object o = run(expr, ImmutableMap.of());
      Assert.assertTrue(o instanceof Number);
      Number result = (Number) o;
      Assert.assertEquals(1, result.intValue());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testReduce_NonNumeric() {
    for (String expr : ImmutableList.of("REDUCE([ 'foo', 'bar', 'grok'], (x, y) -> LIST_ADD(x, y), [] )"
                                       )
        )
    {
      Object o = run(expr, ImmutableMap.of("foo", 1, "bar", 2,"x",0,"y",0));
      Assert.assertTrue(o instanceof List);
      List<String> result = (List<String>) o;
      Assert.assertEquals(3, result.size());
      Assert.assertEquals("foo", result.get(0));
      Assert.assertEquals("bar", result.get(1));
      Assert.assertEquals("grok", result.get(2));
    }
  }

  @Test
  public void testReduce_returns_null_when_less_than_3_args() {
    {
      String expr = "REDUCE([ 1, 2, 3 ], (x, y) -> LIST_ADD(x, y))";
      Assert.assertThat(run(expr, ImmutableMap.of()), CoreMatchers.equalTo(null));
    }
    {
      String expr = "REDUCE([ 1, 2, 3 ])";
      Assert.assertThat(run(expr, ImmutableMap.of()), CoreMatchers.equalTo(null));
    }
  }

}
