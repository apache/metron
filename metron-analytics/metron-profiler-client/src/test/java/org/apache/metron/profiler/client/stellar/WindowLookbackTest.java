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
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.client.window.WindowProcessor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class WindowLookbackTest {

  static FunctionResolver resolver;
  static Context context;
  @BeforeClass
  public static void setup() {
    resolver = new SimpleFunctionResolver()
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class)
                    .withClass(WindowLookback.class);
    context = new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> new HashMap<>())
                    .build();
  }

  @Test
  public void testSpecifyingConfig() throws Exception {
    //we should be able to specify the config and have it take hold.  If we change the
    //profile duration to 1 minute instead of 15 minutes (the default), then we should see
    //the correct number of profiles.
    long durationMs = 60000;

    Map<String, Object> config = new HashMap<>();
    config.put(ProfilerClientConfig.PROFILER_PERIOD.getKey(), 1);

    State state = test("1 hour", new Date(), Optional.of(config), Assertions.NOT_EMPTY, Assertions.CONTIGUOUS);
    Assert.assertEquals(TimeUnit.HOURS.toMillis(1) / durationMs, state.periods.size());
  }

  @Test
  public void testSpecifyingOnlySelector() {
    String stellarStatement = "PROFILE_WINDOW('1 hour')";
    Map<String, Object> variables = new HashMap<>();
    StellarProcessor stellar = new StellarProcessor();
    List<ProfilePeriod> periods = (List<ProfilePeriod>)stellar.parse( stellarStatement
                                                                    , new DefaultVariableResolver(k -> variables.get(k),k -> variables.containsKey(k))
                                                                    , resolver
                                                                    , context
                                                                    );
    Assert.assertEquals(TimeUnit.HOURS.toMillis(1) / getDurationMs(), periods.size());
  }

  @Test
  public void testDenseLookback() throws Exception {
    State state = test("1 hour", Assertions.NOT_EMPTY, Assertions.CONTIGUOUS);
    Assert.assertEquals(TimeUnit.HOURS.toMillis(1) / getDurationMs(), state.periods.size());
  }

  @Test
  public void testShiftedDenseLookback() throws Exception {
    State state = test("from 2 hours ago to 30 minutes ago", Assertions.NOT_EMPTY
                                                           , Assertions.CONTIGUOUS
                                                           , Assertions.INTERVALS_CONTAIN_ALL_PERIODS
                                                           );
    Assert.assertEquals(TimeUnit.MINUTES.toMillis(90) / getDurationMs(), state.periods.size());
  }

  @Test
  public void testShiftedSparseLookback() throws Exception {
    State state = test("30 minute window every 1 hour from 2 hours ago to 30 minutes ago", Assertions.NOT_EMPTY
                                                                                         , Assertions.DISCONTIGUOUS
                                                                                         , Assertions.INTERVALS_CONTAIN_ALL_PERIODS
                                                                                         );
    Assert.assertEquals(TimeUnit.MINUTES.toMillis(60) / getDurationMs(), state.periods.size());
  }

  @Test
  public void testEmptyDueToExclusions() throws Exception {
    test("30 minute window every 24 hours from 7 days ago including saturdays excluding weekends", Assertions.EMPTY);
  }

  @Test(expected= ParseException.class)
  public void testErrorInSelector() throws Exception {
    test("30 minute idow every 24 hours from 7 days ago including saturdays excluding weekends", Assertions.EMPTY);
  }

  long getDurationMs() {
    int duration = ProfilerClientConfig.PROFILER_PERIOD.getDefault(Integer.class);
    TimeUnit unit = TimeUnit.valueOf(ProfilerClientConfig.PROFILER_PERIOD_UNITS.getDefault(String.class));
    return unit.toMillis(duration);
  }

  public State test(String windowSelector, Assertions... assertions) {
    return test(windowSelector, new Date(), Optional.empty(), assertions);
  }

  public State test(String windowSelector, Date now, Optional<Map<String, Object>> config, Assertions... assertions) {

    List<Range<Long>> windowIntervals = WindowProcessor.process(windowSelector).toIntervals(now.getTime());
    String stellarStatement = "PROFILE_WINDOW('" + windowSelector + "', now"
                            + (config.isPresent()?", config":"")
                            + ")";
    Map<String, Object> variables = new HashMap<>();
    variables.put("now", now.getTime());
    if(config.isPresent()) {
      variables.put("config", config.get());
    }
    StellarProcessor stellar = new StellarProcessor();
    List<ProfilePeriod> periods = (List<ProfilePeriod>)stellar.parse( stellarStatement
                                                                    , new DefaultVariableResolver(k -> variables.get(k),k -> variables.containsKey(k))
                                                                    , resolver
                                                                    , context
                                                                    );
    State state = new State(windowIntervals, periods);
    for(Assertions assertion : assertions) {
      Assert.assertTrue(assertion.name(), assertion.test(state));
    }
    return state;
  }

  private enum Assertions implements Predicate<State>{
    EMPTY( state -> state.windowIntervals.isEmpty() && state.periods.isEmpty() ),
    NOT_EMPTY( state -> !state.windowIntervals.isEmpty() && !state.periods.isEmpty()),
    CONTIGUOUS( state -> {
      if(state.periods.size() < 2) {
        return true;
      }
      long duration = state.periods.get(1).getStartTimeMillis() - state.periods.get(0).getStartTimeMillis();
      for(int i = 1;i < state.periods.size();++i) {
        long left = state.periods.get(i - 1).getStartTimeMillis();
        long right = state.periods.get(i).getStartTimeMillis();
        if(right - left != duration) {
          return false;
        }
      }
      return true;
    }),
    DISCONTIGUOUS( state -> !Assertions.CONTIGUOUS.test(state)),
    INTERVALS_CONTAIN_ALL_PERIODS( state -> {
      List<Range<Long>> windowIntervals = state.windowIntervals;
      List<ProfilePeriod> periods = state.periods;

      Set<Range<Long>> foundIntervals = new HashSet<>();
      for(ProfilePeriod period : periods) {
        boolean found = false;
        for(Range<Long> interval : windowIntervals) {
          if(interval.contains(period.getStartTimeMillis())) {
            foundIntervals.add(interval);
            found = true;
          }
        }
        if(!found) {
          return false;
        }
      }
      return foundIntervals.size() == windowIntervals.size();
    })
    ;
    Predicate<State> predicate;
    Assertions(Predicate<State> predicate) {
      this.predicate = predicate;
    }

    @Override
    public boolean test(State s) {
      return predicate.test(s);
    }
  }

  private static class State {
    List<Range<Long>> windowIntervals;
    List<ProfilePeriod> periods;
    public State(List<Range<Long>> windowIntervals, List<ProfilePeriod> periods) {
      this.periods = periods;
      this.windowIntervals = windowIntervals;
    }
  }

}
