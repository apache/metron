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
import org.apache.metron.common.writer.BulkWriterMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BatchTimeoutPolicy implements FlushPolicy {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //In test scenarios, defaultBatchTimeout may not be correctly initialized, so do it here.
  //This is a conservative defaultBatchTimeout for a vanilla bolt with batchTimeoutDivisor=2
  public static final int UNINITIALIZED_DEFAULT_BATCH_TIMEOUT = 6;
  private int defaultBatchTimeout = UNINITIALIZED_DEFAULT_BATCH_TIMEOUT;
  private Clock clock = new Clock();
  private Map<String, Long> timeouts = new HashMap<>();

  public void setClock(Clock clock) {
    this.clock = clock;
  }

  public void setDefaultBatchTimeout(int defaultBatchTimeout) {
    this.defaultBatchTimeout = defaultBatchTimeout;
  }

  /**
   * Manages timeouts for each sensor type and determines when a batch should be flushed.  At the start of a new
   * batch the timeout value is computed based on the current time and configured timeout value.  Subsequent calls check to
   * see if the timeout has been reached and flushes if so.  A reset clears the timeout value for that sensor type.
   * @param sensorType sensor type
   * @param configurations writer configurations includes timeouts
   * @param batchSize number of messages to be written (not used here)
   * @return true if the timeout has been reached
   */
  @Override
  public boolean shouldFlush(String sensorType, WriterConfiguration configurations, int batchSize) {
    boolean shouldFlush = false;
    long currentTimeMillis = clock.currentTimeMillis();
    if (!timeouts.containsKey(sensorType)) {  // no timeout present so assume this is a new batch
      //This block executes at the beginning of every batch, per sensor.
      //configurations can change, so (re)init getBatchTimeout(sensorType) at start of every batch
      long batchTimeoutMs = getBatchTimeout(sensorType, configurations);
      LOG.debug("Setting batch timeout to {} for sensor {}.", batchTimeoutMs, sensorType);
      timeouts.put(sensorType, currentTimeMillis + batchTimeoutMs);
    }
    if (timeouts.get(sensorType) <= currentTimeMillis) {
      LOG.debug("Batch timeout of {} reached. Flushing {} messages for sensor {}.",
              timeouts.get(sensorType), batchSize, sensorType);
      shouldFlush = true;
    }
    return shouldFlush;
  }

  /**
   * Removes the timeout value for a sensor type.  The next call to {@link org.apache.metron.writer.BatchTimeoutPolicy#shouldFlush(String, WriterConfiguration, int)}
   * will set a new timeout.
   * @param sensorType
   */
  @Override
  public void reset(String sensorType) {
    timeouts.remove(sensorType);
  }

  /**
   * Returns the configured timeout for a sensor type in milliseconds.  The default timeout will be used if the configured timeout is
   * set to 0 or greater than the default timeout.
   * @param sensorType
   * @param configurations
   * @return
   */
  protected long getBatchTimeout(String sensorType, WriterConfiguration configurations) {
    int batchTimeoutSecs = configurations.getBatchTimeout(sensorType);
    if (batchTimeoutSecs <= 0 || batchTimeoutSecs > defaultBatchTimeout) {
      batchTimeoutSecs = defaultBatchTimeout;
    }
    return TimeUnit.SECONDS.toMillis(batchTimeoutSecs);
  }

}
