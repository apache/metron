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

package org.apache.metron.common.stellar;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the statistical summary functions of Stellar.
 */
public class StellarStatisticalFunctionsTest {

  private List<Double> values;
  private Map<String, Object> variables;
  private SummaryStatistics stats;

  private static Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(rule, x -> variables.get(x), StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }

  @Before
  public void setup() {
    variables = new HashMap<>();

    // initialize the statistical summary state
    Object result = run("STATS_INIT(0.0)", variables);
    assertNotNull(result);
    variables.put("stats", result);

    // add some values
    values = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0);
    values.stream().forEach(val -> run("STATS_ADD(" + val + ",stats)", variables));

    // add the same values to the StatisticalSummary object that is used for validation
    stats = new SummaryStatistics();
    values.stream().forEach(val -> stats.addValue(val));
  }

  @Test
  public void testCount() throws Exception {
    Object actual = run("STATS_COUNT(stats)", variables);
    assertEquals(stats.getN(), (Long) actual, 0.1);
  }

  @Test
  public void testMean() throws Exception {
    Object actual = run("STATS_MEAN(stats)", variables);
    assertEquals(stats.getMean(), (Double) actual, 0.1);
  }

  @Test
  public void testGeometricMean() throws Exception {
    Object actual = run("STATS_GEOMETRIC_MEAN(stats)", variables);
    assertEquals(stats.getGeometricMean(), (Double) actual, 0.1);
  }

  @Test
  public void testMax() throws Exception {
    Object actual = run("STATS_MAX(stats)", variables);
    assertEquals(stats.getMax(), (Double) actual, 0.1);
  }

  @Test
  public void testMin() throws Exception {
    Object actual = run("STATS_MIN(stats)", variables);
    assertEquals(stats.getMin(), (Double) actual, 0.1);
  }

  @Test
  public void testSum() throws Exception {
    Object actual = run("STATS_SUM(stats)", variables);
    assertEquals(stats.getSum(), (Double) actual, 0.1);
  }

  @Test
  public void testPopulationVariance() throws Exception {
    Object actual = run("STATS_POPULATION_VARIANCE(stats)", variables);
    assertEquals(stats.getPopulationVariance(), (Double) actual, 0.1);
  }

  @Test
  public void testQuadraticMean() throws Exception {
    Object actual = run("STATS_QUADRATIC_MEAN(stats)", variables);
    assertEquals(stats.getQuadraticMean(), (Double) actual, 0.1);
  }

  @Test
  public void testSecondMoment() throws Exception {
    Object actual = run("STATS_SECOND_MOMENT(stats)", variables);
    assertEquals(stats.getSecondMoment(), (Double) actual, 0.1);
  }

  @Test
  public void testSumLogs() throws Exception {
    Object actual = run("STATS_SUM_LOGS(stats)", variables);
    assertEquals(stats.getSumOfLogs(), (Double) actual, 0.1);
  }

  @Test
  public void testStandardDeviation() throws Exception {
    Object actual = run("STATS_SD(stats)", variables);
    assertEquals(stats.getStandardDeviation(), (Double) actual, 0.1);
  }

  @Test
  public void testSumSquares() throws Exception {
    Object actual = run("STATS_SUM_SQUARES(stats)", variables);
    assertEquals(stats.getSumsq(), (Double) actual, 0.1);
  }

  @Test
  public void testVariance() throws Exception {
    Object actual = run("STATS_VARIANCE(stats)", variables);
    assertEquals(stats.getVariance(), (Double) actual, 0.1);
  }
}