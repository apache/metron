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
package org.apache.metron.statistics.outlier.rad;

import com.google.common.collect.Iterables;
import org.apache.metron.stellar.common.utils.ConversionUtils;


/**
 * This is an outlier detector based on Netflix's Surus' implementation of the Robust PCA-based Outlier Detector.
 *
 * See https://medium.com/netflix-techblog/rad-outlier-detection-on-big-data-d6b0494371cc
 * and https://metamarkets.com/2012/algorithmic-trendspotting-the-meaning-of-interesting/ for a high level
 * treatment of this approach.
 *
 * For a more in-depth treatment of RPCA, see Candes, Li, et al at http://statweb.stanford.edu/~candes/papers/RobustPCA.pdf
 *
 * Robust Principal Component Pursuit is a matrix decomposition algorithm that seeks
 * to separate a matrix X into the sum of three parts X = L + S + E. L is a low rank matrix representing
 * a smooth X, S is a sparse matrix containing corrupted data, and E is noise.
 *
 * While computing the low rank matrix L we take an SVD of X and soft threshold the singular values.
 * This approach allows us to dampen all anomalies across the board simultaneously making the method
 * robust to multiple anomalies. Most techniques such as time series regression and moving averages
 * are not robust when there are two or more anomalies present.
 *
 * The thresholding values can be tuned for different applications, however we strongly
 * recommend using the defaults which were proposed by Zhou.
 * For more details on the choice of L.penalty and s.penalty
 * please refer to Zhou's 2010 paper on Stable Principal Component Pursuit (http://arxiv.org/abs/1001.2363)
 */
public class RPCAOutlier {
  private static final double EPSILON = 1e-12;

  private final double LPENALTY_DEFAULT = 1;
  private final double SPENALTY_DEFAULT = 1.4;

  /**
   * A scalar for the amount of thresholding in determining the low rank approximation for X.
   * See https://github.com/Netflix/Surus/blob/master/resources/R/RAD/R/anomaly_detection.R#L13
   */
  private Double  lpenalty;
  /**
   * A scalar for the amount of thresholding in determining the separation between noise and sparse outliers
   * See https://github.com/Netflix/Surus/blob/master/resources/R/RAD/R/anomaly_detection.R#L16
   */
  private Double  spenalty;
  /**
   * Empirical tests show that identifying anomalies is easier if X is stationary.
   * The Augmented Dickey Fuller Test is used to test for stationarity - if X is not stationary
   * then the time series is differenced before calling RPCP. While this test is abstracted away
   * from the user differencing can be forced by setting the forcediff parameter.
   */
  private Boolean isForceDiff = false;
  private int minRecords = 0;

  public RPCAOutlier() {

  }

  public Double getLpenalty() {
    return lpenalty;
  }

  public Double getSpenalty() {
    return spenalty;
  }

  public Boolean getForceDiff() {
    return isForceDiff;
  }

  public int getMinRecords() {
    return minRecords;
  }

  public RPCAOutlier withLPenalty(double lPenalty) {
    this.lpenalty = lPenalty;
    return this;
  }
  public RPCAOutlier withSPenalty(double sPenalty) {
    this.spenalty = sPenalty;
    return this;
  }

  public RPCAOutlier withForceDiff(boolean forceDiff) {
    this.isForceDiff = forceDiff;
    return this;
  }
  public RPCAOutlier withMinRecords(int minRecords) {
    this.minRecords = minRecords;
    return this;
  }

  // Helper Function
  public double[][] vectorToMatrix(double[] x, int rows, int cols) {
    double[][] input2DArray = new double[rows][cols];
    for (int n= 0; n< x.length; n++) {
      int i = n % rows;
      int j = (int) Math.floor(n / rows);
      input2DArray[i][j] = x[n];
    }
    return input2DArray;
  }

  private class AnalyzedData {
    double[] inputData;
    public AnalyzedData(double[] inputData, int numNonZero) {
      this.inputData = numNonZero >= minRecords?inputData:null;
    }
  }

  private AnalyzedData transform(Iterable<? extends Object> dataPoints, Object value ) {
    int size = Iterables.size(dataPoints);
    double[] inputData = new double[size + 1];
    Double val = ConversionUtils.convert(value, Double.class);
    if(val == null) {
        throw new IllegalStateException("Nulls are not accepted as data points.");
      }
    int numNonZero = 0;
    int i = 0;
    for(Object dpo : dataPoints) {
      Double dp = ConversionUtils.convert(dpo, Double.class);
      if(dp == null) {
        throw new IllegalStateException("Nulls are not accepted as data points.");
      }
      inputData[i++] = dp;
      numNonZero += dp > EPSILON ? 1 : 0;
    }
    inputData[i] = val;
    return new AnalyzedData(inputData, numNonZero);
  }


  public double outlierScore(Iterable<? extends Object> dataPoints, Object value) {
    AnalyzedData dps = transform(dataPoints, value);
    double[] inputData = dps.inputData;
    if(inputData == null) {
      return Double.NaN;
    }

    int nCols = 1;
    int nRows = inputData.length;

    /**
     * Empirical tests show that identifying anomalies is easier if X is stationary.
     * The Augmented Dickey Fuller Test is used to test for stationarity - if X is not stationary
     * then the time series is differenced before calling RPCP.
     */
    AugmentedDickeyFuller dickeyFullerTest = new AugmentedDickeyFuller(inputData);
    double[] inputArrayTransformed = inputData;
    if (!this.isForceDiff && dickeyFullerTest.isNeedsDiff()) {
      // Auto Diff
      inputArrayTransformed = dickeyFullerTest.getZeroPaddedDiff();
    } else if (this.isForceDiff) {
      // Force Diff
      inputArrayTransformed = dickeyFullerTest.getZeroPaddedDiff();
    }

    if (this.spenalty == null) {
      this.lpenalty = this.LPENALTY_DEFAULT;
      this.spenalty = this.SPENALTY_DEFAULT/ Math.sqrt(Math.max(nCols, nRows));
    }


    // Calc Mean of the input data
    double mean  = 0;
    for (int n=0; n < inputArrayTransformed.length; n++) {
      mean += inputArrayTransformed[n];
    }
    mean /= inputArrayTransformed.length;

    // Calc standard deviation for the mean adjusted data
    double stdev = 0;
    for (int n=0; n < inputArrayTransformed.length; n++) {
      stdev += Math.pow(inputArrayTransformed[n] - mean,2) ;
    }
    stdev = Math.sqrt(stdev / (inputArrayTransformed.length - 1));

    // Transformation: Zero Mean, Unit Variance
    for (int n=0; n < inputArrayTransformed.length; n++) {
      inputArrayTransformed[n] = (inputArrayTransformed[n]-mean)/stdev;
    }

    // Read Input Data into Array
    double[][] input2DArray = null;
    input2DArray = vectorToMatrix(inputArrayTransformed, nRows, nCols);

    RPCA rSVD = new RPCA(input2DArray, this.lpenalty, this.spenalty);

    double[][] outputS = rSVD.getS().getData();
    /**
     * S is the residual error.  Typically this will be an anomaly when > 0.
     * Intuitively, this means that PCA had trouble recovering the original signal at that point
     * so there's something funny going on and this point is likely an outlier.
     */
    return outputS[nRows-1][0];
  }


}
