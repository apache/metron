package org.apache.metron.common.math.stats;

import org.apache.commons.math.random.GaussianRandomGenerator;
import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class OnlineStatisticsProviderTest {


  private void validateEquality(Iterable<Double> values) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    SummaryStatistics summaryStats = new SummaryStatistics();
    OnlineStatisticsProvider statsProvider = new OnlineStatisticsProvider();
    List<OnlineStatisticsProvider> providers = new ArrayList<>();
    for(int i = 0;i < 10;++i) {
      providers.add(new OnlineStatisticsProvider());
    }
    int i = 0;
    for(double d : values) {
      i++;
      stats.addValue(d);
      summaryStats.addValue(d);
      providers.get(i % providers.size()).addValue(d);
      statsProvider.addValue(d);
    }
    StatisticsProvider aggregatedProvider = providers.get(0);
    for(int j = 1;j < providers.size();++j) {
      aggregatedProvider = aggregatedProvider.merge(providers.get(j));
    }
    //N
    Assert.assertEquals(statsProvider.getCount(), stats.getN());
    Assert.assertEquals(aggregatedProvider.getCount(), stats.getN());
    //sum
    Assert.assertEquals(statsProvider.getSum(), stats.getSum(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getSum(), stats.getSum(),1e-3);
    //sum of squares
    Assert.assertEquals(statsProvider.getSumSquares(), stats.getSumsq(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getSumSquares(), stats.getSumsq(), 1e-3);
    //sum of squares
    Assert.assertEquals(statsProvider.getSumLogs(), summaryStats.getSumOfLogs(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getSumLogs(), summaryStats.getSumOfLogs(), 1e-3);
    //Mean
    Assert.assertEquals(statsProvider.getMean(), stats.getMean(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getMean(), stats.getMean(), 1e-3);
    //Quadratic Mean
    Assert.assertEquals(statsProvider.getQuadraticMean(), summaryStats.getQuadraticMean(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getQuadraticMean(), summaryStats.getQuadraticMean(), 1e-3);
    //SD
    Assert.assertEquals(statsProvider.getStandardDeviation(), stats.getStandardDeviation(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getStandardDeviation(), stats.getStandardDeviation(), 1e-3);
    //Variance
    Assert.assertEquals(statsProvider.getVariance(), stats.getVariance(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getVariance(), stats.getVariance(), 1e-3);
    //Min
    Assert.assertEquals(statsProvider.getMin(), stats.getMin(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getMin(), stats.getMin(), 1e-3);
    //Max
    Assert.assertEquals(statsProvider.getMax(), stats.getMax(), 1e-3);
    Assert.assertEquals(aggregatedProvider.getMax(), stats.getMax(), 1e-3);

    //Kurtosis
    Assert.assertEquals(stats.getKurtosis(), aggregatedProvider.getKurtosis(), 1e-3);
    Assert.assertEquals(stats.getKurtosis(), statsProvider.getKurtosis(), 1e-3);

    //Skewness
    Assert.assertEquals(stats.getSkewness(), aggregatedProvider.getSkewness(), 1e-3);
    Assert.assertEquals(stats.getSkewness(), statsProvider.getSkewness(), 1e-3);
    for(double d = 10.0;d < 100.0;d+=10) {
      Assert.assertEquals(statsProvider.getPercentile(d), stats.getPercentile(d), 1e-3);
      Assert.assertEquals(aggregatedProvider.getPercentile(d), stats.getPercentile(d), 1e-3);
    }
  }

  @Test
  public void testNormallyDistributedRandomData() {
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {
      double d = gaussian.nextNormalizedDouble();
      values.add(d);
    }
    validateEquality(values);
  }
  @Test
  public void testNormallyDistributedRandomDataShifted() {
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {
      double d = gaussian.nextNormalizedDouble() + 10;
      values.add(d);
    }
    validateEquality(values);
  }

  @Test
  public void testNormallyDistributedRandomDataShiftedBackwards() {
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {
      double d = gaussian.nextNormalizedDouble() - 10;
      values.add(d);
    }
    validateEquality(values);
  }
  @Test
  public void testNormallyDistributedRandomDataSkewed() {
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {
      double d = (gaussian.nextNormalizedDouble()+ 10000) /1000;
      values.add(d);
    }
    validateEquality(values);
  }

  @Test
  public void testNormallyDistributedRandomDataAllNegative() {
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {
      double d = -1*gaussian.nextNormalizedDouble();
      values.add(d);
    }
    validateEquality(values);
  }
  @Test
  public void testUniformlyDistributedRandomData() {
    List<Double> values = new ArrayList<>();
    for(int i = 0;i < 100000;++i) {
      double d = Math.random();
      values.add(d);
    }
    validateEquality(values);
  }
}
