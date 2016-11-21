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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Provides basic summary statistics to Stellar.
 *
 * Used as an adapter to provide an interface to the underlying Commons
 * Math class that provide summary statistics for windows of data.
 * NOTE: Windowed statistics providers cannot be merged.
 */
public class WindowedStatisticsProvider implements StatisticsProvider {

  /**
   * DescriptiveStatistics stores a rolling window of input data elements
   * which is then used to calculate the summary statistic.  There are some
   * summary statistics like kurtosis and percentiles, that can only
   * be calculated with this.
   *
   * This implementation is used if the windowSize > 0.
   */
  private DescriptiveStatistics descStats;

  public WindowedStatisticsProvider(int windowSize) {
    descStats = new DescriptiveStatistics(windowSize);
  }

  @Override
  public void addValue(double value) {
    descStats.addValue(value);
  }

  @Override
  public long getCount() {
    return descStats.getN();
  }

  @Override
  public double getMin() {
    return descStats.getMin();
  }

  @Override
  public double getMax() {
    return descStats.getMax();
  }

  @Override
  public double getMean() {
    return descStats.getMean();
  }

  @Override
  public double getSum() {
    return descStats.getSum();
  }

  @Override
  public double getVariance() {
    return descStats.getVariance();
  }

  @Override
  public double getStandardDeviation() {
    return descStats.getStandardDeviation();
  }

  @Override
  public double getGeometricMean() {
    return descStats.getGeometricMean();
  }

  @Override
  public double getPopulationVariance() {
    return descStats.getPopulationVariance();
  }

  @Override
  public double getQuadraticMean() {
    return descStats.getQuadraticMean();
  }

  @Override
  public double getSumLogs() {
    throw new UnsupportedOperationException("sum logs not available if 'windowSize' > 0");
  }

  @Override
  public double getSumSquares() {
    return descStats.getSumsq();
  }

  @Override
  public double getKurtosis() {
    return descStats.getKurtosis();
  }

  @Override
  public double getSkewness() {
    return descStats.getSkewness();
  }

  @Override
  public double getPercentile(double p) {
    return descStats.getPercentile(p);
  }

  @Override
  public StatisticsProvider merge(StatisticsProvider provider) {
    throw new UnsupportedOperationException("Windowed Statistics cannot be merged.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WindowedStatisticsProvider that = (WindowedStatisticsProvider) o;

    return descStats != null ? descStats.equals(that.descStats) : that.descStats == null;

  }

  @Override
  public int hashCode() {
    return descStats != null ? descStats.hashCode() : 0;
  }
}
