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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.metron.common.dsl.BaseStellarFunction;

import java.util.Collections;
import java.util.List;

import static org.apache.metron.common.utils.ConversionUtils.convert;

/**
 * Provides Stellar functions that can calculate summary statistics on
 * streams of data.
 *
 * These functions are limited to those that can be calculated in a
 * single-pass so that the values are not stored in memory.  This leverages
 * the commons-math SummaryStatistics class.
 */
public class SummaryStatisticsFunctions {

  /**
   * Initializes the summary statistics.
   *
   * Initialization can occur from either STATS_INIT and STATS_ADD.
   */
  private static SummaryStatistics statsInit(List<Object> args) {
    return new SummaryStatistics();
  }

  /**
   * Initialize the summary statistics.
   *
   *  STATS_INIT ()
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
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
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
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getMean() : Double.NaN;
    }
  }

  /**
   * Calculates the geometric mean.
   */
  public static class GeometricMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getGeometricMean() : Double.NaN;
    }
  }

  /**
   * Calculates the sum.
   */
  public static class Sum extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getSum() : Double.NaN;
    }
  }

  /**
   * Calculates the max.
   */
  public static class Max extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getMax() : Double.NaN;
    }
  }

  /**
   * Calculates the min.
   */
  public static class Min extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getMin() : Double.NaN;
    }
  }

  /**
   * Calculates the count of elements
   */
  public static class Count extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? convert(stats.getN(), Double.class) : Double.NaN;
    }
  }

  /**
   * Calculates the population variance.
   */
  public static class PopulationVariance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getPopulationVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the variance.
   */
  public static class Variance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getVariance() : Double.NaN;
    }
  }

  /**
   * Calculates the second moment.
   */
  public static class SecondMoment extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getSecondMoment() : Double.NaN;
    }
  }

  /**
   * Calculates the quadratic mean.
   */
  public static class QuadraticMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getQuadraticMean() : Double.NaN;
    }
  }

  /**
   * Calculates the standard deviation.
   */
  public static class StandardDeviation extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getStandardDeviation() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of logs.
   */
  public static class SumLogs extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getSumOfLogs() : Double.NaN;
    }
  }

  /**
   * Calculates the sum of squares.
   */
  public static class SumSquares extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      SummaryStatistics stats = convert(args.get(0), SummaryStatistics.class);
      return (stats != null) ? stats.getSumsq() : Double.NaN;
    }
  }
}