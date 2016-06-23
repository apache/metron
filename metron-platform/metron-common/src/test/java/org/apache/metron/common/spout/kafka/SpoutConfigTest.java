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

package org.apache.metron.common.spout.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpoutConfigTest {

  /**
   {
    "retryDelayMaxMs" : 1000,
    "retryDelayMultiplier" : 1.2,
    "retryInitialDelayMs" : 2000,
    "stateUpdateIntervalMs" : 3000,
    "bufferSizeBytes" : 4000,
    "fetchMaxWait" : 5000,
    "fetchSizeBytes" : 6000,
    "maxOffsetBehind" : 7000,
    "metricsTimeBucketSizeInSecs" : 8000,
    "socketTimeoutMs" : 9000
   }
   */
  @Multiline
  public static String config;

  @Test
  public void testConfigApplication() throws IOException {
    SpoutConfig spoutConfig = new SpoutConfig(null, null, null, null);
    Map<String, Object> configMap = JSONUtils.INSTANCE.load(config, new TypeReference<Map<String, Object>>() {
    });
    SpoutConfigOptions.configure(spoutConfig, SpoutConfigOptions.coerceMap(configMap));
    Assert.assertEquals(1000, spoutConfig.retryDelayMaxMs);
    Assert.assertEquals(1.2, spoutConfig.retryDelayMultiplier, 1e-7);
    Assert.assertEquals(2000, spoutConfig.retryInitialDelayMs);
    Assert.assertEquals(3000, spoutConfig.stateUpdateIntervalMs);
    Assert.assertEquals(4000, spoutConfig.bufferSizeBytes);
    Assert.assertEquals(5000, spoutConfig.fetchMaxWait);
    Assert.assertEquals(6000, spoutConfig.fetchSizeBytes);
    Assert.assertEquals(7000, spoutConfig.maxOffsetBehind);
    Assert.assertEquals(8000, spoutConfig.metricsTimeBucketSizeInSecs);
    Assert.assertEquals(9000, spoutConfig.socketTimeoutMs);
  }
  /**
   {
    "retryDelayMaxMs" : 1000,
    "retryDelayMultiplier" : 1.2,
    "retryInitialDelayMs" : 2000,
    "stateUpdateIntervalMs" : 3000,
    "bufferSizeBytes" : 4000
   }
   */
  @Multiline
  public static String incompleteConfig;
  @Test
  public void testIncompleteConfigApplication() throws IOException {
    SpoutConfig spoutConfig = new SpoutConfig(null, null, null, null);
    Map<String, Object> configMap = JSONUtils.INSTANCE.load(incompleteConfig, new TypeReference<Map<String, Object>>() {
    });
    SpoutConfigOptions.configure(spoutConfig, SpoutConfigOptions.coerceMap(configMap));
    Assert.assertEquals(1000, spoutConfig.retryDelayMaxMs);
    Assert.assertEquals(1.2, spoutConfig.retryDelayMultiplier, 1e-7);
    Assert.assertEquals(2000, spoutConfig.retryInitialDelayMs);
    Assert.assertEquals(3000, spoutConfig.stateUpdateIntervalMs);
    Assert.assertEquals(4000, spoutConfig.bufferSizeBytes);
    Assert.assertEquals(10000, spoutConfig.fetchMaxWait); //default
    Assert.assertEquals(1024*1024, spoutConfig.fetchSizeBytes); //default
    Assert.assertEquals(Long.MAX_VALUE, spoutConfig.maxOffsetBehind);//default
    Assert.assertEquals(60, spoutConfig.metricsTimeBucketSizeInSecs); //default
    Assert.assertEquals(10000, spoutConfig.socketTimeoutMs); //default
  }

  @Test
  public void testEmptyConfigApplication() throws IOException {
    SpoutConfig spoutConfig = new SpoutConfig(null, null, null, null);
    SpoutConfigOptions.configure(spoutConfig, SpoutConfigOptions.coerceMap(new HashMap<>()));
    //ensure defaults are used
    Assert.assertEquals(60*1000, spoutConfig.retryDelayMaxMs);
    Assert.assertEquals(1.0, spoutConfig.retryDelayMultiplier, 1e-7);
    Assert.assertEquals(0, spoutConfig.retryInitialDelayMs);
    Assert.assertEquals(2000, spoutConfig.stateUpdateIntervalMs);
    Assert.assertEquals(1024*1024, spoutConfig.bufferSizeBytes);
    Assert.assertEquals(10000, spoutConfig.fetchMaxWait); //default
    Assert.assertEquals(1024*1024, spoutConfig.fetchSizeBytes); //default
    Assert.assertEquals(Long.MAX_VALUE, spoutConfig.maxOffsetBehind);//default
    Assert.assertEquals(60, spoutConfig.metricsTimeBucketSizeInSecs); //default
    Assert.assertEquals(10000, spoutConfig.socketTimeoutMs); //default
  }
}
