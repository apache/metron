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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.metron.statistics.outlier.rad.RPCAOutlier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.function.Function;

@RunWith(Parameterized.class)
public class RPCAOutlierTest {
  public static int NUM_SAMPLES = 2500;

  public interface Sample {

    default Iterable<Double> getInlier() {
      double min = Double.MAX_VALUE;
      double max = Long.MIN_VALUE;
      for(Object dObj : getSample()) {
        Double d = (Double)dObj;
        min = Math.min(d, min);
        max = Math.max(d, max);
      }
      double width = Math.abs(max - min);
      return ImmutableList.of(min  + width/2, max - width/2);
    }

    default Iterable<Double> getOutlier() {
      double min = Double.MAX_VALUE;
      double max = Long.MIN_VALUE;
      for(Object dObj : getSample()) {
        Double d = (Double)dObj;
        min = Math.min(d, min);
        max = Math.max(d, max);
      }
      double width = Math.abs(max - min);
      return ImmutableList.of(max + 5*width, min - 5*width);
    }
    List<? extends Object> getSample();
  }

  public static class RandomSample implements Sample {

    List<Double> sample = new ArrayList<>();

    public RandomSample(RealDistribution distribution) {
      for(int i = 0;i < NUM_SAMPLES;++i) {
        double d = distribution.sample();
        sample.add(d);
      }
    }

    @Override
    public List<? extends Object> getSample() {
      return sample;
    }
  }

  public static class PeriodicSample implements Sample {
    public static int numPeriods = 5;
    public static double periodSize = Math.PI*2;
    double endPoint = periodSize*numPeriods;
    double startPoint = 0;
    public double delta;
    List<Double> domain = new ArrayList<>();
    List<Double> sample = new ArrayList<>();
    Function<Double, Double> periodicFunction;
    public PeriodicSample(double periodSize, Function<Double, Double> periodicFunction) {
      this.periodSize = periodSize;
      this.periodicFunction = periodicFunction;
      delta = Math.abs(endPoint - startPoint)/NUM_SAMPLES;
      generate();
    }

    private void generate() {
      for(double x = startPoint;x <= endPoint;x += delta) {
        domain.add(x);
        sample.add(periodicFunction.apply(x));
      }

    }


    @Override
    public Iterable<Double> getInlier() {
      return ImmutableList.of(
               periodicFunction.apply(domain.get(domain.size() - 1) + delta)
              ,periodicFunction.apply(domain.get(domain.size() - 2) + delta)
      );
    }

    @Override
    public List<? extends Object> getSample() {
      return sample;
    }
  }

  public enum Samples implements Sample {
    PERIODIC(new PeriodicSample(2*Math.PI, x -> Math.sin(x) + 10))
    ,PERIODIC_LOW_FREQ(new PeriodicSample(20*Math.PI, x -> 10*Math.cos(x) + 10))
    ,RANDOM_NORMAL(new RandomSample(new NormalDistribution(new JDKRandomGenerator(0), 10, 1)))
    ,RANDOM_NORMAL_WIDE(new RandomSample(new NormalDistribution(new JDKRandomGenerator(0), 10, 100)))
    ,RANDOM_UNIFORM(new RandomSample(new UniformRealDistribution(new JDKRandomGenerator(0), -10, 10)))
    ;
    Sample sample;
    Samples(Sample s) {
      this.sample = s;
    }

    @Override
    public Iterable<Double> getInlier() {
      return sample.getInlier();
    }

    @Override
    public Iterable<Double> getOutlier() {
      return sample.getOutlier();
    }

    @Override
    public List<? extends Object> getSample() {
      return sample.getSample();
    }
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    List<Object[]> ret = new ArrayList<>();
    for(Samples s : Samples.values()) {
      ret.add(new Object[] {s});
    }
    return ret;
  }

  Sample s;
  public RPCAOutlierTest(Sample s) {
    this.s = s;
  }


  @Test
  public void testSimpleOutlier() {
    testSimpleOutlier(s.getSample());
  }

  public void testSimpleOutlier(List<? extends Object> sample) {
    for(Double o : s.getOutlier()) {
      double scoreOutlier = score(sample, o);
      Assert.assertTrue("RPCA(" + o + ") = |" + scoreOutlier + "| <= 1.0", Math.abs(scoreOutlier) > 1.0);
    }

    for(Double i : s.getInlier()) {
      double scoreInlier = score(sample, i);
      Assert.assertEquals("RPCA(" + i + ") = |" + scoreInlier + "| != 0.0", 0, scoreInlier, 1e-5);
    }
  }

  protected double score(List<? extends Object> sample, Double x) {
    RPCAOutlier outlier = new RPCAOutlier();
    return outlier.outlierScore(sample, x);
  }

  public List<Double> resample(int size) {
    List<Double> tmp = new ArrayList<>();
    tmp.addAll((Collection<? extends Double>) s.getSample());
    Collections.shuffle(tmp, new Random(0));
    List<Double> ret = new ArrayList<>();
    Iterables.addAll(ret, Iterables.limit(tmp, size));
    return ret;
  }

  @Test
  public void testResampledOutlier() {
    testSimpleOutlier(resample(NUM_SAMPLES/2));
  }
}
