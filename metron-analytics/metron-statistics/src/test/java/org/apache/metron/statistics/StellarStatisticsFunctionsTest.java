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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.function.Function;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the statistical summary functions of Stellar.
 */
public class StellarStatisticsFunctionsTest {

  private List<Double> values;
  private Map<String, Object> variables;
  private DescriptiveStatistics stats;
  private SummaryStatistics summaryStats;

  public static Collection<Object[]> data() {
    // each test will be run against these values for windowSize
    return Arrays.asList(new Object[][] {{ 0 }, { 100 }});
  }

  private static void tolerantAssertEquals( Function<StatisticsProvider, Number> func
                                          , StatisticsProvider left
                                          , StatisticsProvider right
                                          )

  {
    tolerantAssertEquals(func, left, right, null);
  }

  private static void tolerantAssertEquals( Function<StatisticsProvider, Number> func
                                          , StatisticsProvider left
                                          , StatisticsProvider right
                                          , Double epsilon
                                          )
  {
    try {
      Number leftVal = func.apply(left);
      Number rightVal = func.apply(left);
      if(epsilon != null) {
        assertEquals((double)leftVal, (double)rightVal, epsilon);
      }
      else {
        assertEquals(leftVal, rightVal);
      }
    }
    catch(UnsupportedOperationException uoe) {
      //ignore
    }

  }

  /**
   * Runs a Stellar expression.
   * @param expr The expression to run.
   * @param variables The variables available to the expression.
   */
  private static Object run(String expr, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    Object ret = processor.parse(expr, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
    byte[] raw = SerDeUtils.toBytes(ret);
    Object actual = SerDeUtils.fromBytes(raw, Object.class);
    if(ret instanceof StatisticsProvider) {
      StatisticsProvider left = (StatisticsProvider)ret;
      StatisticsProvider right = (StatisticsProvider)actual;
      //N
      tolerantAssertEquals(prov -> prov.getCount(), left, right);
      //sum
      tolerantAssertEquals(prov -> prov.getSum(), left, right, 1e-3);
      //sum of squares
      tolerantAssertEquals(prov -> prov.getSumSquares(), left, right, 1e-3);
      //sum of squares
      tolerantAssertEquals(prov -> prov.getSumLogs(), left, right, 1e-3);
      //Mean
      tolerantAssertEquals(prov -> prov.getMean(), left, right, 1e-3);
      //Quadratic Mean
      tolerantAssertEquals(prov -> prov.getQuadraticMean(), left, right, 1e-3);
      //SD
      tolerantAssertEquals(prov -> prov.getStandardDeviation(), left, right, 1e-3);
      //Variance
      tolerantAssertEquals(prov -> prov.getVariance(), left, right, 1e-3);
      //Min
      tolerantAssertEquals(prov -> prov.getMin(), left, right, 1e-3);
      //Max
      tolerantAssertEquals(prov -> prov.getMax(), left, right, 1e-3);
      //Kurtosis
      tolerantAssertEquals(prov -> prov.getKurtosis(), left, right, 1e-3);
      //Skewness
      tolerantAssertEquals(prov -> prov.getSkewness(), left, right, 1e-3);
      for (double d = 10.0; d < 100.0; d += 10) {
        final double pctile = d;
        //This is a sketch, so we're a bit more forgiving here in our choice of \epsilon.
        tolerantAssertEquals(prov -> prov.getPercentile(pctile), left, right, 1e-2);

      }
    }
    return ret;
  }

  @BeforeEach
  public void setup() {
    variables = new HashMap<>();

    // test input data
    values = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0);

    // the DescriptiveStatistics is used for validation
    stats = new DescriptiveStatistics(1000);
    values.stream().forEach(val -> stats.addValue(val));

    // the StatisticalSummary is used for validation
    summaryStats = new SummaryStatistics();
    values.stream().forEach(val -> summaryStats.addValue(val));
  }

  private void statsInit(int windowSize) {

    // initialize
    Object result = run("STATS_INIT(" + windowSize + ")", variables);
    assertNotNull(result);
    variables.put("stats", result);

    // add some values
    values.stream().forEach(val -> run(format("STATS_ADD (stats, %f)", val), variables));
  }

  @Test
  public void testOverflow() {
   assertThrows(ParseException.class, () -> run(format("STATS_ADD(STATS_INIT(), %f)", (Double.MAX_VALUE + 1)), new HashMap<>()));
  }

