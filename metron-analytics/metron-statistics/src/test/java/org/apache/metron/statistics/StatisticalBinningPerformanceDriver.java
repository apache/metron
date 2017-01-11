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

import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This is a driver to drive evaluation of the performance characteristics of the STATS_BIN stellar function.
 * It gets the distribution of the time it takes to calculate the bin of a million random numbers against the quintile bins
 * of a statistical distribution of 10000 normally distributed reals between [-1000, 1000].
 *
 * On my 4 year old macbook pro, the values came out to be
 *
 * Min/25th/50th/75th/Max Milliseconds: 2687.0 / 2700.5 / 2716.0 / 2733.5 / 3730.0
 */
public class StatisticalBinningPerformanceDriver {
  public static int NUM_DATA_POINTS = 10000;
  public static int NUM_RUNS = 30;
  public static int TRIALS_PER_RUN = 1000000;
  public static List<Number> PERCENTILES = ImmutableList.of(25.0, 50.0, 75.0);

  public static void main(String... argv) {
    DescriptiveStatistics perfStats = new DescriptiveStatistics();
    OnlineStatisticsProvider statsProvider = new OnlineStatisticsProvider();
    List<Double> values = new ArrayList<>();
    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < NUM_DATA_POINTS;++i) {
      //get the data point out of the [0,1] range
      double d = 1000*gaussian.nextNormalizedDouble();
      values.add(d);
      statsProvider.addValue(d);
    }

    for(int perfRun = 0;perfRun < NUM_RUNS;++perfRun) {
      StellarStatisticsFunctions.StatsBin bin = new StellarStatisticsFunctions.StatsBin();
      long start = System.currentTimeMillis();
      Random r = new Random(0);
      for (int i = 0; i < TRIALS_PER_RUN; ++i) {
        //grab a random value and fuzz it a bit so we make sure there's no cheating via caching in t-digest.
        bin.apply(ImmutableList.of(statsProvider, values.get(r.nextInt(values.size())) - 3.5, PERCENTILES));
      }
      perfStats.addValue(System.currentTimeMillis() - start);
    }
    System.out.println( "Min/25th/50th/75th/Max Milliseconds: "
                      + perfStats.getMin()
                      + " / " + perfStats.getPercentile(25)
                      + " / " + perfStats.getPercentile(50)
                      + " / " + perfStats.getPercentile(75)
                      + " / " + perfStats.getMax()
                      );
  }
}
