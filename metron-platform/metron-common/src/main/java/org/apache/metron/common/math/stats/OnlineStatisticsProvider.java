package org.apache.metron.common.math.stats;

import com.tdunning.math.stats.TDigest;
import org.apache.commons.math3.util.FastMath;

public class OnlineStatisticsProvider implements StatisticsProvider {
  public static final int COMPRESSION = 150;

  private TDigest digest;

  private long n = 0;
  private double sum = 0;
  private double sumOfSquares = 0;
  private double sumOfLogs = 0;
  private Double min = null;
  private Double max = null;
  //First standardized moment, E[X]
  private double M1 = 0;
  //Second standardized moment: E[(X - \mu)^2]
  private double M2 = 0;
  //Third standardized moment: E[(X - \mu)^3]
  private double M3 = 0;
  //Fourth standardized moment: E[(X - \mu)^4]
  private double M4 = 0;

  public OnlineStatisticsProvider() {
    digest = TDigest.createAvlTreeDigest(COMPRESSION);
  }

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
    // Adjusting the 3rd and 4th standardized moment, see http://www.johndcook.com/blog/skewness_kurtosis/
    M4 += term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
    M3 += term1 * delta_n * (n - 2) - 3 * delta_n * M2;

    // Adjusting second moment: See Knuth TAOCP vol 2, 3rd edition, page 232
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

  @Override
  public double getKurtosis() {
    return (1.0*getCount())*M4 / (M2*M2) - 3.0;
  }

  @Override
  public double getSkewness() {
    return Math.sqrt(getCount()) * M3/ Math.pow(M2, 1.5);
  }

  @Override
  public double getPercentile(double p) {
    return digest.quantile(p/100.0);
  }

  @Override
  public StatisticsProvider merge(StatisticsProvider provider) {
    OnlineStatisticsProvider combined = new OnlineStatisticsProvider();
    OnlineStatisticsProvider a = this;
    OnlineStatisticsProvider b = (OnlineStatisticsProvider)provider;

    combined.n = a.n + b.n;
    combined.sum = a.sum + b.sum;
    combined.min = Math.min(a.min, b.min);
    combined.max = Math.max(a.max, b.max);
    combined.sumOfSquares = a.sumOfSquares + b.sumOfSquares;
    combined.sumOfLogs = a.sumOfLogs+ b.sumOfLogs;
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
    combined.digest.add(a.digest);
    combined.digest.add(b.digest);
    return combined;
  }
}
