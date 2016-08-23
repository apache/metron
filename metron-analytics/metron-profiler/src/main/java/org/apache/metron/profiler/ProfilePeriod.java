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

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The Profiler captures a ProfileMeasurement once every ProfilePeriod.  There can be
 * multiple ProfilePeriods every hour.
 */
public class ProfilePeriod {

  /**
   * The year.
   */
  private int year;

  /**
   * Day of the year; [1, 366]
   */
  private int dayOfYear;

  /**
   * Hour of the day; [0, 23]
   */
  private int hour;

  /**
   * The period within the hour; [0, periodsPerHour)
   */
  private int period;

  /**
   * The number of periods per hour.  This value must be a divisor of 60; 1, 2, 3, 4, 6, 10, etc.
   */
  private int periodsPerHour;

  /**
   * The actual time used to initialize the ProfilePeriod.  This value should not be
   * used for anything other than troubleshooting.
   */
  private long epochMillis;

  /**
   * @param epochMillis A timestamp contained somewhere within the profile period.
   * @param periodsPerHour The number of periods per hour. Must be an divisor or multiple of 60; 1, 2, 3, 4, 6, 240, etc.
   */
  public ProfilePeriod(long epochMillis, int periodsPerHour) {

    // periods per hour must be a divisor or multiple of 60
    if(60 % periodsPerHour != 0 && periodsPerHour % 60 != 0) {
      throw new RuntimeException(format("invalid periodsPerHour: expected=divisor/multiple of 60, actual=%d", periodsPerHour));
    }

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.setTimeInMillis(epochMillis);

    this.periodsPerHour = periodsPerHour;
    this.period = findPeriod(cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), periodsPerHour);
    this.hour = cal.get(Calendar.HOUR_OF_DAY);
    this.dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
    this.year = cal.get(Calendar.YEAR);
    this.epochMillis = epochMillis;
  }

  /**
   * Returns the next ProfilePeriod in time.
   */
  public ProfilePeriod next() {
    long nextMillis = this.getTimeInMillis() + millisPerPeriod(periodsPerHour);
    return new ProfilePeriod(nextMillis, periodsPerHour);
  }

  /**
   * @return The time in milliseconds since the epoch.
   */
  public long getTimeInMillis() {

    int millisPastHour = (int) millisPerPeriod(periodsPerHour) * period;

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.DAY_OF_YEAR, dayOfYear);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, (millisPastHour / 1000) / 60);
    cal.set(Calendar.SECOND, (millisPastHour / 1000) % 60);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTimeInMillis();
  }

  public int getYear() {
    return year;
  }

  public int getDayOfYear() {
    return dayOfYear;
  }

  public int getHour() {
    return hour;
  }

  public int getPeriod() {
    return period;
  }

  public int getPeriodsPerHour() {
    return periodsPerHour;
  }

  /**
   * Determines the period within the hour based on the minutes/seconds on the clock.
   *
   * @param minutes The minute within the hour; 0-59.
   * @param seconds The second within the minute; 0-59.
   * @return The period within the hour.
   */
  private static int findPeriod(int minutes, int seconds, int periodsPerHour) {
    final int secondsInHour = minutes * 60 + seconds;
    return (int) (secondsInHour / secondsPerPeriod(periodsPerHour));
  }

  /**
   * The number of seconds in each period.
   * @param periodsPerHour The number of periods per hour.
   */
  private static double secondsPerPeriod(int periodsPerHour) {
    return millisPerPeriod(periodsPerHour) / 1000L;
  }

  /**
   * The number of milliseconds in each period.
   * @param periodsPerHour The number of periods per hour.
   */
  private static long millisPerPeriod(int periodsPerHour) {
    final long millisPerHour = 60L * 60L * 1000L;
    return millisPerHour / periodsPerHour;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfilePeriod that = (ProfilePeriod) o;

    if (year != that.year) return false;
    if (dayOfYear != that.dayOfYear) return false;
    if (hour != that.hour) return false;
    if (period != that.period) return false;
    return periodsPerHour == that.periodsPerHour;
  }

  @Override
  public int hashCode() {
    int result = year;
    result = 31 * result + dayOfYear;
    result = 31 * result + hour;
    result = 31 * result + period;
    result = 31 * result + periodsPerHour;
    return result;
  }

  @Override
  public String toString() {
    return "ProfilePeriod{" +
            "year=" + year +
            ", dayOfYear=" + dayOfYear +
            ", hour=" + hour +
            ", period=" + period +
            ", periodsPerHour=" + periodsPerHour +
            '}';
  }
}
