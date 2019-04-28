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
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SetFunctionsTest {

  @Test(expected=ParseException.class)
  @SuppressWarnings("unchecked")
  public void multisetInitTest_wrongType() throws Exception {
    Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT({ 'foo' : 'bar'})", new HashMap<>());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetInitTest() throws Exception {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT()", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int initialization
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT([1,2,3,2])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1,(int)s.get(1));
      Assert.assertTrue(s.containsKey(2));
      Assert.assertEquals(2,(int)s.get(2));
      Assert.assertTrue(s.containsKey(3));
      Assert.assertEquals(1,(int)s.get(3));
    }
    //string initialization
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_INIT(['one','two','three','two'])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey("one"));
      Assert.assertEquals(1,(int)s.get("one"));
      Assert.assertTrue(s.containsKey("two"));
      Assert.assertEquals(2,(int)s.get("two"));
      Assert.assertTrue(s.containsKey("three"));
      Assert.assertEquals(1,(int)s.get("three"));

    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetAddTest() throws Exception {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT(), 1)", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1,(int)s.get(1));
    }
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(null, 1)", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1,(int)s.get(1));
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT([1,2,3,4,4]), 4)", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1,(int)s.get(1));
      Assert.assertTrue(s.containsKey(2));
      Assert.assertEquals(1,(int)s.get(2));
      Assert.assertTrue(s.containsKey(3));
      Assert.assertEquals(1,(int)s.get(3));
      Assert.assertTrue(s.containsKey(4));
      Assert.assertEquals(3,(int)s.get(4));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_ADD(MULTISET_INIT(['one','two','three', 'four', 'four']), 'four')", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.containsKey("one"));
      Assert.assertEquals(1,(int)s.get("one"));
      Assert.assertTrue(s.containsKey("two"));
      Assert.assertEquals(1,(int)s.get("two"));
      Assert.assertTrue(s.containsKey("three"));
      Assert.assertEquals(1,(int)s.get("three"));
      Assert.assertTrue(s.containsKey("four"));
      Assert.assertEquals(3,(int)s.get("four"));
    }
  }
