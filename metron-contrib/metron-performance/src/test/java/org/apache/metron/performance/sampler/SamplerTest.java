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

import org.junit.Assert;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
      Assert.assertEquals(msg, expectedProbs.get(kv.getKey()), empiricalProb, 1e-2);
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
}
