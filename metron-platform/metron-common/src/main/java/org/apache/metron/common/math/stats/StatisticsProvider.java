package org.apache.metron.common.math.stats;


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
  double getKurtosis();
  double getSkewness();
  double getPercentile(double p);
  StatisticsProvider merge(StatisticsProvider provider);
}
