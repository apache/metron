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

import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A inclusion/exclusion selector predicate that returns true if a timestamp falls on a holiday and
 * false otherwise.
 */
public class HolidaysPredicate implements Predicate<Long> {
  HolidayManager manager;
  String[] args;

  /**
   * Create a holidays predicate.  The arguments are the hierarchical specifier for the holidays
   * (see https://github.com/svendiedrichsen/jollyday/tree/master/src/main/resources/holidays for the hierarchies for your
   * supported locales).  The first param is the locale.
   * @param args
   */
  public HolidaysPredicate(List<String> args) {
    if(args == null || args.size() == 0) {
      this.manager = HolidayManager.getInstance();
      this.args = new String[]{};
    }
    else {
      String code = args.get(0);
      this.args = args.size() == 1 ? new String[]{} : new String[args.size() - 1];
      Optional<HolidayCalendar> calendar = getCalendar(code);
      if(calendar.isPresent()) {
        this.manager = HolidayManager.getInstance(ManagerParameters.create(calendar.get()));
      }
      else {
        this.manager = HolidayManager.getInstance(ManagerParameters.create(code));
      }
      for (int i = 1; i < args.size(); ++i) {
        this.args[i - 1] = args.get(i);
      }
    }
  }

  private static Optional<HolidayCalendar> getCalendar(String code) {
    for(HolidayCalendar cal : HolidayCalendar.values()) {
      if(cal.getId().equalsIgnoreCase(code) || cal.name().equalsIgnoreCase(code)) {
        return Optional.of(cal);
      }
    }
    return Optional.empty();
  }

  /**
   * True if the timestamp falls on a holiday as specified or false otherwise.
   * @param ts
   * @return
   */
  @Override
  public boolean test(Long ts) {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date(ts));
    return manager.isHoliday(c, args);
  }
}
