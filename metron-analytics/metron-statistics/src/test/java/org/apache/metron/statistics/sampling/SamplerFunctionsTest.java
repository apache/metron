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
package org.apache.metron.statistics.sampling;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SamplerFunctionsTest {
  static List<Double> sample = new ArrayList<>();
  static List<String> sampleString = new ArrayList<>();
  static List<Sampler> samplers = new ArrayList<>();
  @BeforeAll
  public static void beforeClass() {
    Random rng = new Random(0);
    int sampleSize = 1000000;
    int numSubSamples = 10;
    int subSampleSize = sampleSize/numSubSamples;
    int currSample = -1;
    for(int i = 0,j=0;i < sampleSize;++i,j = (j+1)%subSampleSize) {
      double us= 10*rng.nextDouble();
      sample.add(us);
      sampleString.add(us + "");
      if(j == 0) {
        Sampler s = new UniformSampler(subSampleSize/10);
        samplers.add(s);
        currSample++;
      }
      samplers.get(currSample).add(us);
    }
  }

  @Test
  public void testValidInit_default() {
    String stmt = "SAMPLE_INIT()";
    Sampler s = (Sampler) StellarProcessorUtils.run(stmt, new HashMap<>());
    assertEquals(Sampler.DEFAULT_SIZE, s.getSize());
  }

  @Test
  public void testValidInit_withSize() {
    String stmt = "SAMPLE_INIT(size)";
    Sampler s = (Sampler) StellarProcessorUtils.run(stmt, ImmutableMap.of("size", 10 ));
    assertEquals(10, s.getSize());
  }

  @Test
  public void testInvalidInit(){
    String stmt = "SAMPLE_INIT(size)";
    assertThrows(ParseException.class, () -> StellarProcessorUtils.run(stmt, ImmutableMap.of("size", -10 )));
  }

  @Test
  public void testGet() {
    String stmt = "SAMPLE_GET(SAMPLE_ADD(SAMPLE_INIT(size), values))";
    Iterable<? extends Object> s = (Iterable<? extends Object>) StellarProcessorUtils.run(stmt, ImmutableMap.of("size", 10, "values", sample));
    assertEquals(10, Iterables.size(s));
    for(Object o : s) {
      assertTrue(o instanceof Double);
      assertTrue(sample.contains(o));
    }
  }

  @Test
  public void testAddSingle() {
    String stmt = "SAMPLE_ADD(SAMPLE_INIT(size), value)";
    Sampler s = (Sampler) StellarProcessorUtils.run(stmt, ImmutableMap.of("size", 10, "value", "blah"));
    assertEquals(10, s.getSize());
    assertTrue(Iterables.getFirst(s.get(), null) instanceof String);
  }

  @Test
  public void testAddAll() {
    String stmt = "SAMPLE_ADD(SAMPLE_INIT(size), value)";
    Sampler s = (Sampler) StellarProcessorUtils.run(stmt, ImmutableMap.of("size", 10, "value", sampleString));
    assertEquals(10, s.getSize());
    for(Object o : s.get()) {
      assertTrue(o instanceof String);
      assertTrue(sampleString.contains(o));
    }
  }

  @Test
  public void testMerge() {
    Double sampleMean= null;
    Double mergedSampleMean= null;
    {
      //grab the mean of the sample
      String stmt = "STATS_MEAN(STATS_ADD(STATS_INIT(), SAMPLE_GET(SAMPLE_ADD(SAMPLE_INIT(size), values))))";
      sampleMean = (Double) StellarProcessorUtils.run(stmt, ImmutableMap.of("size", sample.size()/10, "values", sample));
    }
    {
      //grab the mean of the merged set of subsamples of the sample
      String stmt = "STATS_MEAN(STATS_ADD(STATS_INIT(), SAMPLE_GET(SAMPLE_MERGE(samples))))";
      mergedSampleMean = (Double) StellarProcessorUtils.run(stmt, ImmutableMap.of("samples", samplers));
    }
    assertEquals(sampleMean, mergedSampleMean, .1);
    {
      //Merge the sample with a simpler sampler
      String stmt = "SAMPLE_MERGE(samples, SAMPLE_INIT(10))";
      Sampler s = (Sampler) StellarProcessorUtils.run(stmt, ImmutableMap.of("samples", samplers));
      assertEquals(10, s.getSize());
    }
  }
}
