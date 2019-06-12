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

package org.apache.metron.writer;

import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.BulkMessage;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchTimeoutPolicyTest {

  private String sensor1 = "sensor1";
  private String sensor2 = "sensor2";
  private WriterConfiguration configurations = mock(WriterConfiguration.class);
  private int maxBatchTimeout = 6;
  private List<BulkMessage<JSONObject>> messages = new ArrayList<>();

  @Test
  public void shouldFlushSensorsOnTimeouts() {
    Clock clock = mock(Clock.class);

    BatchTimeoutPolicy batchTimeoutPolicy = new BatchTimeoutPolicy<>(maxBatchTimeout, clock);
    when(configurations.getBatchTimeout(sensor1)).thenReturn(1);
    when(configurations.getBatchTimeout(sensor2)).thenReturn(2);

    when(clock.currentTimeMillis()).thenReturn(0L); // initial check
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor2, configurations, messages));

    when(clock.currentTimeMillis()).thenReturn(999L); // no timeouts yet
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor2, configurations, messages));

    when(clock.currentTimeMillis()).thenReturn(1000L); // first sensor timeout reached
    assertTrue(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor2, configurations, messages));

    when(clock.currentTimeMillis()).thenReturn(2000L); // second sensor timeout reached
    assertTrue(batchTimeoutPolicy.shouldFlush(sensor2, configurations, messages));
  }

  @Test
  public void shouldResetTimeouts() {
    Clock clock = mock(Clock.class);

    BatchTimeoutPolicy batchTimeoutPolicy = new BatchTimeoutPolicy(maxBatchTimeout, clock);
    when(configurations.getBatchTimeout(sensor1)).thenReturn(1);

    when(clock.currentTimeMillis()).thenReturn(0L); // initial check
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));

    batchTimeoutPolicy.onFlush(sensor1, new BulkWriterResponse());

    when(clock.currentTimeMillis()).thenReturn(1000L); // sensor was reset so shouldn't timeout
    assertFalse(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));

    when(clock.currentTimeMillis()).thenReturn(2000L); // sensor timeout should be 2 now
    assertTrue(batchTimeoutPolicy.shouldFlush(sensor1, configurations, messages));
  }

  @Test
  public void getBatchTimeoutShouldReturnConfiguredTimeout() {
    BatchTimeoutPolicy batchTimeoutPolicy = new BatchTimeoutPolicy(maxBatchTimeout);

    when(configurations.getBatchTimeout(sensor1)).thenReturn(5);

    assertEquals(5000L, batchTimeoutPolicy.getBatchTimeout(sensor1, configurations));
  }

  @Test
  public void getBatchTimeoutShouldReturnMaxBatchTimeout() {
    BatchTimeoutPolicy batchTimeoutPolicy = new BatchTimeoutPolicy(maxBatchTimeout);

    when(configurations.getBatchTimeout(sensor1)).thenReturn(0);

    assertEquals(maxBatchTimeout * 1000, batchTimeoutPolicy.getBatchTimeout(sensor1, configurations));
  }
}
