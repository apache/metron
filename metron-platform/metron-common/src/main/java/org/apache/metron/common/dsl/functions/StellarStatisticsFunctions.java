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

package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.BaseStellarFunction;

import java.util.Collections;
import java.util.List;

import static org.apache.metron.common.utils.ConversionUtils.convert;

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
  private static StellarStatistics statsInit(List<Object> args) {
    int windowSize = 0;
    if(args.size() > 0 && args.get(0) instanceof Number) {
      windowSize = convert(args.get(0), Integer.class);
    }

    return new StellarStatistics(windowSize);
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
  public static class Add extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // initialize a stats object, if one does not already exist
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      if(stats == null) {
        stats = statsInit(Collections.emptyList());
      }

      // add each of the numeric values
      for(int i=1; i<args.size(); i++) {
        double value = convert(args.get(i), Double.class);
        stats.addValue(value);
      }

      return stats;
    }
  }


  /**
   * Calculates the mean.
   *
   *  STATS_MEAN (stats)
   */
  public static class Mean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getMean() : Double.NaN;
    }
  }

  /**
   * Calculates the geometric mean.
   */
  public static class GeometricMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getGeometricMean() : Double.NaN;
    }
  }

  /**
   * Calculates the sum.
   */
  public static class Sum extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getSum() : Double.NaN;
    }
  }

  /**
   * Calculates the max.
   */
  public static class Max extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getMax() : Double.NaN;
    }
  }

  /**
   * Calculates the min.
   */
  public static class Min extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getMin() : Double.NaN;
    }
  }

  /**
   * Calculates the count of elements
   */
  public static class Count extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? convert(stats.getCount(), Double.class) : Double.NaN;
    }
  }

  /**
   * Calculates the population variance.
   */
  public static class PopulationVariance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getPopulationVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the variance.
   */
  public static class Variance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the quadratic mean.
   */
  public static class QuadraticMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getQuadraticMean() : Double.NaN;
    }
  }

  /**
   * Calculates the standard deviation.
   */
  public static class StandardDeviation extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getStandardDeviation() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of logs.
   */
  public static class SumLogs extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getSumLogs() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of squares.
   */
  public static class SumSquares extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getSumSquares() : Double.NaN;
    }
  }

  /**
   * Calculates the kurtosis.
   */
  public static class Kurtosis extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getKurtosis() : Double.NaN;
    }
  }

  /**
   * Calculates the skewness.
   */
  public static class Skewness extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
      return (stats != null) ? stats.getSkewness() : Double.NaN;
    }
  }

  /**
   * Calculates the Pth percentile.
   *
   * STATS_PERCENTILE(stats, 0.90)
   */
  public static class Percentile extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      StellarStatistics stats = convert(args.get(0), StellarStatistics.class);
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
}