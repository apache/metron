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

package org.apache.metron.common.math.stats;

import com.tdunning.math.stats.TDigest;
import org.apache.commons.math3.util.FastMath;

/**
 * A (near) constant memory implementation of a statistics provider.
 * For first order statistics, simple terms are stored and composed
 * to return the statistics results.  This is intended to provide a
 * mergeable implementation for a statistics provider.
 */
public class OnlineStatisticsProvider implements StatisticsProvider {
  /**
   * A sensible default for compression to use in the T-Digest.
   * As per https://github.com/tdunning/t-digest/blob/master/src/main/java/com/tdunning/math/stats/TDigest.java#L86
   * 100 is a sensible default and the number of centroids retained (to construct the sketch)
   * is usually a smallish (usually < 10) multiple of the compression.
   */
  public static final int COMPRESSION = 100;


  /**
   * A distributional sketch that uses a variant of 1-D k-means to construct a tree of ranges
   * that sketches the distribution.  See https://github.com/tdunning/t-digest#t-digest for
   * more detail.
   */
  private TDigest digest;

  private long n = 0;
  private double sum = 0;
  private double sumOfSquares = 0;
  private double sumOfLogs = 0;
  private Double min = null;
  private Double max = null;

  //\mu_1, E[X]
  private double M1 = 0;
  //\mu_2: E[(X - \mu)^2]
  private double M2 = 0;
  //\mu_3: E[(X - \mu)^3]
  private double M3 = 0;
  //\mu_4: E[(X - \mu)^4]
  private double M4 = 0;

  public OnlineStatisticsProvider() {
    digest = TDigest.createAvlTreeDigest(COMPRESSION);
  }

  /**
   * Add a value.
   * NOTE: This does not store the point, but only updates internal state.
   * NOTE: This is NOT threadsafe.
   * @param value
   */
  @Override
  public void addValue(double value) {
    long n1 = n;
    min = min == null?value:Math.min(min, value);
    max = max == null?value:Math.max(max, value);
    sum += value;
    sumOfLogs += Math.log(value);
    sumOfSquares += value*value;
    digest.add(value);
    n++;
    double delta, delta_n, delta_n2, term1;
    //delta between the value and the mean
    delta = value - M1;
    //(x - E[x])/n
    delta_n = delta / n;
    delta_n2 = delta_n * delta_n;
    term1 = delta * delta_n * n1;

    // Adjusting expected value: See Knuth TAOCP vol 2, 3rd edition, page 232
    M1 += delta_n;
    // Adjusting the \mu_i, see http://www.johndcook.com/blog/skewness_kurtosis/
    M4 += term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
    M3 += term1 * delta_n * (n - 2) - 3 * delta_n * M2;
    M2 += term1;
  }

  @Override
  public long getCount() {
    return n;
  }

  @Override
  public double getMin() {
    return min;
  }

  @Override
  public double getMax() {
    return max;
  }

  @Override
  public double getMean() {
    return getSum()/getCount();
  }

  @Override
  public double getSum() {
    return sum;
  }

  @Override
  public double getVariance() {
    return M2/(n - 1.0);
  }

  @Override
  public double getStandardDeviation() {
    return FastMath.sqrt(getVariance());
  }

  @Override
  public double getGeometricMean() {
    throw new UnsupportedOperationException("Unwilling to compute the geometric mean.");
  }

  @Override
  public double getPopulationVariance() {
    throw new UnsupportedOperationException("Unwilling to compute the geometric mean.");
  }

  @Override
  public double getQuadraticMean() {
    return FastMath.sqrt(sumOfSquares/n);
  }

  @Override
  public double getSumLogs() {
    return sumOfLogs;
  }

  @Override
  public double getSumSquares() {
    return sumOfSquares;
  }

  /**
   * Unbiased kurtosis.
   * See http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/descriptive/moment/Kurtosis.html
   * @return
   */
  @Override
  public double getKurtosis() {
    //kurtosis = { [n(n+1) / (n -1)(n - 2)(n-3)] \mu_4 / std^4 } - [3(n-1)^2 / (n-2)(n-3)]
    if(n < 4) {
      return Double.NaN;
    }
    double std = getStandardDeviation();
    double t1 = (1.0*n)*(n+1)/((n-1)*(n-2)*(n-3));
    double t3 = 3.0*((n-1)*(n-1))/((n-2)*(n-3));
    return t1*(M4/FastMath.pow(std, 4))-t3;
  }

  /**
   * Unbiased skewness.
   * See  http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math4/stat/descriptive/moment/Skewness.html
   * @return
   */
  @Override
  public double getSkewness() {
    //  skewness = [n / (n -1) (n - 2)] sum[(x_i - mean)^3] / std^3
    if(n < 3) {
      return Double.NaN;
    }
    double t1 = (1.0*n)/((n - 1)*(n-2));
    double std = getStandardDeviation();
    return t1*M3/FastMath.pow(std, 3);
  }

  /**
   * This returns an approximate percentile based on a t-digest.
   * @param p
   * @return
   */
  @Override
  public double getPercentile(double p) {
    return digest.quantile(p/100.0);
  }

  @Override
  public StatisticsProvider merge(StatisticsProvider provider) {
    OnlineStatisticsProvider combined = new OnlineStatisticsProvider();
    OnlineStatisticsProvider a = this;
    OnlineStatisticsProvider b = (OnlineStatisticsProvider)provider;

    //Combining the simple terms that obviously form a semigroup
    combined.n = a.n + b.n;
    combined.sum = a.sum + b.sum;
    combined.min = Math.min(a.min, b.min);
    combined.max = Math.max(a.max, b.max);
    combined.sumOfSquares = a.sumOfSquares + b.sumOfSquares;
    combined.sumOfLogs = a.sumOfLogs+ b.sumOfLogs;

    // Adjusting the standardized moments, see http://www.johndcook.com/blog/skewness_kurtosis/
    double delta = b.M1 - a.M1;
    double delta2 = delta*delta;
    double delta3 = delta*delta2;
    double delta4 = delta2*delta2;

    combined.M1 = (a.n*a.M1 + b.n*b.M1) / combined.n;

    combined.M2 = a.M2 + b.M2 +
            delta2 * a.n * b.n / combined.n;

    combined.M3 = a.M3 + b.M3 +
            delta3 * a.n * b.n * (a.n - b.n)/(combined.n*combined.n);
    combined.M3 += 3.0*delta * (a.n*b.M2 - b.n*a.M2) / combined.n;

    combined.M4 = a.M4 + b.M4 + delta4*a.n*b.n * (a.n*a.n - a.n*b.n + b.n*b.n) /
            (combined.n*combined.n*combined.n);
    combined.M4 += 6.0*delta2 * (a.n*a.n*b.M2 + b.n*b.n*a.M2)/(combined.n*combined.n) +
            4.0*delta*(a.n*b.M3 - b.n*a.M3) / combined.n;

    //Merging the distributional sketches
    combined.digest.add(a.digest);
    combined.digest.add(b.digest);
    return combined;
  }
}
