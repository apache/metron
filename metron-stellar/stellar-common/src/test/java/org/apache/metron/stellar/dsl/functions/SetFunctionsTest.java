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

import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SetFunctionsTest {

  @Test
  public void multisetInitTest_wrongType() {
    assertThrows(
        ParseException.class,
        () -> StellarProcessorUtils.run("MULTISET_INIT({ 'foo' : 'bar'})", new HashMap<>()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetInitTest() {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT()", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int initialization
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT([1,2,3,2])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1,(int)s.get(1));
      assertTrue(s.containsKey(2));
      assertEquals(2,(int)s.get(2));
      assertTrue(s.containsKey(3));
      assertEquals(1,(int)s.get(3));
    }
    //string initialization
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT(['one','two','three','two'])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey("one"));
      assertEquals(1,(int)s.get("one"));
      assertTrue(s.containsKey("two"));
      assertEquals(2,(int)s.get("two"));
      assertTrue(s.containsKey("three"));
      assertEquals(1,(int)s.get("three"));

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetAddTest() {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT(), 1)", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1,(int)s.get(1));
    }
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(null, 1)", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1,(int)s.get(1));
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT([1,2,3,4,4]), 4)", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1,(int)s.get(1));
      assertTrue(s.containsKey(2));
      assertEquals(1,(int)s.get(2));
      assertTrue(s.containsKey(3));
      assertEquals(1,(int)s.get(3));
      assertTrue(s.containsKey(4));
      assertEquals(3,(int)s.get(4));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT(['one','two','three', 'four', 'four']), 'four')", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.containsKey("one"));
      assertEquals(1,(int)s.get("one"));
      assertTrue(s.containsKey("two"));
      assertEquals(1,(int)s.get("two"));
      assertTrue(s.containsKey("three"));
      assertEquals(1,(int)s.get("three"));
      assertTrue(s.containsKey("four"));
      assertEquals(3,(int)s.get("four"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetRemoveTest() {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT([1]), 1)", new HashMap<>());
      assertEquals(0, s.size());
    }
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(null, 1)", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT([1,2,3,2]), 2)", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1, (int)s.get(1));
      assertTrue(s.containsKey(2));
      assertEquals(1, (int)s.get(2));
      assertTrue(s.containsKey(3));
      assertEquals(1, (int)s.get(3));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT(['one','two','three', 'two']), 'two')", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey("one"));
      assertEquals(1, (int)s.get("one"));
      assertTrue(s.containsKey("two"));
      assertEquals(1, (int)s.get("two"));
      assertTrue(s.containsKey("three"));
      assertEquals(1, (int)s.get("three"));
    }
  }

  @Test
  public void multisetMergeTest_wrongType() {
    assertThrows(
        ParseException.class,
        () -> StellarProcessorUtils.run("MULTISET_MERGE({ 'bar' : 'foo' } )", new HashMap<>()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetMergeTest() {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT(), MULTISET_INIT(null), null])", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT([1,2]), MULTISET_INIT([2,3]), null, MULTISET_INIT()])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey(1));
      assertEquals(1, (int)s.get(1));
      assertTrue(s.containsKey(2));
      assertEquals(2, (int)s.get(2));
      assertTrue(s.containsKey(3));
      assertEquals(1, (int)s.get(3));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>)StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT(['one','two']), MULTISET_INIT(['two', 'three'])])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.containsKey("one"));
      assertEquals(1, (int)s.get("one"));
      assertTrue(s.containsKey("two"));
      assertEquals(2, (int)s.get("two"));
      assertTrue(s.containsKey("three"));
      assertEquals(1, (int)s.get("three"));
    }
  }

  @Test
  public void setInitTest_wrongType() {
    assertThrows(
        ParseException.class,
        () -> StellarProcessorUtils.run("SET_INIT({ 'foo' : 2})", new HashMap<>()));
  }

  @Test
  public void setInitTest() {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT()", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int initialization
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT([1,2,3])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.contains(1));
      assertTrue(s.contains(2));
      assertTrue(s.contains(3));
    }
    //string initialization
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT(['one','two','three'])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.contains("one"));
      assertTrue(s.contains("two"));
      assertTrue(s.contains("three"));
    }
  }

  @Test
  public void multisetToSetTest() {
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT(), 1))", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.contains(1));
    }
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(null, 1))", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.contains(1));
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT([1,2,3]), 4))", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.contains(1));
      assertTrue(s.contains(2));
      assertTrue(s.contains(3));
      assertTrue(s.contains(4));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT(['one','two','three']), 'four'))", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.contains("one"));
      assertTrue(s.contains("two"));
      assertTrue(s.contains("three"));
      assertTrue(s.contains("four"));
    }
  }

  @Test
  public void setAddTest() {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT(), 1)", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.contains(1));
    }
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(null, 1)", new HashMap<>());
      assertEquals(1, s.size());
      assertTrue(s.contains(1));
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT([1,2,3]), 4)", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.contains(1));
      assertTrue(s.contains(2));
      assertTrue(s.contains(3));
      assertTrue(s.contains(4));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT(['one','two','three']), 'four')", new HashMap<>());
      assertEquals(4, s.size());
      assertTrue(s.contains("one"));
      assertTrue(s.contains("two"));
      assertTrue(s.contains("three"));
      assertTrue(s.contains("four"));
    }
  }

  @Test
  public void setRemoveTest() {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT([1]), 1)", new HashMap<>());
      assertEquals(0, s.size());
    }
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(null, 1)", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT([1,2,3]), 2)", new HashMap<>());
      assertEquals(2, s.size());
      assertTrue(s.contains(1));
      assertTrue(s.contains(3));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT(['one','two','three']), 'three')", new HashMap<>());
      assertEquals(2, s.size());
      assertTrue(s.contains("one"));
      assertTrue(s.contains("two"));
    }
  }

  @Test
  public void setMergeTest_wrongType() {
    assertThrows(
        ParseException.class,
        () -> StellarProcessorUtils.run("SET_MERGE({ 'foo' : 'bar'} )", new HashMap<>()));
  }

  @Test
  public void setMergeTest() {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT(), SET_INIT(null), null])", new HashMap<>());
      assertEquals(0, s.size());
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT([1,2]), SET_INIT([3]), null, SET_INIT()])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.contains(1));
      assertTrue(s.contains(2));
      assertTrue(s.contains(3));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT(['one','two']), SET_INIT(['three'])])", new HashMap<>());
      assertEquals(3, s.size());
      assertTrue(s.contains("one"));
      assertTrue(s.contains("two"));
      assertTrue(s.contains("three"));
    }
  }
}
