package org.apache.metron.common.math.stats.outlier;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

public class MedianAbsoluteDeviationTest {

  @Test
  public void test() {

    GaussianRandomGenerator gaussian = new GaussianRandomGenerator(new MersenneTwister(0L));
    for(int i = 0;i < 1000000;++i) {

    }
  }
}
