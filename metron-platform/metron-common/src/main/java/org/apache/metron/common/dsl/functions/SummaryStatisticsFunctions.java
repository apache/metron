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
   * Initialize the summary statistics.
   *
   *  STATS_INIT ()
   */
  public static class Init extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return new SummaryStatistics();
    }
  }

  /**
   * Add an input value to those that are used to calculate the summary statistics.
   *
   *  STATS_ADD (value, stats)
   */
  public static class Add extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      double value = convert(args.get(0), Double.class);
      SummaryStatistics stats = convert(args.get(1), SummaryStatistics.class);
      stats.addValue(value);
      return null;
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
      return convert(args.get(0), SummaryStatistics.class).getMean();
    }
  }

  /**
   * Calculates the geometric mean.
   */
  public static class GeometricMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getGeometricMean();
    }
  }

  /**
   * Calculates the sum.
   */
  public static class Sum extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getSum();
    }
  }

  /**
   * Calculates the max.
   */
  public static class Max extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getMax();
    }
  }

  /**
   * Calculates the min.
   */
  public static class Min extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getMin();
    }
  }

  /**
   * Calculates the count of elements
   */
  public static class Count extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getN();
    }
  }

  /**
   * Calculates the population variance.
   */
  public static class PopulationVariance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getPopulationVariance();
    }
  }

  /**
   * Calculates the variance.
   */
  public static class Variance extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getVariance();
    }
  }

  /**
   * Calculates the second moment.
   */
  public static class SecondMoment extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getSecondMoment();
    }
  }

  /**
   * Calculates the quadratic mean.
   */
  public static class QuadraticMean extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getQuadraticMean();
    }
  }

  /**
   * Calculates the standard deviation.
   */
  public static class StandardDeviation extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getStandardDeviation();
    }
  }

  /**
   * Calculates the sum of logs.
   */
  public static class SumLogs extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getSumOfLogs();
    }
  }

  /**
   * Calculates the sum of squares.
   */
  public static class SumSquares extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {
      return convert(args.get(0), SummaryStatistics.class).getSumsq();
    }
  }
}