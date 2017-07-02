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
package org.apache.metron.statistics.outlier;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.statistics.OnlineStatisticsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedianAbsoluteDeviationFunctions {
  public static class State {
    OnlineStatisticsProvider tickMedianProvider;
    OnlineStatisticsProvider tickMADProvider;
    OnlineStatisticsProvider windowMedianProvider;
    OnlineStatisticsProvider windowMADProvider;

    public State() {
      tickMedianProvider = new OnlineStatisticsProvider();
      tickMADProvider = new OnlineStatisticsProvider();
      windowMedianProvider = new OnlineStatisticsProvider();
      windowMADProvider = new OnlineStatisticsProvider();
    }

    public State(Optional<List<State>> previousStates, Optional<State> currentState)
    {
      tickMedianProvider = new OnlineStatisticsProvider();
      tickMADProvider = new OnlineStatisticsProvider();
      windowMedianProvider = currentState.isPresent()?currentState.get().tickMedianProvider:new OnlineStatisticsProvider();
      windowMADProvider = currentState.isPresent()?currentState.get().tickMADProvider:new OnlineStatisticsProvider();
      for(State s : previousStates.orElse(new ArrayList<>())) {
        windowMedianProvider = (OnlineStatisticsProvider) windowMedianProvider.merge(s.tickMedianProvider);
        windowMADProvider = (OnlineStatisticsProvider) windowMADProvider.merge(s.tickMADProvider);
      }
    }

    public void add(Double d) {
      if(!Double.isNaN(d)) {
        tickMedianProvider.addValue(d);
        double deviation = Math.abs(d - windowMedianProvider.getPercentile(50));
        windowMedianProvider.addValue(d);
        if(!Double.isNaN(deviation)) {
          windowMADProvider.addValue(deviation);
          tickMADProvider.addValue(deviation);
        }
      }
    }
  }

  @Stellar(namespace="OUTLIER"
          ,name="MAD_STATE_MERGE"
          ,description="Update the statistical state required to compute the Median Absolute Deviation"
          ,params= {
            "[state] - A list of Median Absolute Deviation States to merge.  Generally these are states across time."
           ,"currentState? - The current state (optional)"
          }
          ,returns="The Median Absolute Deviation state."
  )
  public static class StateUpdate implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      State state = null;
      @SuppressWarnings("unchecked")
      List<State> states = (List<State>) args.get(0);
      State currentState = null;
      if(args.size() > 1) {
        currentState = (State) args.get(1);
      }
      state = new State(Optional.ofNullable(states), Optional.ofNullable(currentState));
      return state;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(namespace="OUTLIER"
          ,name="MAD_ADD"
          ,params= {
            "state - The MAD state"
          , "value - The numeric value to add"
                   }
          ,description="Add a piece of data to the state."
          ,returns="The MAD state."
  )
  public static class PointUpdate implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      State state = (State) args.get(0);
      Object o = args.get(1);
      List<Double> data = new ArrayList<>();
      if(o != null) {
        if (o instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> oList = (List<Object>) o;
          for (Object datum : oList) {
            Number n = (Number) datum;
            data.add(n.doubleValue());
          }
        } else {
          Number n = (Number)o;
          data.add(n.doubleValue());
        }
      }
      if(state != null) {
        for(Double d : data) {
          state.add(d);
        }
      }
      return state;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }

  @Stellar(namespace="OUTLIER"
          ,name="MAD_SCORE"
          ,params = {
            "state - The MAD state"
           ,"value - The value to score"
           ,"scale? - Optionally the scale to use when computing the modified z-score.  Default is 0.6745, see the first page of http://web.ipac.caltech.edu/staff/fmasci/home/astro_refs/BetterThanMAD.pdf"
            }
          ,description="Get the modified z-score normalized by the MAD: scale * | x_i - median(X) | / MAD.  See the first page of http://web.ipac.caltech.edu/staff/fmasci/home/astro_refs/BetterThanMAD.pdf"
          ,returns="The modified z-score."
  )
  public static class Score implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      double scale = 0.6745;
      State state = (State) args.get(0);
      Number datum = (Number)args.get(1);
      if(args.size() > 2) {
        Number scaleNum = (Number) args.get(2);
        if(scaleNum != null) {
          scale = scaleNum.doubleValue();
        }
      }
      if(datum == null || state == null) {
        return Double.NaN;
      }
      double deviation = Math.abs(datum.doubleValue() - state.windowMedianProvider.getPercentile(50));
      double medianAbsoluteDeviation = state.windowMADProvider.getPercentile(50);
      double modifiedZScore = scale*deviation/medianAbsoluteDeviation;
      return modifiedZScore;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
