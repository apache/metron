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
  private Calendar calendar;

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
    calendar = Calendar.getInstance();
  }

  @Test
  public void testDayOfWeek() {
    Object result = run(format("DAY_OF_WEEK(epoch)", AUG2016));
    assertEquals(Calendar.THURSDAY, result);
  }

  /**
   * If no argument, then return the current day of week.
   */
  @Test
  public void testDayOfWeekNow() {
    Object result = run(format("DAY_OF_WEEK()", AUG2016));
    assertEquals(calendar.get(Calendar.DAY_OF_WEEK), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testDayOfWeekNull() {
    Object result = run(format("DAY_OF_WEEK(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testWeekOfMonth() {
    Object result = run(format("WEEK_OF_MONTH(epoch)", AUG2016));
    assertEquals(4, result);
  }

  /**
   * If no argument, then return the current week of month.
   */
  @Test
  public void testWeekOfMonthNow() {
    Object result = run(format("WEEK_OF_MONTH()", AUG2016));
    assertEquals(calendar.get(Calendar.WEEK_OF_MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testWeekOfMonthNull() {
    Object result = run(format("WEEK_OF_MONTH(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testMonth() {
    Object result = run(format("MONTH(epoch)", AUG2016));
    assertEquals(Calendar.AUGUST, result);
  }

  /**
   * If no argument, then return the current month.
   */
  @Test
  public void testMonthNow() {
    Object result = run(format("MONTH()", AUG2016));
    assertEquals(calendar.get(Calendar.MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testMonthNull() {
    Object result = run(format("MONTH(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testYear() {
    Object result = run(format("YEAR(epoch)", AUG2016));
    assertEquals(2016, result);
  }

  /**
   * If no argument, then return the current year.
   */
  @Test
  public void testYearNow() {
    Object result = run(format("YEAR()", AUG2016));
    assertEquals(calendar.get(Calendar.YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testYearNull() {
    Object result = run(format("YEAR(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testDayOfMonth() {
    Object result = run(format("DAY_OF_MONTH(epoch)", AUG2016));
    assertEquals(25, result);
  }

  /**
   * If no argument, then return the current day of month.
   */
  @Test
  public void testDayOfMonthNow() {
    Object result = run(format("DAY_OF_MONTH()", AUG2016));
    assertEquals(calendar.get(Calendar.DAY_OF_MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testDayOfMonthNull() {
    Object result = run(format("DAY_OF_MONTH(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testWeekOfYear() {
    Object result = run(format("WEEK_OF_YEAR(epoch)", AUG2016));
    assertEquals(35, result);
  }

  /**
   * If no argument, then return the current week of year.
   */
  @Test
  public void testWeekOfYearNow() {
    Object result = run(format("WEEK_OF_YEAR()", AUG2016));
    assertEquals(calendar.get(Calendar.WEEK_OF_YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testWeekOfYearNull() {
    Object result = run(format("WEEK_OF_YEAR(nada)", AUG2016));
    assertEquals(null, result);
  }

  @Test
  public void testDayOfYear() {
    Object result = run(format("DAY_OF_YEAR(epoch)", AUG2016));
    assertEquals(238, result);
  }

  /**
   * If no argument, then return the current day of year.
   */
  @Test
  public void testDayOfYearNow() {
    Object result = run(format("DAY_OF_YEAR()", AUG2016));
    assertEquals(calendar.get(Calendar.DAY_OF_YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect null returned.
   */
  @Test
  public void testDayOfYearNull() {
    Object result = run(format("DAY_OF_YEAR(nada)", AUG2016));
    assertEquals(null, result);
  }
}