@Test
@SuppressWarnings("unchecked")
  public void multisetRemoveTest() throws Exception {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT([1]), 1)", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(null, 1)", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT([1,2,3,2]), 2)", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1, (int)s.get(1));
      Assert.assertTrue(s.containsKey(2));
      Assert.assertEquals(1, (int)s.get(2));
      Assert.assertTrue(s.containsKey(3));
      Assert.assertEquals(1, (int)s.get(3));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_REMOVE(MULTISET_INIT(['one','two','three', 'two']), 'two')", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey("one"));
      Assert.assertEquals(1, (int)s.get("one"));
      Assert.assertTrue(s.containsKey("two"));
      Assert.assertEquals(1, (int)s.get("two"));
      Assert.assertTrue(s.containsKey("three"));
      Assert.assertEquals(1, (int)s.get("three"));
    }
  }

  @Test(expected=ParseException.class)
  @SuppressWarnings("unchecked")
  public void multisetMergeTest_wrongType() throws Exception {

    Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_MERGE({ 'bar' : 'foo' } )", new HashMap<>());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void multisetMergeTest() throws Exception {
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT(), MULTISET_INIT(null), null])", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int
    {
      Map<Object, Integer> s = (Map<Object, Integer>) StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT([1,2]), MULTISET_INIT([2,3]), null, MULTISET_INIT()])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey(1));
      Assert.assertEquals(1, (int)s.get(1));
      Assert.assertTrue(s.containsKey(2));
      Assert.assertEquals(2, (int)s.get(2));
      Assert.assertTrue(s.containsKey(3));
      Assert.assertEquals(1, (int)s.get(3));
    }
    //string
    {
      Map<Object, Integer> s = (Map<Object, Integer>)StellarProcessorUtils.run("MULTISET_MERGE([MULTISET_INIT(['one','two']), MULTISET_INIT(['two', 'three'])])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.containsKey("one"));
      Assert.assertEquals(1, (int)s.get("one"));
      Assert.assertTrue(s.containsKey("two"));
      Assert.assertEquals(2, (int)s.get("two"));
      Assert.assertTrue(s.containsKey("three"));
      Assert.assertEquals(1, (int)s.get("three"));
    }
  }

  @Test(expected=ParseException.class)
  public void setInitTest_wrongType() throws Exception {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT({ 'foo' : 2})", new HashMap<>());
  }

  @Test
  public void setInitTest() throws Exception {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT()", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int initialization
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT([1,2,3])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.contains(1));
      Assert.assertTrue(s.contains(2));
      Assert.assertTrue(s.contains(3));
    }
    //string initialization
    {
      Set s = (Set) StellarProcessorUtils.run("SET_INIT(['one','two','three'])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.contains("one"));
      Assert.assertTrue(s.contains("two"));
      Assert.assertTrue(s.contains("three"));
    }
  }

  @Test
  public void multisetToSetTest() throws Exception {
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT(), 1))", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.contains(1));
    }
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(null, 1))", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.contains(1));
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT([1,2,3]), 4))", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.contains(1));
      Assert.assertTrue(s.contains(2));
      Assert.assertTrue(s.contains(3));
      Assert.assertTrue(s.contains(4));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("MULTISET_TO_SET(MULTISET_ADD(MULTISET_INIT(['one','two','three']), 'four'))", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.contains("one"));
      Assert.assertTrue(s.contains("two"));
      Assert.assertTrue(s.contains("three"));
      Assert.assertTrue(s.contains("four"));
    }
  }

  @Test
  public void setAddTest() throws Exception {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT(), 1)", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.contains(1));
    }
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(null, 1)", new HashMap<>());
      Assert.assertEquals(1, s.size());
      Assert.assertTrue(s.contains(1));
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT([1,2,3]), 4)", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.contains(1));
      Assert.assertTrue(s.contains(2));
      Assert.assertTrue(s.contains(3));
      Assert.assertTrue(s.contains(4));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_ADD(SET_INIT(['one','two','three']), 'four')", new HashMap<>());
      Assert.assertEquals(4, s.size());
      Assert.assertTrue(s.contains("one"));
      Assert.assertTrue(s.contains("two"));
      Assert.assertTrue(s.contains("three"));
      Assert.assertTrue(s.contains("four"));
    }
  }

  @Test
  public void setRemoveTest() throws Exception {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT([1]), 1)", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(null, 1)", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT([1,2,3]), 2)", new HashMap<>());
      Assert.assertEquals(2, s.size());
      Assert.assertTrue(s.contains(1));
      Assert.assertTrue(s.contains(3));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_REMOVE(SET_INIT(['one','two','three']), 'three')", new HashMap<>());
      Assert.assertEquals(2, s.size());
      Assert.assertTrue(s.contains("one"));
      Assert.assertTrue(s.contains("two"));
    }
  }

  @Test(expected=ParseException.class)
  public void setMergeTest_wrongType() throws Exception {
    Set s = (Set) StellarProcessorUtils.run("SET_MERGE({ 'foo' : 'bar'} )", new HashMap<>());
  }

  @Test
  public void setMergeTest() throws Exception {
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT(), SET_INIT(null), null])", new HashMap<>());
      Assert.assertEquals(0, s.size());
    }
    //int
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT([1,2]), SET_INIT([3]), null, SET_INIT()])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.contains(1));
      Assert.assertTrue(s.contains(2));
      Assert.assertTrue(s.contains(3));
    }
    //string
    {
      Set s = (Set) StellarProcessorUtils.run("SET_MERGE([SET_INIT(['one','two']), SET_INIT(['three'])])", new HashMap<>());
      Assert.assertEquals(3, s.size());
      Assert.assertTrue(s.contains("one"));
      Assert.assertTrue(s.contains("two"));
      Assert.assertTrue(s.contains("three"));
    }
  }
}
