/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.common.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.common.utils.StellarExecutor;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Tests the Stellar time functions.
 */
public class TimeFunctionsTest {

  private StellarExecutor executor;

  @Before
  public void setup() {
    FunctionResolver functionResolver = new SimpleFunctionResolver()
            .withClass(TimeFunctions.Millis.class);

    executor = new StellarExecutor()
            .withFunctionResolver(functionResolver);
  }

  private long toLong(String expression) {
    return executor.execute(expression, Long.class);
  }

  @Test
  public void testMillis() {
    {
      long expected = TimeUnit.MICROSECONDS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'MICROSECONDS')"));
      assertEquals(expected, toLong("MILLIS(15, 'microseconds')"));
    }
    {
      long expected = TimeUnit.SECONDS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'SECONDS')"));
      assertEquals(expected, toLong("MILLIS(15, 'seconds')"));
    }
    {
      long expected = TimeUnit.MINUTES.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'MINUTES')"));
      assertEquals(expected, toLong("MILLIS(15, 'minutes')"));
    }
    {
      long expected = TimeUnit.HOURS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'HOURS')"));
      assertEquals(expected, toLong("MILLIS(15, 'hours')"));
    }
    {
      long expected = TimeUnit.DAYS.toMillis(15);
      assertEquals(expected, toLong("MILLIS(15, 'DAYS')"));
      assertEquals(expected, toLong("MILLIS(15, 'days')"));
    }
  }
}
