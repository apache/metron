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
package org.apache.metron.statistics.outlier;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.common.utils.SerDeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedianAbsoluteDeviationTest {
  public static Object run(String rule, Map<String, Object> variables) {
    Context context = Context.EMPTY_CONTEXT();
    StellarProcessor processor = new StellarProcessor();
    Assert.assertTrue(rule + " not valid.", processor.validate(rule, context));
    return processor.parse(rule, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  private void assertScoreEquals(MedianAbsoluteDeviationFunctions.State currentState, MedianAbsoluteDeviationFunctions.State clonedState, double value) {
     Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", value));
      Double clonedScore = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", clonedState, "value", value));
      Assert.assertEquals(score, clonedScore, 1e-6);
  }

  @Test
  public void testSerialization() {
    MedianAbsoluteDeviationFunctions.State currentState = null;
    List<MedianAbsoluteDeviationFunctions.State> states = new ArrayList<>();
    currentState = (MedianAbsoluteDeviationFunctions.State) run("OUTLIER_MAD_STATE_MERGE(states, NULL)", ImmutableMap.of("states", states));
    for(int i = 0;i < 100;++i) {
      double d = 1.2*i;
      run("OUTLIER_MAD_ADD(currentState, data)", ImmutableMap.of("currentState", currentState, "data", d));
    }

    byte[] stateBytes = SerDeUtils.toBytes(currentState);
    MedianAbsoluteDeviationFunctions.State clonedState = SerDeUtils.fromBytes(stateBytes, MedianAbsoluteDeviationFunctions.State.class);
    assertScoreEquals(currentState, clonedState, 0d);
    assertScoreEquals(currentState, clonedState, 1d);
    assertScoreEquals(currentState, clonedState, 10d);
  }


  @Test
  public void test() {

    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    DescriptiveStatistics stats = new DescriptiveStatistics();
    List<MedianAbsoluteDeviationFunctions.State> states = new ArrayList<>();
    MedianAbsoluteDeviationFunctions.State currentState = null;
    //initialize the state
    currentState = (MedianAbsoluteDeviationFunctions.State) run("OUTLIER_MAD_STATE_MERGE(states, NULL)", ImmutableMap.of("states", states));
    for(int i = 0,j=0;i < 10000;++i,++j) {
      Double d = gaussian.nextNormalizedDouble();
      stats.addValue(d);
      run("OUTLIER_MAD_ADD(currentState, data)", ImmutableMap.of("currentState", currentState, "data", d));
      if(j >= 1000) {
        j = 0;
        List<MedianAbsoluteDeviationFunctions.State> stateWindow = new ArrayList<>();
        for(int stateIndex = Math.max(0, states.size() - 5);stateIndex < states.size();++stateIndex) {
          stateWindow.add(states.get(stateIndex));
        }
        currentState = (MedianAbsoluteDeviationFunctions.State) run("OUTLIER_MAD_STATE_MERGE(states, currentState)"
                , ImmutableMap.of("states", stateWindow, "currentState", currentState));
      }
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMin()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being a minimum.", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMax()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being a maximum", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean() + 4*stats.getStandardDeviation()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being 4 std deviations away from the mean", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean() - 4*stats.getStandardDeviation()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being 4 std deviations away from the mean", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean()));
      Assert.assertFalse("Score: " + score + " is an outlier despite being the mean", score > 3.5);
    }
  }

  @Test
  public void testLongTailed() {

    TDistribution generator = new TDistribution(new MersenneTwister(0L), 100);
    DescriptiveStatistics stats = new DescriptiveStatistics();
    List<MedianAbsoluteDeviationFunctions.State> states = new ArrayList<>();
    MedianAbsoluteDeviationFunctions.State currentState = null;
    //initialize the state
    currentState = (MedianAbsoluteDeviationFunctions.State) run("OUTLIER_MAD_STATE_MERGE(states, NULL)", ImmutableMap.of("states", states));
    for(int i = 0,j=0;i < 10000;++i,++j) {
      Double d = generator.sample();
      stats.addValue(d);
      run("OUTLIER_MAD_ADD(currentState, data)", ImmutableMap.of("currentState", currentState, "data", d));
      if(j >= 1000) {
        j = 0;
        List<MedianAbsoluteDeviationFunctions.State> stateWindow = new ArrayList<>();
        for(int stateIndex = Math.max(0, states.size() - 5);stateIndex < states.size();++stateIndex) {
          stateWindow.add(states.get(stateIndex));
        }
        currentState = (MedianAbsoluteDeviationFunctions.State) run("OUTLIER_MAD_STATE_MERGE(states, currentState)"
                , ImmutableMap.of("states", stateWindow, "currentState", currentState));
      }
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMin()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being a minimum.", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMax()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being a maximum", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean() + 4*stats.getStandardDeviation()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being 4 std deviations away from the mean", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean() - 4*stats.getStandardDeviation()));
      Assert.assertTrue("Score: " + score + " is not an outlier despite being 4 std deviations away from the mean", score > 3.5);
    }
    {
      Double score = (Double) run("OUTLIER_MAD_SCORE(currentState, value)", ImmutableMap.of("currentState", currentState, "value", stats.getMean()));
      Assert.assertFalse("Score: " + score + " is an outlier despite being the mean", score > 3.5);
    }
  }
}
