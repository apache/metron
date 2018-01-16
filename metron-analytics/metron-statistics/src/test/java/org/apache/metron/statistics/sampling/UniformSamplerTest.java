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

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class UniformSamplerTest {
  public static final int SAMPLE_SIZE = 2000000;
  static DescriptiveStatistics uniformStats = new DescriptiveStatistics();
  static List<Double> uniformSample = new ArrayList<>();
  static DescriptiveStatistics gaussianStats = new DescriptiveStatistics();
  static List<Double> gaussianSample = new ArrayList<>();

  @BeforeClass
  public static void beforeClass() {
    Random rng = new Random(0);
    GaussianRandomGenerator gen = new GaussianRandomGenerator(new MersenneTwister(0));
    for(int i = 0;i < SAMPLE_SIZE;++i) {
      double us= 10*rng.nextDouble();
      uniformSample.add(us);
      uniformStats.addValue(us);
      double gs= 10*gen.nextNormalizedDouble();
      gaussianSample.add(gs);
      gaussianStats.addValue(gs);
    }
  }

  @Test
  public void testUniformDistributionIsPreserved() {
    Sampler s = new UniformSampler(SAMPLE_SIZE/10);
    s.addAll(uniformSample);
    validateDistribution(s, uniformStats);
  }

  @Test
  public void testGaussianDistributionIsPreserved() {
    Sampler s = new UniformSampler(SAMPLE_SIZE/10);
    s.addAll(gaussianSample);
    validateDistribution(s, gaussianStats);
  }

  public void validateDistribution(Sampler sample, DescriptiveStatistics distribution) {
    DescriptiveStatistics s = new DescriptiveStatistics();
    for(Object d : sample.get()) {
      s.addValue((Double)d);
    }
    Assert.assertEquals(s.getMean(), distribution.getMean(), .1);
    Assert.assertEquals(s.getStandardDeviation(), distribution.getStandardDeviation(), .1);
  }

  @Test
  public void testMergeUniform() {
    Iterable<Sampler> subsamples = getSubsamples(uniformSample);
    Sampler s = SamplerUtil.INSTANCE.merge(subsamples, Optional.empty());
    validateDistribution(s, uniformStats);
  }

  @Test
  public void testMerge() {
    UniformSampler sampler = new UniformSampler(10);
    Iterable<Sampler> subsamples = getSubsamples(uniformSample);
    Sampler s = SamplerUtil.INSTANCE.merge(subsamples, Optional.of(sampler));
    Assert.assertEquals(s.getSize(), 10);
  }


  @Test
  public void testMergeGaussian() {
    Iterable<Sampler> subsamples = getSubsamples(gaussianSample);
    Sampler s = SamplerUtil.INSTANCE.merge(subsamples, Optional.empty());
    validateDistribution(s, gaussianStats);
  }

  public Iterable<Sampler> getSubsamples(List<Double> sample) {
    int numSamplers = 20;
    int numSamplesPerSampler = SAMPLE_SIZE/numSamplers;
    Sampler[] samplers = new Sampler[numSamplers];
    int j = 0;
    for(int i = 0;i < numSamplers;++i) {
      samplers[i] = new UniformSampler(numSamplesPerSampler/10);
      for(;j < (i+1)*numSamplesPerSampler && j < sample.size();++j) {
        samplers[i].add(sample.get(j));
      }
    }
    List<Sampler> ret = new ArrayList<>();
    for(int i = 0;i < samplers.length;++i) {
      ret.add(samplers[i]);
    }
    return ret;
  }
}
