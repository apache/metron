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

package org.apache.metron.profiler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the ProfilePeriod class.
 */
public class ProfilePeriodTest {

  /**
   * Thu, Aug 25 2016 09:27:10 EST
   * Thu, Aug 25 2016 13:27:10 GMT
   *
   * 238th day of the year
   */
  private long AUG2016 = 1472131630748L;

  /**
   * The number of periods per hour must always ensure that the first period falls on the start of each hour.  This
   * means that the number of periods must be a divisor or multiple of 60.
   */
  @Test(expected = RuntimeException.class)
  public void testInvalidPeriodsPerHour() {
    new ProfilePeriod(AUG2016, 241);
  }

  @Test
  public void test1PeriodPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 1);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(0, period.getPeriod());
  }

  @Test
  public void test2PeriodsPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 2);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(0, period.getPeriod());
  }

  @Test
  public void test3PeriodsPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 3);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(1, period.getPeriod());
  }

  @Test
  public void test4PeriodsPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 4);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(1, period.getPeriod());
  }

  @Test
  public void test60PeriodsPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 60);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(27, period.getPeriod());
  }

  @Test
  public void test240PeriodsPerHour() {
    ProfilePeriod period = new ProfilePeriod(AUG2016, 240);

    assertEquals(2016, period.getYear());
    assertEquals(238, period.getDayOfYear());
    assertEquals(13, period.getHour());
    assertEquals(108, period.getPeriod());
  }


  @Test
  public void testNextWith2PeriodsPerHour() {
    int periodsPerHour = 2;

    ProfilePeriod first = new ProfilePeriod(AUG2016, periodsPerHour);
    assertEquals(2016, first.getYear());
    assertEquals(238, first.getDayOfYear());
    assertEquals(13, first.getHour());
    assertEquals(0, first.getPeriod());

    // find the next period
    ProfilePeriod second = first.next();
    assertEquals(2016, second.getYear());
    assertEquals(238, second.getDayOfYear());
    assertEquals(13, second.getHour());
    assertEquals(1, second.getPeriod());
    assertEquals(periodsPerHour, second.getPeriodsPerHour());

    // find the next period
    ProfilePeriod third = second.next();
    assertEquals(2016, third.getYear());
    assertEquals(238, third.getDayOfYear());
    assertEquals(14, third.getHour());
    assertEquals(0, third.getPeriod());
    assertEquals(periodsPerHour, third.getPeriodsPerHour());

    // find the next period
    ProfilePeriod fourth = third.next();
    assertEquals(2016, fourth.getYear());
    assertEquals(238, fourth.getDayOfYear());
    assertEquals(14, fourth.getHour());
    assertEquals(1, fourth.getPeriod());
    assertEquals(periodsPerHour, fourth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod fifth = fourth.next();
    assertEquals(2016, fifth.getYear());
    assertEquals(238, fifth.getDayOfYear());
    assertEquals(15, fifth.getHour());
    assertEquals(0, fifth.getPeriod());
    assertEquals(periodsPerHour, fifth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod sixth = fifth.next();
    assertEquals(2016, sixth.getYear());
    assertEquals(238, sixth.getDayOfYear());
    assertEquals(15, sixth.getHour());
    assertEquals(1, sixth.getPeriod());
    assertEquals(periodsPerHour, sixth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod seventh = sixth.next();
    assertEquals(2016, seventh.getYear());
    assertEquals(238, seventh.getDayOfYear());
    assertEquals(16, seventh.getHour());
    assertEquals(0, seventh.getPeriod());
    assertEquals(periodsPerHour, seventh.getPeriodsPerHour());
  }

  @Test
  public void testNextWith4PeriodsPerHour() {
    int periodsPerHour = 4;

    ProfilePeriod first = new ProfilePeriod(AUG2016, periodsPerHour);
    assertEquals(2016, first.getYear());
    assertEquals(238, first.getDayOfYear());
    assertEquals(13, first.getHour());
    assertEquals(1, first.getPeriod());

    // find the next period
    ProfilePeriod second = first.next();
    assertEquals(2016, second.getYear());
    assertEquals(238, second.getDayOfYear());
    assertEquals(13, second.getHour());
    assertEquals(2, second.getPeriod());
    assertEquals(periodsPerHour, second.getPeriodsPerHour());

    // find the next period
    ProfilePeriod third = second.next();
    assertEquals(2016, third.getYear());
    assertEquals(238, third.getDayOfYear());
    assertEquals(13, third.getHour());
    assertEquals(3, third.getPeriod());
    assertEquals(periodsPerHour, third.getPeriodsPerHour());

    // find the next period
    ProfilePeriod fourth = third.next();
    assertEquals(2016, fourth.getYear());
    assertEquals(238, fourth.getDayOfYear());
    assertEquals(14, fourth.getHour());
    assertEquals(0, fourth.getPeriod());
    assertEquals(periodsPerHour, fourth.getPeriodsPerHour());
  }

  @Test
  public void testNextWith10PeriodsPerHour() {
    int periodsPerHour = 10;

    ProfilePeriod first = new ProfilePeriod(AUG2016, periodsPerHour);
    assertEquals(2016, first.getYear());
    assertEquals(238, first.getDayOfYear());
    assertEquals(13, first.getHour());
    assertEquals(4, first.getPeriod());

    // find the next period
    ProfilePeriod second = first.next();
    assertEquals(2016, second.getYear());
    assertEquals(238, second.getDayOfYear());
    assertEquals(13, second.getHour());
    assertEquals(5, second.getPeriod());
    assertEquals(periodsPerHour, second.getPeriodsPerHour());

    // find the next period
    ProfilePeriod third = second.next();
    assertEquals(2016, third.getYear());
    assertEquals(238, third.getDayOfYear());
    assertEquals(13, third.getHour());
    assertEquals(6, third.getPeriod());
    assertEquals(periodsPerHour, third.getPeriodsPerHour());

    // find the next period
    ProfilePeriod fourth = third.next();
    assertEquals(2016, fourth.getYear());
    assertEquals(238, fourth.getDayOfYear());
    assertEquals(13, fourth.getHour());
    assertEquals(7, fourth.getPeriod());
    assertEquals(periodsPerHour, fourth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod fifth = fourth.next();
    assertEquals(2016, fifth.getYear());
    assertEquals(238, fifth.getDayOfYear());
    assertEquals(13, fifth.getHour());
    assertEquals(8, fifth.getPeriod());
    assertEquals(periodsPerHour, fifth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod sixth = fifth.next();
    assertEquals(2016, sixth.getYear());
    assertEquals(238, sixth.getDayOfYear());
    assertEquals(13, sixth.getHour());
    assertEquals(9, sixth.getPeriod());
    assertEquals(periodsPerHour, sixth.getPeriodsPerHour());

    // find the next period
    ProfilePeriod seventh = sixth.next();
    assertEquals(2016, seventh.getYear());
    assertEquals(238, seventh.getDayOfYear());
    assertEquals(14, seventh.getHour());
    assertEquals(0, seventh.getPeriod());
    assertEquals(periodsPerHour, seventh.getPeriodsPerHour());
  }

  @Test
  public void testNextWith240PeriodsPerHour() {
    final int periodsPerHour = 240;

    ProfilePeriod p = new ProfilePeriod(AUG2016, periodsPerHour);
    assertEquals(2016, p.getYear());
    assertEquals(238, p.getDayOfYear());
    assertEquals(13, p.getHour());
    assertEquals(108, p.getPeriod());

    int lastPeriod = p.getPeriod();
    for(int i=0; i<(periodsPerHour - 108); i++) {
      p = p.next();

      // validate the next period
      assertEquals(2016, p.getYear());
      assertEquals(238, p.getDayOfYear());
      assertEquals(periodsPerHour, p.getPeriodsPerHour());

      int nextPeriod = lastPeriod + 1;
      boolean rolloverToNextHour = nextPeriod >= periodsPerHour;
      if(!rolloverToNextHour) {
        // still within the same hour
        assertEquals(13, p.getHour());
        assertEquals(nextPeriod, p.getPeriod());

      } else {
        // rollover to next hour
        assertEquals(14, p.getHour());
        assertEquals(0, p.getPeriod());
        break;
      }

      lastPeriod = p.getPeriod();
    }
  }

  /**
   * With 2 periods per hour, 'Thu, Aug 25 2016 13:27:10 GMT' falls within the 1st period.
   * Period starts at 'Thu, Aug 25 2016 13:00:00 000 GMT' ~ 1472130000000L
   */
  @Test
  public void testTimeInMillisWith2PeriodsPerHour() {
    final ProfilePeriod period = new ProfilePeriod(AUG2016, 2);
    assertEquals(1472130000000L, period.getTimeInMillis());
  }

  /**
   * With 4 periods per hour, 'Thu, Aug 25 2016 13:27:10 GMT' falls within the 2nd period.
   * Period starts at 'Thu, Aug 25 2016 13:15:00 000 GMT' ~ 1472130900000L
   */
  @Test
  public void testTimeInMillisWith4PeriodsPerHour() {
    final ProfilePeriod period = new ProfilePeriod(AUG2016, 4);
    assertEquals(1472130900000L, period.getTimeInMillis());
  }

  /**
   * With 60 periods per hour, 'Thu, Aug 25 2016 13:27:10 GMT' falls within the 27th period.
   * Period starts at 'Thu, Aug 25 2016 13:27:00 000 GMT' ~ 1472131620000L
   */
  @Test
  public void testTimeInMillisWith60PeriodsPerHour() {
    final ProfilePeriod period = new ProfilePeriod(AUG2016, 60);
    assertEquals(1472131620000L, period.getTimeInMillis());
  }

  /**
   * With 240 periods per hour, 'Thu, Aug 25 2016 13:27:10 GMT' falls within the 108th period.
   * Period starts at 'Thu, Aug 25 2016 13:27:00 000 GMT' ~ 1472131620000L
   */
  @Test
  public void testTimeInMillisWith240PeriodsPerHour() {
    final ProfilePeriod period = new ProfilePeriod(AUG2016, 240);
    assertEquals(1472131620000L, period.getTimeInMillis());
  }


}
