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

import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An inclusion/exclusion specifier which matches against a specific date.
 * The default format is yyyy/MM/dd
 */
public class DateSpecifierPredicate implements Predicate<Long> {
  final static ThreadLocal<SimpleDateFormat> FORMAT = new ThreadLocal<SimpleDateFormat>() {

    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyy/MM/dd");
    }
  };
  Date date;

  /**
   * Create a predicate given a date and (optionally) a format.  If no format is specified, it is assumed to be
   * yyyy/MM/dd
   * @param args
   */
  public DateSpecifierPredicate(List<String> args) {
    if(args.size() == 1) {
      //just the date, use the default.
      try {
        date = FORMAT.get().parse(args.get(0));
      } catch (ParseException e) {
        throw new IllegalStateException("Unable to process " + args.get(0) + " as a date using " + FORMAT.get().toPattern());
      }
    }
    else if(args.size() == 0){
      throw new IllegalStateException("You must specify at least a date and optionally a format");
    }
    else {
      String dateStr = args.get(0);
      String format =  args.get(1);
      try {
        date = new SimpleDateFormat(format).parse(dateStr);
      } catch (ParseException e) {
        throw new IllegalStateException("Unable to process " + dateStr + " as a date using " + format);
      }
    }
  }

  /**
   * Returns true if the timestamp happens on the specified day and false otherwise.
   * @param ts
   * @return boolean returns true if the timestamp happens on the specified day and false otherwise
   */
  @Override
  public boolean test(Long ts) {
    return DateUtils.isSameDay(new Date(ts), date);
  }
}
