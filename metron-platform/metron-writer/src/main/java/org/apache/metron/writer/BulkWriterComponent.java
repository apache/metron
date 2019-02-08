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
import org.apache.metron.common.writer.BulkWriterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This component implements message batching, with both flush on queue size, and flush on queue timeout.
 * There is a queue for each sensorType.
 * Ideally each queue would have its own timer, but we only have one global timer, the Tick Tuple
 * generated at fixed intervals by the system and received by the Bolt.  Given this constraint,
 * we use the following strategy:
 *   - The default batchTimeout is, as recommended by Storm, 1/2 the Storm 'topology.message.timeout.secs',
 *   modified by batchTimeoutDivisor, in case multiple batching writers are daisy-chained in one topology.
 *   - If some sensors configure their own batchTimeouts, they are compared with the default.  Batch
 *   timeouts greater than the default will be ignored, because they can cause message recycling in Storm.
 *   Batch timeouts configured to {@literal <}= zero, or undefined, mean use the default.
 *   - The *smallest* configured batchTimeout among all sensor types, greater than zero and less than
 *   the default, will be used to configure the 'topology.tick.tuple.freq.secs' for the Bolt.  If there are no
 *   valid configured batchTimeouts, the defaultBatchTimeout will be used.
 *   - The age of the queue is checked every time a sensor message arrives.  Thus, if at least one message
 *   per second is received for a given sensor, that queue will flush on timeout or sooner, depending on batchSize.
 *   - On each Tick Tuple received, *all* queues will be checked, and if any are older than their respective
 *   batchTimeout, they will be flushed.  Note that this does NOT guarantee timely flushing, depending on the
 *   phase relationship between the queue's batchTimeout and the tick interval.  The maximum age of a queue
 *   before it flushes is its batchTimeout + the tick interval, which is guaranteed to be less than 2x the
 *   batchTimeout, and also less than the 'topology.message.timeout.secs'.  This guarantees that the messages
 *   will not age out of the Storm topology, but it does not guarantee the flush interval requested, for
 *   sensor types not receiving at least one message every second.
 *
 * @param <MESSAGE_T>
 */
public class BulkWriterComponent<MESSAGE_T> {
  public static final Logger LOG = LoggerFactory
            .getLogger(BulkWriterComponent.class);
  private Map<String, Map<String, MESSAGE_T>> sensorMessageMap = new LinkedHashMap<>();
  private Map<String, long[]> batchTimeoutMap = new HashMap<>();
  //In test scenarios, defaultBatchTimeout may not be correctly initialized, so do it here.
  //This is a conservative defaultBatchTimeout for a vanilla bolt with batchTimeoutDivisor=2
  public static final int UNINITIALIZED_DEFAULT_BATCH_TIMEOUT = 6;
  private int defaultBatchTimeout = UNINITIALIZED_DEFAULT_BATCH_TIMEOUT;
  private static final int LAST_CREATE_TIME_MS = 0; //index zero'th element of long[] in batchTimeoutMap
  private static final int TIMEOUT_MS = 1;          //index next element of long[] in batchTimeoutMap
  private Clock clock = new Clock();
  private BulkWriterResponseHandler bulkWriterResponseHandler;

  public BulkWriterComponent(BulkWriterResponseHandler bulkWriterResponseHandler) {
    this.bulkWriterResponseHandler = bulkWriterResponseHandler;
  }

