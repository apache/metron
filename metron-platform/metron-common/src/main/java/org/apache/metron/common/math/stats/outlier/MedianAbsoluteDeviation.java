package org.apache.metron.common.math.stats.outlier;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.math.stats.OnlineStatisticsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedianAbsoluteDeviation {
  public static class State {
    OnlineStatisticsProvider tickMedianProvider;
    OnlineStatisticsProvider tickMADProvider;
    transient OnlineStatisticsProvider windowMedianProvider;
    transient OnlineStatisticsProvider windowMADProvider;

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
      tickMedianProvider.addValue(d);
      double deviation = Math.abs(d - windowMedianProvider.getPercentile(50));
      windowMedianProvider.addValue(d);
      windowMADProvider.addValue(deviation);
      tickMADProvider.addValue(deviation);
    }
  }

  @Stellar(namespace="OUTLIER"
          ,name="MAD_STATE_UPDATE"
          ,description="Update the statistical state required to compute the Median Absolute Deviation"
          ,returns="The state."
  )
  public static class StateUpdate implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      State state = null;
      List<State> states = (List<State>) args.get(0);
      State currentState = (State) args.get(1);
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
          ,description="Add a piece of data to the state."
          ,returns="The state."
  )
  public static class PointUpdate implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      State state = (State) args.get(0);
      Number datum = (Number)args.get(1);
      if(datum != null && state != null) {
        state.add(datum.doubleValue());
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
          ,description="Get the MAD score"
          ,returns="The score."
  )
  public static class Score implements StellarFunction{

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      double zScore = 0.6745;
      State state = (State) args.get(0);
      Number datum = (Number)args.get(1);
      if(args.size() > 2) {
        zScore = ((Number)args.get(2)).doubleValue();
      }
      double deviation = datum.doubleValue() - state.windowMedianProvider.getPercentile(50);
      double medianAbsoluteDeviation = state.windowMADProvider.getPercentile(50);
      double modifiedZScore = zScore*deviation/medianAbsoluteDeviation;
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
