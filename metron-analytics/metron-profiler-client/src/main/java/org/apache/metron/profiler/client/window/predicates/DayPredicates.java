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
package org.apache.metron.profiler.client.window.predicates;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The inclusion/exclusion selector predicates.  The enum name is intended to match the inclusion/exclusion selector
 * name in the window selector grammar.
 */
public enum DayPredicates {
  /**
   * True if the day is sunday, false otherwise.
   */
  SUNDAY( x -> dayOfWeekPredicate(1)),
  /**
   * True if the day is monday, false otherwise.
   */
  MONDAY( x -> dayOfWeekPredicate(2)),
  /**
   * True if the day is tuesday, false otherwise.
   */
  TUESDAY( x -> dayOfWeekPredicate(3)),
  /**
   * True if the day is wednesday, false otherwise.
   */
  WEDNESDAY( x -> dayOfWeekPredicate(4)),
  /**
   * True if the day is thursday, false otherwise.
   */
  THURSDAY( x -> dayOfWeekPredicate(5)),
  /**
   * True if the day is friday, false otherwise.
   */
  FRIDAY( x -> dayOfWeekPredicate(6)),
  /**
   * True if the day is saturday, false otherwise.
   */
  SATURDAY( x -> dayOfWeekPredicate(7)),
  /**
   * True if the day is a weekday, false otherwise.
   */
  WEEKDAY( x -> (ts -> {
    int dow = toCalendar(ts).get(Calendar.DAY_OF_WEEK);
    return dow > 1 && dow < 7;
  })),
  /**
   * True if the day is a weekend, false otherwise.
   */
  WEEKEND( x -> (ts -> {
    int dow = toCalendar(ts).get(Calendar.DAY_OF_WEEK);
    return dow == 1 || dow == 7;
  })),
  /**
   * True if the day is a holiday, false otherwise.
   */
  HOLIDAY(x -> new HolidaysPredicate(x)),
  /**
   * True if the day is a specifie ddate, false otherwise.
   */
  DATE( x -> new DateSpecifierPredicate(x))
  ;
  Function<List<String>, Predicate<Long>> predicateCreator;
  DayPredicates(Function<List<String>, Predicate<Long>> predicate) {
    this.predicateCreator = predicate;
  }

  private static Calendar toCalendar(Long ts) {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date(ts));
    return c;
  }

  public static int getDayOfWeek(Long ts) {
    return toCalendar(ts).get(Calendar.DAY_OF_WEEK);
  }

  public static Predicate<Long> dayOfWeekPredicate(int dayOfWeek) {
    return ts -> getDayOfWeek(ts) == dayOfWeek;
  }

  /**
   * Create a Predicate given a set of arguments.
   * @param name
   * @param arg
   * @return Predicate<Long> return a Predicate given a set of arguments
   */
  public static Predicate<Long> create(String name, List<String> arg) {
    return DayPredicates.valueOf(name).predicateCreator.apply(arg);
  }

}
