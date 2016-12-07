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

package org.apache.metron.common.stellar;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.metron.common.dsl.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.common.utils.StellarProcessorUtils.run;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class StellarArithmeticTest {

  @Test
  public void addingLongsShouldYieldLong() throws Exception {
    final long timestamp = 1452013350000L;
    String query = "TO_EPOCH_TIMESTAMP('2016-01-05 17:02:30', 'yyyy-MM-dd HH:mm:ss', 'UTC') + 2";
    Assert.assertEquals(timestamp + 2, run(query, new HashMap<>()));
  }

  @Test
  public void addingIntegersShouldYieldAnInteger() throws Exception {
    String query = "1 + 2";
    Assert.assertEquals(3, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoublesShouldYieldADouble() throws Exception {
    String query = "1.0 + 2.0";
    Assert.assertEquals(3.0, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoubleAndIntegerWhereSubjectIsDoubleShouldYieldADouble() throws Exception {
    String query = "2.1 + 1";
    Assert.assertEquals(3.1, run(query, new HashMap<>()));
  }

  @Test
  public void addingDoubleAndIntegerWhereSubjectIsIntegerShouldYieldADouble() throws Exception {
    String query = "1 + 2.1";
    Assert.assertEquals(3.1, run(query, new HashMap<>()));
  }

  @Test
  public void testArithmetic() {
    {
      String query = "1 + 2";
      Assert.assertEquals(3, ((Number)run(query, new HashMap<>())).doubleValue(), 1e-3);
    }
    {
      String query = "1.2 + 2";
      Assert.assertEquals(3.2, ((Number)run(query, new HashMap<>())).doubleValue(), 1e-3);
    }
    {
      String query = "1.2e-3 + 2";
      Assert.assertEquals(1.2e-3 + 2, ((Number)run(query, new HashMap<>())).doubleValue(), 1e-3);
    }
  }

  @Test
  public void testNumericOperations() {
    {
      String query = "TO_INTEGER(1 + 2*2 + 3 - 4 - 0.5)";
      Assert.assertEquals(3, (Integer) run(query, new HashMap<>()), 1e-6);
    }
    {
      String query = "1 + 2*2 + 3 - 4 - 0.5";
      Assert.assertEquals(3.5, (Double) run(query, new HashMap<>()), 1e-6);
    }
    {
      String query = "2*one*(1 + 2*2 + 3 - 4)";
      Assert.assertEquals(8, run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "2*(1 + 2 + 3 - 4)";
      Assert.assertEquals(4, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 - 4 - 2";
      Assert.assertEquals(0, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2 + 3 + 4";
      Assert.assertEquals(10, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "(one + 2)*3";
      Assert.assertEquals(9, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "TO_INTEGER((one + 2)*3.5)";
      Assert.assertEquals(10, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "1 + 2*3";
      Assert.assertEquals(7, (Integer) run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.000001)), 1e-6);
    }
    {
      String query = "TO_LONG(foo)";
      Assert.assertNull(run(query,ImmutableMap.of("foo","not a number")));
    }
    {
      String query = "TO_LONG(foo)";
      Assert.assertEquals(232321L,run(query,ImmutableMap.of("foo","00232321")));
    }
    {
      String query = "TO_LONG(foo)";
      Assert.assertEquals(Long.MAX_VALUE,run(query,ImmutableMap.of("foo", Long.toString(Long.MAX_VALUE))));
    }
  }

  @Test
  public void verifyExpectedReturnTypes() throws Exception {
    Token<Integer> integer = mock(Token.class);
    when(integer.getValue()).thenReturn(1);

    Token<Long> lng = mock(Token.class);
    when(lng.getValue()).thenReturn(1L);

    Token<Double> dbl = mock(Token.class);
    when(dbl.getValue()).thenReturn(1.0D);

    Token<Float> flt = mock(Token.class);
    when(flt.getValue()).thenReturn(1.0F);

    Map<Pair<String, String>, Class<? extends Number>> expectedReturnTypeMappings =
        new HashMap<Pair<String, String>, Class<? extends Number>>() {{
          put(Pair.of("TO_FLOAT(3.0)", "TO_LONG(1)"), Float.class);
          put(Pair.of("TO_FLOAT(3)", "3.0"), Double.class);
          put(Pair.of("TO_FLOAT(3)", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("TO_FLOAT(3)", "3"), Float.class);

          put(Pair.of("TO_LONG(1)", "TO_LONG(1)"), Long.class);
          put(Pair.of("TO_LONG(1)", "3.0"), Double.class);
          put(Pair.of("TO_LONG(1)", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("TO_LONG(1)", "3"), Long.class);

          put(Pair.of("3.0", "TO_LONG(1)"), Double.class);
          put(Pair.of("3.0", "3.0"), Double.class);
          put(Pair.of("3.0", "TO_FLOAT(3)"), Double.class);
          put(Pair.of("3.0", "3"), Double.class);

          put(Pair.of("3", "TO_LONG(1)"), Long.class);
          put(Pair.of("3", "3.0"), Double.class);
          put(Pair.of("3", "TO_FLOAT(3)"), Float.class);
          put(Pair.of("3", "3"), Integer.class);
        }};

    expectedReturnTypeMappings.forEach( (pair, expectedClass) -> {
      assertTrue(run(pair.getLeft() + " * " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " + " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " - " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
      assertTrue(run(pair.getLeft() + " / " + pair.getRight(), ImmutableMap.of()).getClass() == expectedClass);
    });
  }
}
