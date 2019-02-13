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
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This component manages an internal cache of messages to be written in batch.  A separate cache is used for each sensor.
 * Each time a message is written to this component, flush policies are applied to determine if a batch of messages should
 * be flushed.  When a flush does happen, this component calls the supplied {@link org.apache.metron.writer.BulkWriterResponseHandler}
 * so that the calling class can handle any necessary message acknowledgement.  This component also ensures all messages
 * in a batch are included in the response as either a success or failure.
 *
 * @param <MESSAGE_T>
 */
public class BulkWriterComponent<MESSAGE_T> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private Map<String, List<BulkWriterMessage<MESSAGE_T>>> sensorMessageCache = new HashMap<>();
  private BulkWriterResponseHandler bulkWriterResponseHandler;
  private BatchTimeoutPolicy batchTimeoutPolicy;
  private List<FlushPolicy> flushPolicies;

  public BulkWriterComponent(BulkWriterResponseHandler bulkWriterResponseHandler) {
    this.bulkWriterResponseHandler = bulkWriterResponseHandler;
    flushPolicies = new ArrayList<>();
    flushPolicies.add(new BatchSizePolicy());
    batchTimeoutPolicy = new BatchTimeoutPolicy();
    flushPolicies.add(batchTimeoutPolicy);
  }

  /**
   * Used only for testing.  Overrides the default (actual) wall clock.
   * @return this mutated BulkWriterComponent
   */
   public BulkWriterComponent<MESSAGE_T> withClock(Clock clock) {
     this.batchTimeoutPolicy.setClock(clock);
     return this;
  }

  /**
   * Accepts a message to be written and stores it in an internal cache of messages.  Iterates through {@link org.apache.metron.writer.FlushPolicy}
   * implementations to determine if a batch should be flushed.
   * @param sensorType sensor type
   * @param messageId messageId used to track write success/failure
   * @param message message to be written
   * @param bulkMessageWriter writer that will do the actual writing
   * @param configurations writer configurations
   */
  public void write( String sensorType
                   , String messageId
                   , MESSAGE_T message
                   , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                   , WriterConfiguration configurations
                   )
  {
    // if sensor is disabled a response should still be sent so that the message can be acknowledged
    if (!configurations.isEnabled(sensorType)) {
      BulkWriterResponse response = new BulkWriterResponse();
      response.addSuccess(messageId);
      bulkWriterResponseHandler.handleFlush(sensorType, response);
      return;
    }

    List<BulkWriterMessage<MESSAGE_T>> messages = sensorMessageCache.getOrDefault(sensorType, new ArrayList<>());
    sensorMessageCache.put(sensorType, messages);
    messages.add(new BulkWriterMessage<>(messageId, message));

    applyFlushPolicies(sensorType, bulkMessageWriter, configurations, sensorMessageCache.get(sensorType));
  }

  /**
   * Flushes a batch for a sensor type by writing messages with the supplied {@link org.apache.metron.common.writer.BulkMessageWriter}.
   * Ensures all message ids in a batch are included in the response passed to the {@link org.apache.metron.writer.BulkWriterResponseHandler}.
   * After messages are written the cache is cleared and flush policies are reset for that sensor type.
   * @param sensorType sensor type
   * @param bulkMessageWriter writer that will do the actual writing
   * @param configurations writer configurations
   * @param messages messages to be written
   */
  protected void flush( String sensorType
                    , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                    , WriterConfiguration configurations
                    , List<BulkWriterMessage<MESSAGE_T>> messages
                    )
  {
    long startTime = System.currentTimeMillis(); //no need to mock, so use real time
    BulkWriterResponse response = new BulkWriterResponse();

    Set<String> ids = messages.stream().map(BulkWriterMessage::getId).collect(Collectors.toSet());
    try {
      response = bulkMessageWriter.write(sensorType, configurations, messages);

      // Make sure all ids are included in the BulkWriterResponse
      ids.removeAll(response.getSuccesses());
      response.getErrors().values().forEach(ids::removeAll);
      response.addAllSuccesses(ids);
    } catch (Throwable e) {
      response.addAllErrors(e, ids);
    }
    finally {
      bulkWriterResponseHandler.handleFlush(sensorType, response);
      sensorMessageCache.remove(sensorType);
      flushPolicies.forEach(flushPolicy -> flushPolicy.reset(sensorType));
    }
    long endTime = System.currentTimeMillis();
    long elapsed = endTime - startTime;
    LOG.debug("Bulk batch for sensor {} wrote {} messages in ~{} ms", sensorType, messages.size(), elapsed);
  }

  /**
   * Flushes all queues older than their batchTimeouts.
   * @param bulkMessageWriter writer that will do the actual writing
   * @param configurations writer configurations
   */
  public void flushTimeouts(
            BulkMessageWriter<MESSAGE_T> bulkMessageWriter
          , WriterConfiguration configurations
          )
  {
    // No need to do "all" sensorTypes here, just the ones that have data batched up.
    for (String sensorType : new HashSet<>(sensorMessageCache.keySet())) {
      applyFlushPolicies(sensorType, bulkMessageWriter, configurations, sensorMessageCache.get(sensorType));
    }
  }

  /**
   * Applies flush policies for a sensor type.  A batch is flushed whenever the first policy indicates a flush is needed.
   * @param sensorType sensor type
   * @param bulkMessageWriter writer that will do the actual writing
   * @param configurations writer configurations
   * @param messages messages to be written
   */
  private void applyFlushPolicies(String sensorType
          , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
          , WriterConfiguration configurations
          , List<BulkWriterMessage<MESSAGE_T>> messages) {
    if (messages.size() > 0) { // no need to flush empty batches
      for(FlushPolicy flushPolicy: flushPolicies) {
        if (flushPolicy.shouldFlush(sensorType, configurations, messages.size())) {
          flush(sensorType, bulkMessageWriter, configurations, messages);
          break;
        }
      }
    }
  }

  /**
   * @param defaultBatchTimeout
   */
  public void setDefaultBatchTimeout(int defaultBatchTimeout) {
    this.batchTimeoutPolicy.setDefaultBatchTimeout(defaultBatchTimeout);
  }
}
