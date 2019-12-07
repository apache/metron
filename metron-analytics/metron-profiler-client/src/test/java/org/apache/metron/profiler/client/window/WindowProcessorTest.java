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
package org.apache.metron.profiler.client.window;

import org.apache.commons.lang3.Range;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * On any test case where we explicitly include or exclude days of the week, in 24hr periods,
 * we need to understand that on Daylight Savings Time (DST) transition weekends,
 * Sunday is either 23 or 25 hours long.  This leads to surprising correct results,
 * that may fail the assert criteria, if one starts the hour before or after midnight.
 * Thus in such test cases, we force now.setHours(6).
 */
public class WindowProcessorTest {

  @Test
  public void testBaseCase() {
    for (String text : new String[] {
            "1 hour"
            ,"1 hour(s)"
            ,"1 hours"
    }) {
      Window w = WindowProcessor.process(text);
      Date now = new Date();
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(1, intervals.size());
      assertEquals(now.getTime(), (long)intervals.get(0).getMaximum());
      assertEquals(now.getTime() - TimeUnit.HOURS.toMillis(1), (long)intervals.get(0).getMinimum());
    }
  }

  @Test
  public void testDenseWindow() {
    for (String text : new String[] {
            "from 2 hours ago to 30 minutes ago"
            ,"starting from 2 hours until 30 minutes"
            ,"starting from 2 hours ago until 30 minutes ago"
            ,"starting from 30 minutes ago until 2 hours ago"
            ,"from 30 minutes ago to 2 hours ago "
    }) {
      Window w = WindowProcessor.process(text);
    /*
    A dense window starting 2 hour ago and continuing until 30 minutes ago
     */
      Date now = new Date();
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(1, intervals.size());
      assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(2), intervals.get(0).getMinimum());
      assertTimeEquals(now.getTime() - TimeUnit.MINUTES.toMillis(30), intervals.get(0).getMaximum());
    }
  }

  @Test
  public void testSparse() {
    for(String text : new String[] {
      "30 minute window every 1 hour from 2 hours ago to 30 minutes ago",
      "30 minute window every 1 hour starting from 2 hours ago to 30 minutes ago",
      "30 minute window every 1 hour starting from 2 hours ago until 30 minutes ago",
      "30 minute window for every 1 hour starting from 2 hours ago until 30 minutes ago",
    })
    {
      Window w = WindowProcessor.process(text);
    /*
    A window size of 30 minutes
    Starting 2 hour ago and continuing until 30 minutes ago
    window 1: ( now - 2 hour, now - 2 hour + 30 minutes)
    window 2: (now - 1 hour, now - 1 hour + 30 minutes)
     */
      Date now = new Date();
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(2, intervals.size());
      assertEquals(now.getTime() - TimeUnit.HOURS.toMillis(2), intervals.get(0).getMinimum());
      assertEquals(now.getTime() - TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30), intervals.get(0).getMaximum());
      assertEquals(now.getTime() - TimeUnit.HOURS.toMillis(1), intervals.get(1).getMinimum());
      assertEquals(now.getTime() - TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(30), intervals.get(1).getMaximum());
    }
  }


  @Test
  public void testRepeatTilNow() {
    Window w = WindowProcessor.process("30 minute window every 1 hour from 3 hours ago");
    /*
    A window size of 30 minutes
    Starting 3 hours ago and continuing until now
    window 1: ( now - 3 hour, now - 3 hour + 30 minutes)
    window 2: ( now - 2 hour, now - 2 hour + 30 minutes)
    window 3: ( now - 1 hour, now - 1 hour + 30 minutes)
     */
    Date now = new Date();
    List<Range<Long>> intervals = w.toIntervals(now.getTime());
    assertEquals(3, intervals.size());

    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(3), intervals.get(0).getMinimum());
    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(3) + TimeUnit.MINUTES.toMillis(30), intervals.get(0).getMaximum());

    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(2), intervals.get(1).getMinimum());
    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30), intervals.get(1).getMaximum());

    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(1), intervals.get(2).getMinimum());
    assertTimeEquals(now.getTime() - TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(30), intervals.get(2).getMaximum());
  }

  @Test
  public void testRepeatWithInclusions() {
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago including tuesdays");
    /*
    A window size of 30 minutes
    Starting 14 days ago  and continuing until now
    Gotta be 2 tuesdays in 14 days.
     */
      Date now = new Date();
      now.setHours(6); //avoid DST impacts if near Midnight
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(2, intervals.size());
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago including this day of the week");
    /*
    A window size of 30 minutes
    Starting 14 days ago  and continuing until now
    Gotta be 2 days with the same dow in 14 days.
     */
      Date now = new Date();
      now.setHours(6); //avoid DST impacts if near Midnight
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(2, intervals.size());
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago");
    /*
    A window size of 30 minutes
    Starting 14 days ago  and continuing until now
    Gotta be 14 intervals in 14 days.
     */
      Date now = new Date();
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(14, intervals.size());
    }
  }


  @Test
  public void testRepeatWithConflictingExclusionInclusion() {
    Window w = WindowProcessor.process("30 minute window every 24 hours from 7 days ago including saturdays excluding weekends");

    Date now = new Date();
    now.setHours(6); //avoid DST impacts if near Midnight
    List<Range<Long>> intervals = w.toIntervals(now.getTime());
    assertEquals(0, intervals.size());
  }

  @Test
  public void testRepeatWithWeekendExclusion() {
    Window w = WindowProcessor.process("30 minute window every 24 hours from 7 days ago excluding weekends");

    Date now = new Date();
    now.setHours(6); //avoid DST impacts if near Midnight
    List<Range<Long>> intervals = w.toIntervals(now.getTime());
    assertEquals(5, intervals.size());
  }

  @Test
  public void testRepeatWithInclusionExclusion() throws ParseException {
    Window w = WindowProcessor.process("30 minute window every 24 hours from 7 days ago including holidays:us excluding weekends");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    Date now = sdf.parse("2017/12/26 12:00");
    List<Range<Long>> intervals = w.toIntervals(now.getTime());
    assertEquals(1, intervals.size());
  }

  @Test
  public void testManyMoonsAgo() throws ParseException {
    {
      Window w = WindowProcessor.process("1 hour window every 24 hours starting from 56 days ago");

      Date now = new Date();
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(56, intervals.size());
    }
    {
      Window w = WindowProcessor.process("1 hour window every 24 hours starting from 56 days ago including this day of the week");

      Date now = new Date();
      now.setHours(6); //avoid DST impacts if near Midnight
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(8, intervals.size());
    }
  }

  @Test
  public void testRepeatWithWeekdayExclusion() throws ParseException {
    Window w = WindowProcessor.process("30 minute window every 24 hours from 7 days ago excluding weekdays");

    Date now = new Date();
    now.setHours(6); //avoid DST impacts if near Midnight
    List<Range<Long>> intervals = w.toIntervals(now.getTime());
    assertEquals(2, intervals.size());
  }

  @Test
  public void testRepeatWithHolidayExclusion() throws ParseException {
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago excluding holidays:us");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/12/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(13, intervals.size());
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago excluding holidays:us:nyc");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/12/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(13, intervals.size());
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago excluding holidays:us");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/08/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(14, intervals.size());
    }
  }

  @Test
  public void testDateDaySpecifier() throws ParseException {
    for(String text : new String[] {
        "30 minute window every 24 hours from 14 days ago including date:20171225:yyyyMMdd",
        "30 minute window every 24 hours from 14 days ago including date:2017-12-25:yyyy-MM-dd",
        "30 minute window every 24 hours from 14 days ago including date:2017/12/25",
      })
    {
      Window w = WindowProcessor.process(text);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/12/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(1, intervals.size());
      Date includedDate = new Date(intervals.get(0).getMinimum());
      SimpleDateFormat equalityFormat = new SimpleDateFormat("yyyyMMdd");
      assertEquals("20171225", equalityFormat.format(includedDate));
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago excluding date:2017/12/25");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/12/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(13, intervals.size());
    }
    {
      Window w = WindowProcessor.process("30 minute window every 24 hours from 14 days ago including date:2017/12/25, date:2017/12/24");
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
      Date now = sdf.parse("2017/12/26 12:00");
      List<Range<Long>> intervals = w.toIntervals(now.getTime());
      assertEquals(2, intervals.size());
      {
        Date includedDate = new Date(intervals.get(0).getMinimum());
        SimpleDateFormat equalityFormat = new SimpleDateFormat("yyyyMMdd");
        assertEquals("20171224", equalityFormat.format(includedDate));
      }
      {
        Date includedDate = new Date(intervals.get(1).getMinimum());
        SimpleDateFormat equalityFormat = new SimpleDateFormat("yyyyMMdd");
        assertEquals("20171225", equalityFormat.format(includedDate));
      }
    }
  }

  @Test
  public void testWithInvalidDaySpecifier() {
    assertThrows(
        org.apache.metron.stellar.dsl.ParseException.class,
        () ->
            WindowProcessor.process(
                "30 minute window every 24 hours from 14 days ago excluding hoolidays:us"));
  }

  @Test
  public void testWithInvalidTimeUnit() {
    assertThrows(
        org.apache.metron.stellar.dsl.ParseException.class,
        () -> WindowProcessor.process("30 minute window every 24 months from 14 days ago"));
  }

  @Test
  public void testWithInvalidWindowUnit() {
    assertThrows(
        org.apache.metron.stellar.dsl.ParseException.class,
        () -> WindowProcessor.process("30 minuete window every 24 hours from 14 days ago"));
  }

  @Test
  public void testWithInvalidTimeNumber() {
    assertThrows(
        org.apache.metron.stellar.dsl.ParseException.class,
        () -> WindowProcessor.process("30p minute window every 24 hours from 14 days ago"));
  }

  @Test
  public void testInvalidDaySpecifier() {
    assertThrows(
        org.apache.metron.stellar.dsl.ParseException.class,
        () ->
            WindowProcessor.process(
                "30 minute window every 14 hours from 14 days ago including date"));
  }

  private static void assertTimeEquals(long expected, long actual) {
    long diff = expected - actual;
    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
    String message =  expected + " - " + actual + " = " + diffInMinutes + " minutes off.";
    assertEquals(expected, actual, message);
  }
}
