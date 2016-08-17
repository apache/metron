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

import java.util.List;
import java.util.function.Function;

import static org.apache.metron.common.utils.ConversionUtils.convert;

/**
 * Provides Stellar functions that can calculate summary statistics on
 * streams of data.
 */
public class StellarStatisticsFunctions {

  /**
   * Initialize the summary statistics.
   *
   *  STATS_INIT (window_size)
   *
   * @param window_size The number of input data values to maintain in a rolling window
   *                    in memory.  If equal to 0, then no rolling window is maintained.
   *                    Using no rolling window is less memory intensive, but cannot
   *                    calculate certain statistics like percentiles and kurtosis.
   */
  public static Function<List<Object>, Object> Init = args -> {
    int windowSize = (args.size() > 0 && args.get(0) instanceof Number) ? ((Number) args.get(0)).intValue() : 0;
    return new StellarStatistics(windowSize);
  };

  /**
   * Add an input value to those that are used to calculate the summary statistics.
   *
   *  STATS_ADD (value, stats)
   *
   *  @param value An input value to use when calculating a summary statistic.
   *  @param stats The stats object.
   */
  public static Function<List<Object>, Object> Add = objects -> {
    double value = convert(objects.get(0), Double.class);
    StellarStatistics stats = convert(objects.get(1), StellarStatistics.class);
    stats.addValue(value);
    return null;
  };

  /**
   * Calculates the mean.
   *
   *  STATS_MEAN (stats)
   */
  public static Function<List<Object>, Object> Mean = objects ->
          convert(objects.get(0), StellarStatistics.class).getMean();

  /**
   * Calculates the sum.
   */
  public static Function<List<Object>, Object> Sum = objects ->
          convert(objects.get(0), StellarStatistics.class).getSum();

  /**
   * Calculates the max.
   */
  public static Function<List<Object>, Object> Max = objects ->
          convert(objects.get(0), StellarStatistics.class).getMax();

  /**
   * Calculates the min.
   */
  public static Function<List<Object>, Object> Min = objects ->
          convert(objects.get(0), StellarStatistics.class).getMin();

  /**
   * Calculates the count of elements
   */
  public static Function<List<Object>, Object> Count = objects ->
          convert(objects.get(0), StellarStatistics.class).getCount();

  /**
   * Calculates the variance.
   */
  public static Function<List<Object>, Object> Variance = objects ->
          convert(objects.get(0), StellarStatistics.class).getVariance();

  /**
   * Calculates the standard deviation.
   */
  public static Function<List<Object>, Object> StandardDeviation = objects ->
          convert(objects.get(0), StellarStatistics.class).getStandardDeviation();

  /**
   * Calculates the geometric mean.
   */
  public static Function<List<Object>, Object> GeometricMean = objects ->
          convert(objects.get(0), StellarStatistics.class).getGeometricMean();

  /**
   * Calculates the population variance.
   */
  public static Function<List<Object>, Object> PopulationVariance = objects ->
          convert(objects.get(0), StellarStatistics.class).getPopulationVariance();

  /**
   * Calculates the second moment.
   */
  public static Function<List<Object>, Object> SecondMoment = objects ->
          convert(objects.get(0), StellarStatistics.class).getSecondMoment();

  /**
   * Calculates the quadratic mean.
   */
  public static Function<List<Object>, Object> QuadraticMean = objects ->
          convert(objects.get(0), StellarStatistics.class).getQuadraticMean();

  /**
   * Calculates the sum of logs.
   */
  public static Function<List<Object>, Object> SumLogs = objects ->
          convert(objects.get(0), StellarStatistics.class).getSumLogs();

  /**
   * Calculates the sum of squares.
   */
  public static Function<List<Object>, Object> SumSquares = objects ->
          convert(objects.get(0), StellarStatistics.class).getSumSquares();

  /**
   * Calculates the kurtosis.
   */
  public static Function<List<Object>, Object> Kurtosis = objects ->
          convert(objects.get(0), StellarStatistics.class).getKurtosis();

  /**
   * Calculates the skewness.
   */
  public static Function<List<Object>, Object> Skewness = objects ->
          convert(objects.get(0), StellarStatistics.class).getSkewness();

  /**
   * Calculates the Pth percentile.
   */
  public static Function<List<Object>, Object> Percentile = objects -> {
    double p = convert(objects.get(0), Double.class);
    return convert(objects.get(1), StellarStatistics.class).getPercentile(p);
  };

}
