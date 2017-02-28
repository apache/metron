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

import org.apache.commons.lang3.Range;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A predicate applied to a type T which can be converted into a long which indicates whether it exists
 * within a set of inclusive ranges of longs.  Generally these ranges may be thought of as timestamps.
 * In this interpretation, it will let you quickly indicate whether a given timestamp is within a set of timestamp
 * ranges.
 *
 * @param <T>
 */
public class IntervalPredicate<T> implements Predicate<T> {
  private final List<Range<Long>> intervals;
  private final Function<T, Long> timestampTransformer;

  /**
   * In the situation where we want longs directly.
   */
  public static final class Identity extends IntervalPredicate<Long> {

    public Identity(List<Range<Long>> intervals) {
      super(x -> x, intervals, Long.class);
    }
  }

  /**
   * Construct an interval predicate given a set of intervals and a function to convert T's to timestamps.
   * Please please please understand that intervals MUST be sorted.
   *
   * @param timestampTransformer The function to convert T's to timestamps.
   * @param intervals A sorted list of timestamp intervals.
   * @param clazz
   */
  public IntervalPredicate(Function<T, Long> timestampTransformer, List<Range<Long>> intervals, Class<T> clazz) {
    this.intervals = intervals;
    this.timestampTransformer = timestampTransformer;
  }

  private boolean containsInclusive(Range<Long> interval, long ts) {
    return interval.contains(ts) || interval.getMaximum() == ts;
  }


  /**
   * A helpful interval comparator that looks sorts the intervals according to left-side.
   */
  public static final Comparator<Range<Long>> INTERVAL_COMPARATOR = (o1, o2) -> {
      if(o1.getMinimum() == o2.getMinimum() && o1.getMaximum() == o2.getMaximum()) {
        return 0;
      }
      else {
        int ret = Long.compare(o1.getMinimum(), o2.getMinimum());
        if(ret == 0) {
          return Long.compare(o1.getMaximum(), o2.getMaximum());
        }
        else {
          return ret;
        }
      }
  };

  /**
   * Determine if x is in the set of intervals in O(log*n) time.
   * @param x
   * @return true if in the set of intervals and false otherwise.
   */
  @Override
  public boolean test(T x) {
    long ts = timestampTransformer.apply(x);
    int pos = Collections.binarySearch(intervals, Range.is(ts), INTERVAL_COMPARATOR);
    if(pos < 0) {
      pos = -pos - 1;
    }

    Optional<Range<Long>> right = pos >= 0 && pos < intervals.size()?Optional.of(intervals.get(pos)):Optional.empty();
    Optional<Range<Long>> left = pos - 1 >= 0 && pos - 1 < intervals.size()?Optional.of(intervals.get(pos - 1)):Optional.empty();
    return (right.isPresent()?containsInclusive(right.get(),ts):false) || (left.isPresent()?containsInclusive(left.get(),ts):false);
  }
}