  /**
   * Used only for testing.  Overrides the default (actual) wall clock.
   * @return this mutated BulkWriterComponent
   */
   public BulkWriterComponent<MESSAGE_T> withClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public void write( String sensorType
                   , String messageId
                   , MESSAGE_T message
                   , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                   , WriterConfiguration configurations
                   ) throws Exception
  {
    if (!configurations.isEnabled(sensorType)) {
      BulkWriterResponse response = new BulkWriterResponse();
      response.addSuccess(messageId);
      bulkWriterResponseHandler.handleFlush(sensorType, response);
      return;
    }
    int batchSize = configurations.getBatchSize(sensorType);

    Map<String, MESSAGE_T> messageList = sensorMessageMap.getOrDefault(sensorType, new LinkedHashMap<>());
    sensorMessageMap.put(sensorType, messageList);
    if (batchSize <= 1) { //simple case - no batching, no timeouts

      messageList.put(messageId, message);

        //still read in case batchSize changed
      flush(sensorType, bulkMessageWriter, configurations, messageList);
      return;
    }

    //Otherwise do the full batch buffering with timeouts
    long[] batchTimeoutInfo = batchTimeoutMap.get(sensorType);
    if (batchTimeoutInfo == null) {
      //lazily create the batchTimeoutInfo array, once per sensor.
      batchTimeoutInfo = new long[] {0L, 0L};
      batchTimeoutMap.put(sensorType, batchTimeoutInfo);
    }

    if (messageList.isEmpty()) {
      //This block executes at the beginning of every batch, per sensor.
      batchTimeoutInfo[LAST_CREATE_TIME_MS] = clock.currentTimeMillis();
      //configurations can change, so (re)init getBatchTimeout(sensorType) at start of every batch
      int batchTimeoutSecs = configurations.getBatchTimeout(sensorType);
      if (batchTimeoutSecs <= 0 || batchTimeoutSecs > defaultBatchTimeout) {
        batchTimeoutSecs = defaultBatchTimeout;
      }
      batchTimeoutInfo[TIMEOUT_MS] = TimeUnit.SECONDS.toMillis(batchTimeoutSecs);
      LOG.debug("Setting batch timeout to {} for sensor {}.", batchTimeoutInfo[TIMEOUT_MS], sensorType);
    }

    messageList.put(messageId, message);

    //Check for batchSize flush
    if (messageList.size() >= batchSize) {
      LOG.debug("Batch size of {} reached. Flushing {} messages for sensor {}.", batchSize, messageList.size(), sensorType);
      flush(sensorType, bulkMessageWriter, configurations, messageList);
      return;
    }
    //Check for batchTimeout flush (if the tupleList isn't brand new).
    //Debugging note: If your queue always flushes at length==2 regardless of feed rate,
    //it may mean defaultBatchTimeout has somehow been set to zero.
    long elapsed = clock.currentTimeMillis() - batchTimeoutInfo[LAST_CREATE_TIME_MS];
    if (messageList.size() > 1 && (elapsed >= batchTimeoutInfo[TIMEOUT_MS])) {
      LOG.debug("Batch timeout of {} reached because {} ms have elapsed. Flushing {} messages for sensor {}.",
              batchTimeoutInfo[TIMEOUT_MS], elapsed, messageList.size(), sensorType);
      flush(sensorType, bulkMessageWriter, configurations, messageList);
      return;
    }
  }

  protected void flush( String sensorType
                    , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                    , WriterConfiguration configurations
                    , Map<String, MESSAGE_T> messages
                    ) throws Exception
  {
    long startTime = System.currentTimeMillis(); //no need to mock, so use real time
    BulkWriterResponse response = new BulkWriterResponse();
    try {
       response = bulkMessageWriter.write(sensorType, configurations, messages);

      // Make sure all ids are included in the BulkWriterResponse
      Set<String> ids = new HashSet<>(messages.keySet());
      ids.removeAll(response.getSuccesses());
      response.getErrors().values().forEach(ids::removeAll);
      response.addAllSuccesses(ids);
    } catch (Throwable e) {
      response.addAllErrors(e, messages.keySet());
    }
    finally {
      bulkWriterResponseHandler.handleFlush(sensorType, response);
      sensorMessageMap.remove(sensorType);
    }
    long endTime = System.currentTimeMillis();
    long elapsed = endTime - startTime;
    LOG.debug("Bulk batch for sensor {} wrote {} messages in ~{} ms", sensorType, messages.size(), elapsed);
  }

  // Flushes all queues older than their batchTimeouts.
  public void flushTimeouts(
            BulkMessageWriter<MESSAGE_T> bulkMessageWriter
          , WriterConfiguration configurations
          ) throws Exception
  {
    // No need to do "all" sensorTypes here, just the ones that have data batched up.
    // Note queues with batchSize == 1 don't get batched, so they never persist in the sensorGroupMap.
    for (String sensorType : sensorMessageMap.keySet()) {
      long[] batchTimeoutInfo = batchTimeoutMap.get(sensorType);
      if (batchTimeoutInfo == null) {  //Shouldn't happen, but conservatively flush if so
        flush(sensorType, bulkMessageWriter, configurations, sensorMessageMap.get(sensorType));
        continue;
      }
      long elapsed = clock.currentTimeMillis() - batchTimeoutInfo[LAST_CREATE_TIME_MS];
      if (elapsed >= batchTimeoutInfo[TIMEOUT_MS]) {
        LOG.debug("Flush timeouts called.  Batch timeout of {} reached because {} ms have elapsed. Flushing {} messages for sensor {}.",
                batchTimeoutInfo[TIMEOUT_MS], elapsed, sensorType, sensorMessageMap.get(sensorType).size());
        flush(sensorType, bulkMessageWriter, configurations, sensorMessageMap.get(sensorType));
      }
    }
  }

  /**
   * @param defaultBatchTimeout
   */
  public void setDefaultBatchTimeout(int defaultBatchTimeout) {
    this.defaultBatchTimeout = defaultBatchTimeout;
  }
}
