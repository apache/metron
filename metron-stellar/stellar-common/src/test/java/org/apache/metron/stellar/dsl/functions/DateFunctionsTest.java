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

package org.apache.metron.stellar.dsl.functions;

import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    return processor.parse(expr, new DefaultVariableResolver( x -> variables.get(x), x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
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
    Object result = run("DAY_OF_WEEK(epoch)");
    assertEquals(Calendar.THURSDAY, result);
  }

  /**
   * If no argument, then return the current day of week.
   */
  @Test
  public void testDayOfWeekNow() {
    Object result = run("DAY_OF_WEEK()");
    assertEquals(calendar.get(Calendar.DAY_OF_WEEK), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testDayOfWeekNull() {
    Object result = run("DAY_OF_WEEK(nada)");
  }

  @Test
  public void testWeekOfMonth() {
    Object result = run("WEEK_OF_MONTH(epoch)");
    assertEquals(4, result);
  }

  /**
   * If no argument, then return the current week of month.
   */
  @Test
  public void testWeekOfMonthNow() {
    Object result = run("WEEK_OF_MONTH()");
    assertEquals(calendar.get(Calendar.WEEK_OF_MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testWeekOfMonthNull() {
    Object result = run("WEEK_OF_MONTH(nada)");
  }

  @Test
  public void testMonth() {
    Object result = run("MONTH(epoch)");
    assertEquals(Calendar.AUGUST, result);
  }

  /**
   * If no argument, then return the current month.
   */
  @Test
  public void testMonthNow() {
    Object result = run("MONTH()");
    assertEquals(calendar.get(Calendar.MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testMonthNull() {
    Object result = run("MONTH(nada)");
  }

  @Test
  public void testYear() {
    Object result = run("YEAR(epoch)");
    assertEquals(2016, result);
  }

  /**
   * If no argument, then return the current year.
   */
  @Test
  public void testYearNow() {
    Object result = run("YEAR()");
    assertEquals(calendar.get(Calendar.YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testYearNull() {
    Object result = run("YEAR(nada)");
  }

  @Test
  public void testDayOfMonth() {
    Object result = run("DAY_OF_MONTH(epoch)");
    assertEquals(25, result);
  }

  /**
   * If no argument, then return the current day of month.
   */
  @Test
  public void testDayOfMonthNow() {
    Object result = run("DAY_OF_MONTH()");
    assertEquals(calendar.get(Calendar.DAY_OF_MONTH), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testDayOfMonthNull() {
    Object result = run("DAY_OF_MONTH(nada)");
  }

  @Test
  public void testWeekOfYear() {
    Object result = run("WEEK_OF_YEAR(epoch)");
    calendar.setTimeInMillis(AUG2016);
    assertEquals(calendar.get(Calendar.WEEK_OF_YEAR), result);
  }

  /**
   * If no argument, then return the current week of year.
   */
  @Test
  public void testWeekOfYearNow() {
    Object result = run("WEEK_OF_YEAR()");
    assertEquals(calendar.get(Calendar.WEEK_OF_YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testWeekOfYearNull() {
    Object result = run("WEEK_OF_YEAR(nada)");
  }

  @Test
  public void testDayOfYear() {
    Object result = run("DAY_OF_YEAR(epoch)");
    assertEquals(238, result);
  }

  /**
   * If no argument, then return the current day of year.
   */
  @Test
  public void testDayOfYearNow() {
    Object result = run("DAY_OF_YEAR()");
    assertEquals(calendar.get(Calendar.DAY_OF_YEAR), result);
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testDayOfYearNull() {
    Object result = run("DAY_OF_YEAR(nada)");
  }

  @Test
  public void testDateFormat() {
    Object result = run("DATE_FORMAT('EEE MMM dd yyyy hh:mm:ss zzz', epoch, 'EST')");
    assertEquals("Thu Aug 25 2016 08:27:10 EST", result);
  }

  /**
   * Test that the String returned is formatted as specified.
   * LocalDate.parse will throw if it is not.
   * @throws Exception
   */
  @Test
  public void testDateFormatDefault() throws Exception {
    Object result = run("DATE_FORMAT('EEE MMM dd yyyy hh:mm:ss zzzz')");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy hh:mm:ss zzzz");
    LocalDate.parse(result.toString(), formatter);
  }

  @Test
  public void testDateFormatNow() {
    Object result = run("DATE_FORMAT('EEE MMM dd yyyy hh:mm:ss zzz', 'GMT')");
    assertTrue(result.toString().endsWith("GMT"));
  }

  @Test
  public void testDateFormatDefaultTimezone() {
    Object result = run("DATE_FORMAT('EEE MMM dd yyyy hh:mm:ss zzzz', epoch)");
    assertTrue(result.toString().endsWith(TimeZone.getDefault().getDisplayName(true, 1)));
  }

  /**
   * If refer to variable that does not exist, expect ParseException.
   */
  @Test(expected = ParseException.class)
  public void testDateFormatNull() {
    Object result = run("DATE_FORMAT('EEE MMM dd yyyy hh:mm:ss zzz', nada, 'EST')");
  }

  @Test(expected = ParseException.class)
  public void testDateFormatInvalid() {
    Object result = run("DATE_FORMAT('INVALID DATE FORMAT', epoch, 'EST')");
  }
}
