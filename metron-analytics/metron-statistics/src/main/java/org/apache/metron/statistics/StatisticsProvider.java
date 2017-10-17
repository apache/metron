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


/**
 * Provides statistical functions.
 */
public interface StatisticsProvider {
  void addValue(double value);
  long getCount();
  double getMin();
  double getMax();
  double getMean();
  double getSum();
  double getVariance();
  double getStandardDeviation();
  double getGeometricMean();
  double getPopulationVariance();
  double getQuadraticMean();
  double getSumLogs();
  double getSumSquares();

  /**
   * Unbiased Kurtosis.
   * See http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/descriptive/moment/Kurtosis.html
   * @return unbiased kurtosis
   */
  double getKurtosis();

  /**
   * Unbiased skewness.
   * See  http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/descriptive/moment/Skewness.html
   * @return unbiased skewness
   */
  double getSkewness();

  double getPercentile(double p);

  /**
   * Merge an existing statistics provider.
   * @param provider The provider to merge with the current object
   * @return A merged statistics provider.
   */
  StatisticsProvider merge(StatisticsProvider provider);
}
