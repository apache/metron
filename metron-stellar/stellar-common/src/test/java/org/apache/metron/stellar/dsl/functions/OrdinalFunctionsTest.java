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
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class OrdinalFunctionsTest {


  private static Context context;

  @BeforeEach
  public void setup() throws Exception {
    context = new Context.Builder().build();
  }
  @Test
  public void testMaxOfMixedNumerical() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56.0);
      add(56.3);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(56.3, res);
  }

  @Test
  public void testMinOfMixedNumerical() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56.0);
      add(457L);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(res, 12L);
  }
  @Test
  public void testMaxOfStringList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add("value3");
      add("value1");
      add("23");
      add("value2");
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals("value3", res);
  }

  @Test
  public void testMaxOfIntegerList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12);
      add(56);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(56, res);
  }

  @Test
  public void testMaxWithVarList() {

    Object res = run("MAX([string1,string2])", ImmutableMap.of("string1","abc","string2","def"));
    assertNotNull(res);
    assertEquals("def", res);
  }

  @Test
  public void testMinWithNullInList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(145);
      add(null);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(145, res);
  }

  @Test
  public void testAllNullList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(null);
      add(null);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    assertNull(res);
  }

  @Test
  public void testMinOfIntegerList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(56);
      add(12);
      add(23);
      add(null);
    }};

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(12, res);
  }


  @Test
  public void testMaxOfLongList() {

    List<Object> inputList = new ArrayList<Object>(){{
      add(12L);
      add(56L);
      add(457L);
    }};

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
    assertNotNull(res);
    assertEquals(457L, res);
  }

  @Test
  public void testMaxOfMixedList() {
    List<Object> inputList = new ArrayList<Object>(){{
      add(12);
      add("string");
      add(457L);
    }};

    ParseException e = assertThrows(ParseException.class, () -> {
      Object res = run("MAX(input_list)", ImmutableMap.of("input_list", inputList));
      assertNull(res);
    });
    assertTrue(e.getMessage().contains("Incomparable objects were submitted to MAX: class java.lang.String is incomparable to class java.lang.Long"));
  }

  @Test
  public void testSetInput() {

    Set<Object> inputSet = new HashSet<Object>(){{
      add(14L);
      add(15.3d);
      add(15);
    }};

    Object res = run("MAX(input_set)", ImmutableMap.of("input_set", inputSet));
    assertNotNull(res);
    assertEquals(15.3d, res);
  }

  @Test
  public void testNonComparableList() {

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

    ParseException e = assertThrows(ParseException.class, () -> {
      Object res = run("MIN(input_list)", ImmutableMap.of("input_list", inputList));
      assertNull(res);
    });
    assertTrue(e.getMessage().contains("Noncomparable object type org.apache.metron.stellar.dsl.functions.OrdinalFunctionsTest$1TestObject submitted to MIN"));
  }

  @Test
  public void testMaxOfStats() {
    Ordinal provider = new Ordinal() {
      @Override
      public double getMin() {
        return 10;
      }

      @Override
      public double getMax() {
        return 100;
      }
    };

    Object res = run("MAX(input_list)", ImmutableMap.of("input_list", provider));
    assertNotNull(res);
    assertEquals(100.0d, res);
  }

  @Test
  public void testMinOfStats() {
    Ordinal provider = new Ordinal() {
      @Override
      public double getMin() {
        return 10;
      }

      @Override
      public double getMax() {
        return 100;
      }
    };

    Object res = run("MIN(input_list)", ImmutableMap.of("input_list", provider));
    assertNotNull(res);
    assertEquals(10.0d, res);
  }

  public Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x), x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }
}
