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
import org.apache.metron.statistics.outlier.rad.RPCAOutlier;
import org.apache.metron.statistics.outlier.rad.RPCAOutlierFunctions;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StellarRPCAOutlierTest extends RPCAOutlierTest {
  public StellarRPCAOutlierTest(Sample s) {
    super(s);
  }

  @Override
  protected double score(List<? extends Object> sample, Double x) {
    String stmt = "OUTLIER_RPCA_SCORE(sample, x)";
    return (Double) StellarProcessorUtils.run(stmt, ImmutableMap.of("sample", sample, "x", x));
  }

  @Test
  public void testConfig() {
    RPCAOutlier outlier = new RPCAOutlier();
    RPCAOutlierFunctions.Config.configure(outlier
            , ImmutableMap.of("lpenalty", 15.0, "spenalty", 12, "forceDiff", true, "minNonZero", 5));
    Assert.assertTrue(outlier.getForceDiff());
    Assert.assertEquals(15.0, outlier.getLpenalty(), 1e-5);
    Assert.assertEquals(12.0, outlier.getSpenalty(), 1e-5);
    Assert.assertEquals(5, outlier.getMinRecords());
  }
}
