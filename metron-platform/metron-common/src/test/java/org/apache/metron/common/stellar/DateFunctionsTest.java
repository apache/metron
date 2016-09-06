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

package org.apache.metron.common.stellar;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.lang.String.format;

/**
 * Tests the DateFunctions class.
 */
public class DateFunctionsTest {

  private Map<String, Object> variables = new HashMap<>();

  /**
   * Runs a Stellar expression.
   * @param expr The expression to run.
   */
  private Object run(String expr) {
    StellarProcessor processor = new StellarProcessor();
    assertTrue(processor.validate(expr));
    return processor.parse(expr, x -> variables.get(x), StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }

  /**
   * Thu Aug 25 2016 09:27:10 EST
   */
  private long AUG2016 = 1472131630748L;

  @Before
  public void setup() {
    variables.put("epoch", AUG2016);
  }

  @Test
  public void testDayOfWeek() {
    Object result = run(format("DAY_OF_WEEK(epoch)", AUG2016));
    assertEquals(Calendar.THURSDAY, result);
  }

  @Test
  public void testWeekOfMonth() {
    Object result = run(format("WEEK_OF_MONTH(epoch)", AUG2016));
    assertEquals(4, result);
  }

  @Test
  public void testMonth() {
    Object result = run(format("MONTH(epoch)", AUG2016));
    assertEquals(Calendar.AUGUST, result);
  }

  @Test
  public void testYear() {
    Object result = run(format("YEAR(epoch)", AUG2016));
    assertEquals(2016, result);
  }

  @Test
  public void testDayOfMonth() {
    Object result = run(format("DAY_OF_MONTH(epoch)", AUG2016));
    assertEquals(25, result);
  }

  @Test
  public void testWeekOfYear() {
    Object result = run(format("WEEK_OF_YEAR(epoch)", AUG2016));
    assertEquals(35, result);
  }

  @Test
  public void testDayOfYear() {
    Object result = run(format("DAY_OF_YEAR(epoch)", AUG2016));
    assertEquals(238, result);
  }
}
