/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.performance.sampler;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SamplerTest {
  private static final int SIMULATION_SIZE = 10000;
  private void testSampler(Sampler sampler, Map<Integer, Double> expectedProbs) {
    Random rng = new Random(0);
    Map<Integer, Double> empiricalProbs = new HashMap<>();
    for(int i = 0;i < SIMULATION_SIZE;++i) {
      int sample = sampler.sample(rng, 10);
      Double cnt = empiricalProbs.get(sample);
      empiricalProbs.put(sample, ((cnt == null)?0:cnt) + 1);
    }
    for(Map.Entry<Integer, Double> kv : empiricalProbs.entrySet()) {
      double empiricalProb = kv.getValue()/SIMULATION_SIZE;
      String msg = expectedProbs.get(kv.getKey()) + " != " + empiricalProb;
      assertEquals(expectedProbs.get(kv.getKey()), empiricalProb, 1e-2, msg);
    }
  }

  @Test
  public void testUnbiasedSampler() {
    Sampler sampler = new UnbiasedSampler();
    testSampler(sampler, new HashMap<Integer, Double>() {{
      for(int i = 0;i < 10;++i) {
        put(i, 0.1);
      }
    }});
  }

  @Test
  public void testBiasedSampler() {
    Sampler sampler = new BiasedSampler(
            new ArrayList<Map.Entry<Integer, Integer>>() {{
              add(new AbstractMap.SimpleEntry<>(30, 80));
              add(new AbstractMap.SimpleEntry<>(70, 20));
            }}
            , 10
            );
    testSampler(sampler, new HashMap<Integer, Double>() {{
      for(int i = 0;i < 3;++i) {
        put(i, 0.8/3);
      }
      for(int i = 3;i < 10;++i) {
        put(i, 0.2/7);
      }
    }});

  }

  /**
   80,20
   */
  @Multiline
  static String paretoConfigImplicit;

  /**
   80,20
   20,80
   */
  @Multiline
  static String paretoConfig;

  @Test
  public void testDistributionRead() throws IOException {
    for(String config : ImmutableList.of(paretoConfig, paretoConfigImplicit)) {
      List<Map.Entry<Integer, Integer>> endpoints = BiasedSampler.readDistribution(new BufferedReader(new StringReader(config)), true);
      assertEquals(2, endpoints.size());
      assertEquals(new AbstractMap.SimpleEntry<>(80,20), endpoints.get(0));
      assertEquals(new AbstractMap.SimpleEntry<>(20,80), endpoints.get(1));
    }
  }

  /**
   80,20
   10,70
   10,10
   */
  @Multiline
  static String longerConfig;
  /**
   80,20
   10,70
   */
  @Multiline
  static String longerConfigImplicit;

  @Test
  public void testDistributionReadLonger() throws IOException {
    for(String config : ImmutableList.of(longerConfig, longerConfigImplicit)) {
      List<Map.Entry<Integer, Integer>> endpoints = BiasedSampler.readDistribution(new BufferedReader(new StringReader(config)), true);
      assertEquals(3, endpoints.size());
      assertEquals(new AbstractMap.SimpleEntry<>(80,20), endpoints.get(0));
      assertEquals(new AbstractMap.SimpleEntry<>(10,70), endpoints.get(1));
      assertEquals(new AbstractMap.SimpleEntry<>(10,10), endpoints.get(2));
    }
  }

  @Test
  public void testDistributionRead_garbage() {
    assertThrows(IllegalArgumentException.class, () -> BiasedSampler.readDistribution(new BufferedReader(new StringReader("blah foo")), true));
  }

  @Test
  public void testDistributionRead_negative() {
    assertThrows(IllegalArgumentException.class, () -> BiasedSampler.readDistribution(new BufferedReader(new StringReader("80,-20")), true));
  }

  @Test
  public void testDistributionRead_over100() {
    assertThrows(IllegalArgumentException.class, () -> BiasedSampler.readDistribution(new BufferedReader(new StringReader("200,20")), true));
  }
}
