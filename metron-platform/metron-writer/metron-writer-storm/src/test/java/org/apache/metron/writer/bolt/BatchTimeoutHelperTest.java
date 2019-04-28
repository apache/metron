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
package org.apache.metron.writer.bolt;

import org.apache.storm.Config;
import org.apache.storm.utils.Utils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class BatchTimeoutHelperTest {
  private final TimeoutListSupplier defaultConfigList = new TimeoutListSupplier(Arrays.asList());
  private final TimeoutListSupplier disabledConfigList = new TimeoutListSupplier(Arrays.asList(0,0));
  private final TimeoutListSupplier smallTimeoutsList = new TimeoutListSupplier(Arrays.asList(5, 2, 4, 6));
  private final TimeoutListSupplier largeTimeoutsList = new TimeoutListSupplier(Arrays.asList(100, 200, 150, 500));
  private final TimeoutListSupplier illegalTimeoutsList = new TimeoutListSupplier(Arrays.asList(5, 2, -3, 6));

  @Test
  public void testGetMaxBatchTimeout() throws Exception {
    //The maxBatchTimeout is dependent only on batchTimeoutDivisor and the Storm config
    //and CLI overrides, which aren't of interest here.
    assertEquals(30, Utils.readStormConfig().getOrDefault(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 0));
    BatchTimeoutHelper bth;
    bth = new BatchTimeoutHelper(defaultConfigList, 1);
    assertEquals(14, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(defaultConfigList, 2);
    assertEquals(6, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(defaultConfigList, 3);
    assertEquals(4, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(defaultConfigList, 4);
    assertEquals(2, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(defaultConfigList, 6);
    assertEquals(1, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(defaultConfigList, 20);
    assertEquals(1, bth.getMaxBatchTimeout());

    bth = new BatchTimeoutHelper(disabledConfigList, 2);
    assertEquals(6, bth.getMaxBatchTimeout());
    bth = new BatchTimeoutHelper(smallTimeoutsList, 2);
    assertEquals(6, bth.getMaxBatchTimeout());
  }

  @Test
  public void testGetRecommendedTickInterval() throws Exception {
    //The recommendedTickInterval is the min of maxBatchTimeout and the configured TimeoutsList.
    BatchTimeoutHelper bth;
    bth = new BatchTimeoutHelper(defaultConfigList, 2);
    assertEquals(6, bth.getRecommendedTickInterval());
    bth = new BatchTimeoutHelper(disabledConfigList, 2);
    assertEquals(6, bth.getRecommendedTickInterval());
    bth = new BatchTimeoutHelper(largeTimeoutsList, 2);
    assertEquals(6, bth.getRecommendedTickInterval());
    bth = new BatchTimeoutHelper(smallTimeoutsList, 2);
    assertEquals(2, bth.getRecommendedTickInterval());
    bth = new BatchTimeoutHelper(illegalTimeoutsList, 2);
    assertEquals(2, bth.getRecommendedTickInterval());
  }

  public static class TimeoutListSupplier implements Supplier<List<Integer>> {
    private List<Integer> list;
    public TimeoutListSupplier(List<Integer> list) {
      this.list = list;
    }
    @Override
    public List<Integer> get() {
      return list;
    }
  }

}
