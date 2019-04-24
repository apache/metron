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
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class BatchSizePolicy<MESSAGE_T> implements FlushPolicy<MESSAGE_T> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Flushes a batch whenever the number of messages to be written is greater than or equal to configured batch size of
   * the sensor type.
   * @param sensorType sensor type
   * @param configurations writer configurations
   * @param messages messages to be written
   * @return true if message batch size is greater than configured batch size.
   */
  @Override
  public boolean shouldFlush(String sensorType, WriterConfiguration configurations, List<BulkMessage<MESSAGE_T>> messages) {
    boolean shouldFlush = false;
    int batchSize = messages.size();
    int configuredBatchSize = configurations.getBatchSize(sensorType);
    //Check for batchSize flush
    if (batchSize >= configuredBatchSize) {
      LOG.debug("Batch size of {} reached. Flushing {} messages for sensor {}.", configuredBatchSize, batchSize, sensorType);
      shouldFlush = true;
    }
    return shouldFlush;
  }

  @Override
  public void onFlush(String sensorType, BulkWriterResponse response) {

  }
}
