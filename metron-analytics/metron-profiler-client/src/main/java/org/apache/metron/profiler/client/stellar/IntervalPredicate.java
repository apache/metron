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
package org.apache.metron.profiler.client.stellar;

import org.joda.time.Interval;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class IntervalPredicate<T> implements Predicate<T> {
  private final List<Interval> intervals;
  private final Function<T, Long> timestampTransformer;

  public static final class Identity extends IntervalPredicate<Long> {

    public Identity(List<Interval> intervals) {
      super(x -> x, intervals, Long.class);
    }
  }

  public IntervalPredicate(Function<T, Long> timestampTransformer, List<Interval> intervals, Class<T> clazz) {
    this.intervals = intervals;
    this.timestampTransformer = timestampTransformer;
  }

  private boolean containsInclusive(Interval interval, long ts) {
    return interval.contains(ts) || interval.getEndMillis() == ts;
  }


  public static final Comparator<Interval> INTERVAL_COMPARATOR = (o1, o2) -> {
      if(o1.getStartMillis() == o2.getStartMillis() && o1.getEndMillis() == o2.getEndMillis()) {
        return 0;
      }
      else {
        int ret = Long.compare(o1.getStartMillis(), o2.getStartMillis());
        if(ret == 0) {
          return Long.compare(o1.getEndMillis(), o2.getEndMillis());
        }
        else {
          return ret;
        }
      }
  };

  @Override
  public boolean test(T x) {
    long ts = timestampTransformer.apply(x);
    int pos = Collections.binarySearch(intervals, new Interval(ts, ts), INTERVAL_COMPARATOR);
    if(pos < 0) {
      pos = -pos - 1;
    }

    Optional<Interval> right = pos >= 0 && pos < intervals.size()?Optional.of(intervals.get(pos)):Optional.empty();
    Optional<Interval> left = pos - 1 >= 0 && pos - 1 < intervals.size()?Optional.of(intervals.get(pos - 1)):Optional.empty();
    return (right.isPresent()?containsInclusive(right.get(),ts):false) || (left.isPresent()?containsInclusive(left.get(),ts):false);
  }
}
