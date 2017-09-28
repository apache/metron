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

package org.apache.metron.statistics;

import com.google.common.collect.ImmutableList;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.Collections;
import java.util.List;

import static org.apache.metron.stellar.common.utils.ConversionUtils.convert;

/**
 * Provides Stellar functions that can calculate summary statistics on
 * streams of data.
 */
public class StellarStatisticsFunctions {

  /**
   * Initializes the summary statistics.
   *
   * Initialization can occur from either STATS_INIT and STATS_ADD.
   */
  private static StatisticsProvider statsInit(List<Object> args) {
    int windowSize = 0;
    if(args.size() > 0 && args.get(0) instanceof Number) {
      windowSize = convert(args.get(0), Integer.class);
    }
    if(windowSize > 0) {
      return new WindowedStatisticsProvider(windowSize);
    }
    return new OnlineStatisticsProvider();
  }

  @Stellar( namespace="STATS"
          , name="MERGE"
          , description = "Merges statistics objects."
          , params = {
                      "statistics - A list of statistics objects"
                      }
          , returns = "A Stellar statistics object"
          )
  public static class Merge extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      if(args.size() > 0) {
        Object firstArg = args.get(0);
        if(firstArg instanceof List) {
          StatisticsProvider ret = null;
          for(Object sp : (List)firstArg) {
            if(sp instanceof StatisticsProvider) {
              if(ret == null) {
                ret = (StatisticsProvider)sp;
              }
              else {
                ret = ret.merge((StatisticsProvider)sp);
              }
            }
          }
          return ret;
        }
        else {
          return null;
        }
      }
      return null;
    }
  }

  /**
   * Initialize the summary statistics.
   *
   *  STATS_INIT (window_size)
   *
   * window_size The number of input data values to maintain in a rolling window
   *             in memory.  If equal to 0, then no rolling window is maintained.
   *             Using no rolling window is less memory intensive, but cannot
   *             calculate certain statistics like percentiles and kurtosis.
   */
  @Stellar( namespace="STATS"
          , name="INIT"
          , description = "Initializes a statistics object"
          , params = {
                      "window_size - The number of input data values to maintain in a rolling window " +
                      "in memory.  If window_size is equal to 0, then no rolling window is maintained. " +
                      "Using no rolling window is less memory intensive, but cannot " +
                      "calculate certain statistics like percentiles and kurtosis."
                      }
          , returns = "A Stellar statistics object"
          )
  public static class Init extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return statsInit(args);
    }
  }

  /**
   * Add an input value to those that are used to calculate the summary statistics.
   *
   *  STATS_ADD (stats, value [, value2, value3, ...])
   */
  @Stellar(namespace="STATS"
          , name="ADD"
          , description = "Adds one or more input values to those that are used to calculate the summary statistics."
          , params = {
                      "stats - The Stellar statistics object.  If null, then a new one is initialized."
                     , "value+ - One or more items to add. Each item may be a number or a list of numbers. If an item is a list, each number in the list will be added."
                     }
          , returns = "A Stellar statistics object"
          )
  public static class Add extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // initialize a stats object, if one does not already exist
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      if(stats == null) {
        stats = statsInit(Collections.emptyList());
      }

      // add each of the numeric values
      for(int i=1; i<args.size(); i++) {
        Object n = args.get(i);
        if( n != null) {
          if(n instanceof Iterable) {
            for(Object num : (Iterable<Object>)n) {
              if(num != null) {
                Double value = convert(num, Double.class);
                stats.addValue(value);
              }
            }
          }
          else {
            Double value = convert(args.get(i), Double.class);
            stats.addValue(value);
          }
        }
      }

      return stats;
    }
  }

  /**
   * Calculates the mean.
   *
   *  STATS_MEAN (stats)
   */
  @Stellar( namespace="STATS"
          , name="MEAN"
          , description = "Calculates the mean of the accumulated values (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The mean of the values in the window or NaN if the statistics object is null."
          )
  public static class Mean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getMean() : Double.NaN;
    }
  }

  /**
   * Calculates the geometric mean.
   */
  @Stellar( namespace="STATS"
          , name="GEOMETRIC_MEAN"
          , description = "Calculates the geometric mean of the accumulated values (or in the window if a window is used). See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The geometric mean of the values in the window or NaN if the statistics object is null."
          )
  public static class GeometricMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getGeometricMean() : Double.NaN;
    }
  }

  /**
   * Calculates the sum.
   */
  @Stellar(namespace="STATS"
          , name="SUM"
          , description = "Calculates the sum of the accumulated values (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The sum of the values in the window or NaN if the statistics object is null."
          )
  public static class Sum extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getSum() : Double.NaN;
    }
  }

  /**
   * Calculates the max.
   */
  @Stellar(namespace="STATS", name="MAX"
          , description = "Calculates the maximum of the accumulated values (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The maximum of the accumulated values in the window or NaN if the statistics object is null."
          )
  public static class Max extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getMax() : Double.NaN;
    }
  }

  /**
   * Calculates the min.
   */
  @Stellar(namespace="STATS", name="MIN"
          , description = "Calculates the minimum of the accumulated values (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The minimum of the accumulated values in the window or NaN if the statistics object is null."
          )
  public static class Min extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getMin() : Double.NaN;
    }
  }

  /**
   * Calculates the count of elements
   */
  @Stellar(namespace="STATS", name="COUNT"
          , description = "Calculates the count of the values accumulated (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The count of the values in the window or NaN if the statistics object is null.")
  public static class Count extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? convert(stats.getCount(), Double.class) : Double.NaN;
    }
  }

  /**
   * Calculates the population variance.
   */
  @Stellar(namespace="STATS", name="POPULATION_VARIANCE"
          , description = "Calculates the population variance of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The population variance of the values in the window or NaN if the statistics object is null.")
  public static class PopulationVariance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getPopulationVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the variance.
   */
  @Stellar(namespace="STATS", name="VARIANCE"
          , description = "Calculates the variance of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The variance of the values in the window or NaN if the statistics object is null.")
  public static class Variance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the quadratic mean.
   */
  @Stellar(namespace="STATS", name="QUADRATIC_MEAN"
          , description = "Calculates the quadratic mean of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The quadratic mean of the values in the window or NaN if the statistics object is null.")
  public static class QuadraticMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getQuadraticMean() : Double.NaN;
    }
  }

  /**
   * Calculates the standard deviation.
   */
  @Stellar(namespace="STATS", name="SD"
          , description = "Calculates the standard deviation of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The standard deviation of the values in the window or NaN if the statistics object is null.")
  public static class StandardDeviation extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getStandardDeviation() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of logs.
   */
  @Stellar(namespace="STATS", name="SUM_LOGS"
          , description = "Calculates the sum of the (natural) log of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The sum of the (natural) log of the values in the window or NaN if the statistics object is null.")
  public static class SumLogs extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getSumLogs() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of squares.
   */
  @Stellar(namespace="STATS", name="SUM_SQUARES"
          , description = "Calculates the sum of the squares of the accumulated values (or in the window if a window is used)."
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The sum of the squares of the values in the window or NaN if the statistics object is null.")
  public static class SumSquares extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getSumSquares() : Double.NaN;
    }
  }

  /**
   * Calculates the kurtosis.
   */
  @Stellar(namespace="STATS", name="KURTOSIS"
          , description = "Calculates the kurtosis of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The kurtosis of the values in the window or NaN if the statistics object is null.")
  public static class Kurtosis extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getKurtosis() : Double.NaN;
    }
  }

  /**
   * Calculates the skewness.
   */
  @Stellar(namespace="STATS", name="SKEWNESS"
          , description = "Calculates the skewness of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics "
          , params = {
            "stats - The Stellar statistics object"
                     }
          , returns = "The skewness of the values in the window or NaN if the statistics object is null.")
  public static class Skewness extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      return (stats != null) ? stats.getSkewness() : Double.NaN;
    }
  }

  /**
   * Calculates the Pth percentile.
   *
   * STATS_PERCENTILE(stats, 0.90)
   */
  @Stellar(namespace="STATS", name="PERCENTILE"
          , description = "Computes the p'th percentile of the accumulated values (or in the window if a window is used)."
          , params = {
          "stats - The Stellar statistics object"
          ,"p - a double where 0 <= p < 1 representing the percentile"

                     }
          , returns = "The p'th percentile of the data or NaN if the statistics object is null"
          )
  public static class Percentile extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      Double p = convert(args.get(1), Double.class);

      Double result;
      if(stats == null || p == null) {
        result = Double.NaN;
      } else {
        result = stats.getPercentile(p);
      }

      return result;
    }
  }

  /**
   * Calculates the statistical bin that a value falls in.
   */
  @Stellar(namespace = "STATS", name = "BIN"
          , description = "Computes the bin that the value is in based on the statistical distribution."
          , params = {
          "stats - The Stellar statistics object"
          , "value - The value to bin"
          , "bounds? - A list of percentile bin bounds (excluding min and max) or a string representing a known and common set of bins.  " +
          "For convenience, we have provided QUARTILE, QUINTILE, and DECILE which you can pass in as a string arg." +
          " If this argument is omitted, then we assume a Quartile bin split."
                    }
          ,returns = "Which bin N the value falls in such that bound(N-1) < value <= bound(N). " +
          "No min and max bounds are provided, so values smaller than the 0'th bound go in the 0'th bin, " +
          "and values greater than the last bound go in the M'th bin."
  )
  public static class StatsBin extends BaseStellarFunction {
    public enum BinSplits {
      QUARTILE(ImmutableList.of(25.0, 50.0, 75.0)),
      QUINTILE(ImmutableList.of(20.0, 40.0, 60.0, 80.0)),
      DECILE(ImmutableList.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0))
      ;
      public final List<Number> split;
      BinSplits(List<Number> split) {
        this.split = split;
      }

      public static List<Number> getSplit(Object o) {
        if(o instanceof String) {
          return BinSplits.valueOf((String)o).split;
        }
        else if(o instanceof List) {
          return ConversionUtils.convert(o, List.class);
        }
        throw new IllegalStateException("The split you tried to pass is not a valid split: " + o.toString());
      }
    }


    @Override
    public Object apply(List<Object> args) {
      StatisticsProvider stats = convert(args.get(0), StatisticsProvider.class);
      Double value = convert(args.get(1), Double.class);
      final List<Number> bins = args.size() > 2?BinSplits.getSplit(args.get(2)):BinSplits.QUARTILE.split;

      if (stats == null || value == null || bins.size() == 0) {
        return -1;
      }
      return BinFunctions.Bin.getBin(value, bins.size(), bin -> stats.getPercentile(bins.get(bin).doubleValue()));
    }
  }

}
