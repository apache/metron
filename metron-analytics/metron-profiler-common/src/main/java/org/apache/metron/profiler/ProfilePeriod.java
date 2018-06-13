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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.String.format;

/**
 * The Profiler captures a ProfileMeasurement once every ProfilePeriod.  There can be
 * multiple ProfilePeriods every hour.
 */
public class ProfilePeriod implements Serializable {

  /**
   * A monotonically increasing number identifying the period.  The first period is 0
   * and began at the epoch.
   */
  private long period;

  /**
   * The duration of each period in milliseconds.
   */
  private long durationMillis;


  /**
   * @param epochMillis A timestamp contained somewhere within the profile period.
   * @param duration The duration of each profile period.
   * @param units The units of the duration; hours, minutes, etc.
   */
  public ProfilePeriod(long epochMillis, long duration, TimeUnit units) {
    if(duration <= 0) {
      throw new IllegalArgumentException(format(
              "period duration must be greater than 0; got '%d %s'", duration, units));
    }
    this.durationMillis = units.toMillis(duration);
    this.period = epochMillis / durationMillis;
  }

  /**
   * When this period started in milliseconds since the epoch.
   */
  public long getStartTimeMillis() {
    return period * durationMillis;
  }

  /**
   * When this period ended in milliseconds since the epoch.
   */
  public long getEndTimeMillis() {
    return getStartTimeMillis() + getDurationMillis();
  }

  /**
   * Returns the next ProfilePeriod in time.
   */
  public ProfilePeriod next() {
    long nextStart = getStartTimeMillis() + durationMillis;
    return new ProfilePeriod(nextStart, durationMillis, TimeUnit.MILLISECONDS);
  }

  public long getPeriod() {
    return period;
  }


  public long getDurationMillis() {
    return durationMillis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfilePeriod that = (ProfilePeriod) o;
    if (period != that.period) return false;
    return durationMillis == that.durationMillis;
  }

  @Override
  public int hashCode() {
    int result = (int) (period ^ (period >>> 32));
    result = 31 * result + (int) (durationMillis ^ (durationMillis >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "ProfilePeriod{" +
            "period=" + period +
            ", durationMillis=" + durationMillis +
            '}';
  }

  public static <T> List<T> visitPeriods(long startEpochMillis
                                        , long endEpochMillis
                                        , long duration
                                        , TimeUnit units
                                        , Optional<Predicate<ProfilePeriod>> inclusionPredicate
                                        , Function<ProfilePeriod,T> transformation
                                        )
  {
    ProfilePeriod period = new ProfilePeriod(startEpochMillis, duration, units);
    List<T> ret = new ArrayList<>();
    while(period.getStartTimeMillis() <= endEpochMillis) {
      if(!inclusionPredicate.isPresent() || inclusionPredicate.get().test(period)) {
        ret.add(transformation.apply(period));
      }
      period = period.next();
    }
    return ret;
  }
}