  @Test
  public void ensureDeterminism() {
    for(int i = 0;i < 20;++i) {
      testMergeProviders();
    }
  }

  @Test
  public void testMergeProviders() {
    List<StatisticsProvider> providers = new ArrayList<>();
    /*
    Create 10 providers, each with a sample drawn from a gaussian distribution.
    Update the reference stats from commons math to ensure we are
     */
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(1L));
    SummaryStatistics sStatistics= new SummaryStatistics();
    DescriptiveStatistics dStatistics = new DescriptiveStatistics();
    for(int i = 0;i < 10;++i) {
      List<Double> sample = new ArrayList<>();
      for(int j = 0;j < 100;++j) {
        double s = gaussian.nextNormalizedDouble();
        sample.add(s);
        sStatistics.addValue(s);
        dStatistics.addValue(s);
      }
      StatisticsProvider provider = (StatisticsProvider)run("STATS_ADD(STATS_INIT(), " + Joiner.on(",").join(sample) + ")"
                                                           , new HashMap<>()
                                                           );
      providers.add(provider);
    }

    /*
    Merge the providers and validate
     */
    Map<String, Object> providerVariables = new HashMap<>();
    for(int i = 0;i < providers.size();++i) {
      providerVariables.put("provider_" + i, providers.get(i));
    }
    StatisticsProvider mergedProvider =
            (StatisticsProvider)run("STATS_MERGE([" + Joiner.on(",").join(providerVariables.keySet()) + "])"
                                   , providerVariables
                                   );
    OnlineStatisticsProviderTest.validateStatisticsProvider(mergedProvider, sStatistics , dStatistics);

  }

  @ParameterizedTest
  @MethodSource("data")
  public void testAddAllManyIntegers(int windowSize) {
    statsInit(windowSize);
    Object result = run("STATS_COUNT(stats)", variables);
    double countAtStart = (double) result;

    run("STATS_ADD(stats, [10, 20, 30, 40, 50])", variables);

    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(countAtStart + 5.0, (double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testAddManyIntegers(int windowSize) {
    statsInit(windowSize);
    Object result = run("STATS_COUNT(stats)", variables);
    double countAtStart = (double) result;

    run("STATS_ADD(stats, 10, 20, 30, 40, 50)", variables);

    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(countAtStart + 5.0, (double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testAllManyFloat(int windowSize) {
    statsInit(windowSize);
    Object result = run("STATS_COUNT(stats)", variables);
    double countAtStart = (double) result;

    run("STATS_ADD(stats, [10.0, 20.0, 30.0, 40.0, 50.0, null])", variables);

    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(countAtStart + 5.0, (double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testAddManyFloats(int windowSize) {
    statsInit(windowSize);
    Object result = run("STATS_COUNT(stats)", variables);
    double countAtStart = (double) result;

    run("STATS_ADD(stats, 10.0, 20.0, 30.0, 40.0, 50.0)", variables);

    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(countAtStart + 5.0, (double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testCount(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(stats.getN(), (double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testMean(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_MEAN(stats)", variables);
    assertEquals(stats.getMean(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testGeometricMean(int windowSize) {
    if(windowSize > 0) {
      statsInit(windowSize);
      Object actual = run("STATS_GEOMETRIC_MEAN(stats)", variables);
      assertEquals(stats.getGeometricMean(), (Double) actual, 0.1);
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testMax(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_MAX(stats)", variables);
    assertEquals(stats.getMax(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testMin(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_MIN(stats)", variables);
    assertEquals(stats.getMin(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testSum(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_SUM(stats)", variables);
    assertEquals(stats.getSum(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testStandardDeviation(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_SD(stats)", variables);
    assertEquals(stats.getStandardDeviation(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testVariance(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_VARIANCE(stats)", variables);
    assertEquals(stats.getVariance(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testPopulationVariance(int windowSize) {
    if(windowSize > 0) {
      statsInit(windowSize);
      Object actual = run("STATS_POPULATION_VARIANCE(stats)", variables);
      assertEquals(stats.getPopulationVariance(), (Double) actual, 0.1);
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testQuadraticMean(int windowSize) {
    if(windowSize > 0) {
      statsInit(windowSize);
      Object actual = run("STATS_QUADRATIC_MEAN(stats)", variables);
      assertEquals(stats.getQuadraticMean(), (Double) actual, 0.1);
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testSumLogsNoWindow() {
    statsInit(0);
    Object actual = run("STATS_SUM_LOGS(stats)", variables);
    assertEquals(summaryStats.getSumOfLogs(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testSumLogsWithWindow() {
    statsInit(100);
    assertThrows(ParseException.class, () -> run("STATS_SUM_LOGS(stats)", variables));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testSumSquares(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_SUM_SQUARES(stats)", variables);
    assertEquals(stats.getSumsq(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testKurtosis(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_KURTOSIS(stats)", variables);
    assertEquals(stats.getKurtosis(), (Double) actual, 0.1);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testSkewness(int windowSize) {
    statsInit(windowSize);
    Object actual = run("STATS_SKEWNESS(stats)", variables);
    assertEquals(stats.getSkewness(), (Double) actual, 0.1);
  }

  /**
   * This test is testing that the STATS_BIN function operates correctly by taking a sorted list of 
   * numbers, walking down it and ensuring that the STATS_BIN for each number yields the correct bin. 
   * This is a reasonable test because we are not actually computing the bin so much as recognizing 
   * since the numbers are sorted, the bin will increase at the percentile boundaries, thus we have 
   * the expected bin without recreating the computation in the STATS_BIN function.
   **/
  @ParameterizedTest
  @MethodSource("data")
  public void testStatsBin(int windowSize) {
    statsInit(windowSize);
    statsBinRunner(StellarStatisticsFunctions.StatsBin.BinSplits.QUARTILE.split);
    statsBinRunner(StellarStatisticsFunctions.StatsBin.BinSplits.QUARTILE.split, "'QUARTILE'");
    statsBinRunner(StellarStatisticsFunctions.StatsBin.BinSplits.QUINTILE.split, "'QUINTILE'");
    statsBinRunner(StellarStatisticsFunctions.StatsBin.BinSplits.DECILE.split, "'DECILE'");
    statsBinRunner(ImmutableList.of(25.0, 50.0, 75.0), "[25.0, 50.0, 75.0]");
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testStatsBin_singleValue(int windowSize) {
    StatisticsProvider provider = (StatisticsProvider)run("STATS_INIT(" + windowSize + ")", variables);
    provider.addValue(10);
    variables.put("stats", provider);
    assertEquals(0, run(format("STATS_BIN(stats, %f)", 9.0), variables));
    assertEquals(0, run(format("STATS_BIN(stats, %f)", 10.0), variables));
    assertEquals(3, run(format("STATS_BIN(stats, %f)", 11.0), variables));
  }

  public void statsBinRunner(List<Number> splits) {
    statsBinRunner(splits, null);
  }

  public void statsBinRunner(List<Number> splits, String splitsName) {
    int bin = 0;
    StatisticsProvider provider = (StatisticsProvider)variables.get("stats");
    for(Double d : stats.getSortedValues()) {
      while ( bin < splits.size()  &&  d > provider.getPercentile(splits.get(bin).doubleValue()) ) {
        //increment the bin number until it includes the target value, or we run out of bins
        bin++;
      }

      Object actual = null;
      if(splitsName != null) {
        actual = run(format("STATS_BIN(stats, %f, %s)", d, splitsName), variables);
      }
      else {
        actual = run(format("STATS_BIN(stats, %f)", d), variables);
      }
      assertEquals(bin, actual);
    }
  }

  @Test
  public void testPercentileNoWindow() {
    statsInit(0);
    final double percentile = 0.9;
    Object actual = run(format("STATS_PERCENTILE(stats, %f)", percentile), variables);
    assertEquals(stats.getPercentile(percentile), (Double) actual, 1);
  }

  @Test
  public void testPercentileWithWindow() {
    statsInit(100);
    final double percentile = 0.9;
    Object actual = run(format("STATS_PERCENTILE(stats, %f)", percentile), variables);
    assertEquals(stats.getPercentile(percentile), (Double) actual, 0.1);
  }

  @Test
  public void testWithNull() {
    Object actual = run("STATS_MEAN(null)", variables);
    assertTrue(((Double)actual).isNaN());

    actual = run("STATS_COUNT(null)", variables);
    assertTrue(((Double)actual).isNaN());

    actual = run("STATS_VARIANCE(null)", variables);
    assertTrue(((Double)actual).isNaN());
  }
}
