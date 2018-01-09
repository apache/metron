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


import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.*;


public class OrdinalFunctionsTest {


  private static Context context;

  @Before
  public void setup() throws Exception {
    context = new Context.Builder().build();
  }
  @Test
  public void testMaxOfMixedNumerical() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56.0);
      add(56.3);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertEquals(56.3, res);
  }

  @Test
  public void testMinOfMixedNumerical() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56.0);
      add(457L);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertEquals(res, 12L);
  }
  @Test
  public void testMaxOfStringList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add("value3");
      add("value1");
      add("23");
      add("value2");
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals("value3"));
  }

  @Test
  public void testMaxOfIntegerList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12);
      add(56);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals(56));
  }

  @Test
  public void testMaxWithVarList() throws Exception {

    Object res = run("MAX([string1,string2])", ImmutableMap.of("string1","abc","string2","def"));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals("def"));
  }

  @Test
  public void testMinWithNullInList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(145);
      add(null);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals(145));
  }

  @Test
  public void testAllNullList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(null);
      add(null);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNull(res);
  }

  @Test
  public void testMinOfIntegerList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(56);
      add(12);
      add(23);
      add(null);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals(12));
  }


  @Test
  public void testMaxOfLongList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56L);
      add(457L);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals(457L));
  }

  @Test
  public void testMaxOfMixedList() throws Exception {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12);
      add("string");
      add(457L);
    }};

    Object res = null;

    try {
      res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    } catch(IllegalStateException e) {
      Assert.assertThat(e.getMessage(), is("Incomparable objects were submitted to MAX: class java.lang.String is incomparable to class java.lang.Long"));
      Assert.assertNull(res);
    }
  }

  @Test
  public void testSetInput() throws Exception {

    Set<Object> inputSet = new HashSet<Object>(){{
      add(14L);
      add(15.3d);
      add(15);
    }};

    Object res = run("MAX(input_set)", ImmutableMap.of("input_set", inputSet));
    Assert.assertNotNull(res);
    Assert.assertTrue(res.equals(15.3d));
  }

  @Test
  public void testNonComparableList() throws Exception {

    class TestObject {
      private String arg;
      public TestObject(String arg) {
        this.arg = arg;
      }
    }

    List<Object> inputList = new ArrayList<Object>(){{
      add(new TestObject("one"));
      add(new TestObject("two"));
      add(new TestObject("three"));
    }};

    Object res = null;

    try {
      res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    } catch(IllegalStateException e) {
      Assert.assertThat(e.getMessage(), is("Noncomparable object type org.apache.metron.stellar.dsl.functions.OrdinalFunctionsTest$1TestObject submitted to MIN"));
      Assert.assertNull(res);
    }
  }

  public Object run(String rule, Map<String, Object> variables) throws Exception {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x), x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }
}
