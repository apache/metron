package org.apache.metron.common.math.stats;


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
  double getSkewness();
  double getPercentile(double p);

  /**
   * Merge an existing statistics provider.
   * @param provider The provider to merge with the current object
   * @return A merged statistics provider.
   */
  StatisticsProvider merge(StatisticsProvider provider);
}
