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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.io.Serializable;

/**
 * Provides basic summary statistics to Stellar.
 *
 * Used as an adapter to provide a single interface to two underlying Commons
 * Math classes that provide summary statistics.
 */
public class StellarStatistics implements Serializable {

  /**
   * DescriptiveStatistics stores a rolling window of input data elements
   * which is then used to calculate the summary statistic.  There are some
   * summary statistics like kurtosis and percentiles, that can only
   * be calculated with this.
   *
   * This implementation is used if the windowSize > 0.
   */
  private DescriptiveStatistics descStats;

  /**
   * SummaryStatistics can be used for summary statistics that only
   * require a single pass over the input data.  This has the advantage
   * of being less memory intensive, but not all summary statistics are
   * available.
   *
   * This implementation is used if the windowSize == 0.
   */
  private SummaryStatistics summStats;

  /**
   * @param windowSize The number of input data elements to maintain in memory.  If
   *                   windowSize == 0, then no data elements will be maintained in
   *                   memory.
   */
  public StellarStatistics(int windowSize) {

    // only one of the underlying implementation classes will be used at a time
    if(windowSize > 0) {
      descStats = new DescriptiveStatistics(windowSize);
    } else {
      summStats = new SummaryStatistics();
    }
  }

  public void addValue(double value) {
    if(descStats != null) {
      descStats.addValue(value);
    } else {
      summStats.addValue(value);
    }
  }

  public long getCount() {
    if(descStats != null) {
      return descStats.getN();
    } else {
      return summStats.getN();
    }
  }

  public double getMin() {
    if(descStats != null) {
      return descStats.getMin();
    } else {
      return summStats.getMin();
    }
  }

  public double getMax() {
    if(descStats != null) {
      return descStats.getMax();
    } else {
      return summStats.getMax();
    }
  }

  public double getMean() {
    if(descStats != null) {
      return descStats.getMean();
    } else {
      return summStats.getMean();
    }
  }

  public double getSum() {
    if(descStats != null) {
      return descStats.getSum();
    } else {
      return summStats.getSum();
    }
  }

  public double getVariance() {
    if(descStats != null) {
      return descStats.getVariance();
    } else {
      return summStats.getVariance();
    }
  }

  public double getStandardDeviation() {
    if(descStats != null) {
      return descStats.getStandardDeviation();
    } else {
      return summStats.getStandardDeviation();
    }
  }

  public double getGeometricMean() {
    if(descStats != null) {
      return descStats.getGeometricMean();
    } else {
      return summStats.getGeometricMean();
    }
  }

  public double getPopulationVariance() {
    if(descStats != null) {
      return descStats.getPopulationVariance();
    } else {
      return summStats.getPopulationVariance();
    }
  }

  public double getQuadraticMean() {
    if(descStats != null) {
      return descStats.getQuadraticMean();
    } else {
      return summStats.getQuadraticMean();
    }
  }

  public double getSumLogs() {
    if(descStats != null) {
      throw new NotImplementedException("sum logs not available if 'windowSize' > 0");
    } else {
      return summStats.getSumOfLogs();
    }
  }

  public double getSumSquares() {
    if(descStats != null) {
      return descStats.getSumsq();
    } else {
      return summStats.getSumsq();
    }
  }

  public double getKurtosis() {
    if(descStats != null) {
      return descStats.getKurtosis();
    } else {
      throw new NotImplementedException("kurtosis not available if 'windowSize' = 0");
    }
  }

  public double getSkewness() {
    if(descStats != null) {
      return descStats.getSkewness();
    } else {
      throw new NotImplementedException("skewness not available if 'windowSize' = 0");
    }
  }

  public double getPercentile(double p) {
    if(descStats != null) {
      return descStats.getPercentile(p);
    } else {
      throw new NotImplementedException("percentile not available if 'windowSize' = 0");
    }
  }
}