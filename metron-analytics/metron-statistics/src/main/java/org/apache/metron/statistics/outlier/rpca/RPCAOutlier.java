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
package org.apache.metron.statistics.outlier.rpca;

import com.google.common.collect.Iterables;
import org.apache.metron.stellar.common.utils.ConversionUtils;


public class RPCAOutlier {
  private static final double EPSILON = 1e-12;

  private final double LPENALTY_DEFAULT = 1;
  private final double SPENALTY_DEFAULT = 1.4;

  private Double  lpenalty;
  private Double  spenalty;
  private Boolean isForceDiff = false;
  private int minRecords = 0;

  public RPCAOutlier() {

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
    return outputS[nRows-1][0];
  }


}
