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
package org.apache.metron.common.writer;

import org.junit.Assert;
import org.junit.Test;

public class NoopWriterTest {
  @Test
  public void testFixedLatencyConfig() {
    NoopWriter writer = new NoopWriter().withLatency("10");
    Assert.assertTrue(writer.sleepFunction instanceof NoopWriter.FixedLatency);
    NoopWriter.FixedLatency sleepFunction = (NoopWriter.FixedLatency)writer.sleepFunction;
    Assert.assertEquals(10, sleepFunction.getLatency());
  }

  private void ensureRandomLatencyConfig(String latencyConfig, int min, int max) {
    NoopWriter writer = new NoopWriter().withLatency(latencyConfig);
    Assert.assertTrue(writer.sleepFunction instanceof NoopWriter.RandomLatency);
    NoopWriter.RandomLatency sleepFunction = (NoopWriter.RandomLatency)writer.sleepFunction;
    Assert.assertEquals(min, sleepFunction.getMin());
    Assert.assertEquals(max, sleepFunction.getMax());
  }

  @Test
  public void testRandomLatencyConfig() {
    ensureRandomLatencyConfig("10,20", 10, 20);
    ensureRandomLatencyConfig("10, 20", 10, 20);
    ensureRandomLatencyConfig("10 ,20", 10, 20);
    ensureRandomLatencyConfig("10 , 20", 10, 20);
  }

}
