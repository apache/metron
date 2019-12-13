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

package org.apache.metron.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.aggregator.Aggregators;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AggregatorsTest {
  @Test
  public void testMax() {
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.MAX.aggregate(ImmutableList.of(1, 5, -1, 7), new HashMap<>()), 1e-7);
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.MAX.aggregate(ImmutableList.of(1, 5, -1, -2), new HashMap<>()), 1e-7);
    assertEquals(7d, Aggregators.MAX.aggregate(ImmutableList.of(1, 5, -1, 7, 0), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }

  @Test
  public void testMin() {
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.MIN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), new HashMap<>()), 1e-7);
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.MIN.aggregate(ImmutableList.of(1, 5, -1, -2, 0), new HashMap<>()), 1e-7);
    assertEquals(-1d, Aggregators.MIN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }

  @Test
  public void testMinAllPositive() {
    assertEquals(1, Aggregators.MIN.aggregate(ImmutableList.of(1, 5, 7), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }

  @Test
  public void testMean() {
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.MEAN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), new HashMap<>()), 1e-7);
    assertEquals(12.0/5.0, Aggregators.MEAN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }

  @Test
  public void testPositiveMean() {
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.POSITIVE_MEAN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), new HashMap<>()), 1e-7);
    assertEquals(13.0/3.0, Aggregators.POSITIVE_MEAN.aggregate(ImmutableList.of(1, 5, -1, 7, 0), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }

  @Test
  public void testSum() {
    assertEquals(Double.NEGATIVE_INFINITY, Aggregators.SUM.aggregate(ImmutableList.of(1, 5, -1, 7), new HashMap<>()), 1e-7);
    assertEquals(1 + 5 + -1 + 7, Aggregators.SUM.aggregate(ImmutableList.of(1, 5, -1, 7), ImmutableMap.of(Aggregators.NEGATIVE_VALUES_TRUMP_CONF, "false")), 1e-7);
  }
}
