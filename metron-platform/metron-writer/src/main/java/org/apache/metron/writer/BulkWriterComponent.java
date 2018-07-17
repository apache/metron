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

import static java.lang.String.format;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private Map<String, Collection<Tuple>> sensorTupleMap = new HashMap<>();
  private Map<String, List<MESSAGE_T>> sensorMessageMap = new HashMap<>();
  private Map<String, long[]> batchTimeoutMap = new HashMap<>();
  private OutputCollector collector;
  //In test scenarios, defaultBatchTimeout may not be correctly initialized, so do it here.
  //This is a conservative defaultBatchTimeout for a vanilla bolt with batchTimeoutDivisor=2
  public static final int UNINITIALIZED_DEFAULT_BATCH_TIMEOUT = 6;
  private int defaultBatchTimeout = UNINITIALIZED_DEFAULT_BATCH_TIMEOUT;
  private boolean handleCommit = true;
  private boolean handleError = true;
  private static final int LAST_CREATE_TIME_MS = 0; //index zero'th element of long[] in batchTimeoutMap
  private static final int TIMEOUT_MS = 1;          //index next element of long[] in batchTimeoutMap
  private Clock clock = new Clock();

  public BulkWriterComponent(OutputCollector collector) {
    this.collector = collector;
  }

  public BulkWriterComponent(OutputCollector collector, boolean handleCommit, boolean handleError) {
    this(collector);
    this.handleCommit = handleCommit;
    this.handleError = handleError;
  }

  /**
   * Used only for testing.  Overrides the default (actual) wall clock.
   * @return this mutated BulkWriterComponent
   */
   public BulkWriterComponent withClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public void commit(Iterable<Tuple> tuples) {
    tuples.forEach(t -> collector.ack(t));
    if(LOG.isDebugEnabled()) {
      LOG.debug("Acking {} tuples", Iterables.size(tuples));
    }
  }

  public void commit(BulkWriterResponse response) {
    commit(response.getSuccesses());
  }

  public void error(String sensorType, Throwable e, Iterable<Tuple> tuples, MessageGetStrategy messageGetStrategy) {
    LOG.error(format("Failing %d tuple(s); sensorType=%s", Iterables.size(tuples), sensorType), e);
    MetronError error = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e);
    tuples.forEach(t -> error.addRawMessage(messageGetStrategy.get(t)));
    handleError(tuples, error);
  }

  /**
   * Error a set of tuples that may not contain a valid message.
   *
   * <p>Without a valid message, the source type is unknown.
   * <p>Without a valid message, the JSON message cannot be added to the error.
   *
   * @param e The exception that occurred.
   * @param tuples The tuples to error that may not contain valid messages.
   */
  public void error(Throwable e, Iterable<Tuple> tuples) {
    LOG.error(format("Failing %d tuple(s)", Iterables.size(tuples)), e);
    MetronError error = new MetronError()
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e);
    handleError(tuples, error);
  }

  /**
   * Errors a set of tuples.
   *
   * @param tuples The tuples to error.
   * @param error
   */
  private void handleError(Iterable<Tuple> tuples, MetronError error) {
    tuples.forEach(t -> collector.ack(t));
    ErrorUtils.handleError(collector, error);
  }

  public void error(String sensorType, BulkWriterResponse errors, MessageGetStrategy messageGetStrategy) {
    Map<Throwable, Collection<Tuple>> errorMap = errors.getErrors();
    for(Map.Entry<Throwable, Collection<Tuple>> entry : errorMap.entrySet()) {
      error(sensorType, entry.getKey(), entry.getValue(), messageGetStrategy);
    }
  }

  protected Collection<Tuple> createTupleCollection() {
    return new ArrayList<>();
  }

  public void errorAll(Throwable e, MessageGetStrategy messageGetStrategy) {
    for(String key : new HashSet<>(sensorTupleMap.keySet())) {
      errorAll(key, e, messageGetStrategy);
    }
  }

  public void errorAll(String sensorType, Throwable e, MessageGetStrategy messageGetStrategy) {
    Collection<Tuple> tuples = Optional.ofNullable(sensorTupleMap.get(sensorType)).orElse(new ArrayList<>());
    error(sensorType, e, tuples, messageGetStrategy);
    sensorTupleMap.remove(sensorType);
    sensorMessageMap.remove(sensorType);
  }

  public void write( String sensorType
                   , Tuple tuple
                   , MESSAGE_T message
                   , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                   , WriterConfiguration configurations
                   , MessageGetStrategy messageGetStrategy
                   ) throws Exception
  {
    if (!configurations.isEnabled(sensorType)) {
      collector.ack(tuple);
      return;
    }
    int batchSize = configurations.getBatchSize(sensorType);

    if (batchSize <= 1) { //simple case - no batching, no timeouts
      Collection<Tuple> tupleList = sensorTupleMap.get(sensorType);  //still read in case batchSize changed
      if (tupleList == null) {
        tupleList = createTupleCollection();
      }
      tupleList.add(tuple);

      List<MESSAGE_T> messageList = sensorMessageMap.get(sensorType);  //still read in case batchSize changed
      if (messageList == null) {
        messageList = new ArrayList<>();
      }
      messageList.add(message);

      flush(sensorType, bulkMessageWriter, configurations, messageGetStrategy, tupleList, messageList);
      return;
    }

    //Otherwise do the full batch buffering with timeouts
    long[] batchTimeoutInfo = batchTimeoutMap.get(sensorType);
    if (batchTimeoutInfo == null) {
      //lazily create the batchTimeoutInfo array, once per sensor.
      batchTimeoutInfo = new long[] {0L, 0L};
      batchTimeoutMap.put(sensorType, batchTimeoutInfo);
    }

    Collection<Tuple> tupleList = sensorTupleMap.get(sensorType);
    if (tupleList == null) {
      //This block executes at the beginning of every batch, per sensor.
      tupleList = createTupleCollection();
      sensorTupleMap.put(sensorType, tupleList);
      batchTimeoutInfo[LAST_CREATE_TIME_MS] = clock.currentTimeMillis();
      //configurations can change, so (re)init getBatchTimeout(sensorType) at start of every batch
      int batchTimeoutSecs = configurations.getBatchTimeout(sensorType);
      if (batchTimeoutSecs <= 0 || batchTimeoutSecs > defaultBatchTimeout) {
        batchTimeoutSecs = defaultBatchTimeout;
      }
      batchTimeoutInfo[TIMEOUT_MS] = TimeUnit.SECONDS.toMillis(batchTimeoutSecs);
    }
    tupleList.add(tuple);

    List<MESSAGE_T> messageList = sensorMessageMap.get(sensorType);
    if (messageList == null) {
      messageList = new ArrayList<>();
      sensorMessageMap.put(sensorType, messageList);
    }
    messageList.add(message);

    //Check for batchSize flush
    if (tupleList.size() >= batchSize) {
      flush(sensorType, bulkMessageWriter, configurations, messageGetStrategy, tupleList, messageList);
      return;
    }
    //Check for batchTimeout flush (if the tupleList isn't brand new).
    //Debugging note: If your queue always flushes at length==2 regardless of feed rate,
    //it may mean defaultBatchTimeout has somehow been set to zero.
    if (tupleList.size() > 1 && (clock.currentTimeMillis() - batchTimeoutInfo[LAST_CREATE_TIME_MS] >= batchTimeoutInfo[TIMEOUT_MS])) {
      flush(sensorType, bulkMessageWriter, configurations, messageGetStrategy, tupleList, messageList);
      return;
    }
  }

  private void flush( String sensorType
                    , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                    , WriterConfiguration configurations
		                , MessageGetStrategy messageGetStrategy
                    , Collection<Tuple> tupleList
                    , List<MESSAGE_T> messageList
                    ) throws Exception
  {
    long startTime = System.currentTimeMillis(); //no need to mock, so use real time
    try {
      BulkWriterResponse response = bulkMessageWriter.write(sensorType, configurations, tupleList, messageList);

      // Commit or error piecemeal.
      if(handleCommit) {
        commit(response);
      }

      if(handleError) {
        error(sensorType, response, messageGetStrategy);
      } else if (response.hasErrors()) {
        throw new IllegalStateException("Unhandled bulk errors in response: " + response.getErrors());
      }
    } catch (Throwable e) {
      if(handleError) {
        error(sensorType, e, tupleList, messageGetStrategy);
      }
      else {
        throw e;
      }
    }
    finally {
      sensorTupleMap.remove(sensorType);
      sensorMessageMap.remove(sensorType);
    }
    long endTime = System.currentTimeMillis();
    long elapsed = endTime - startTime;
    LOG.debug("Bulk batch for sensor {} completed in ~{} ns", sensorType, elapsed);
  }

  // Flushes all queues older than their batchTimeouts.
  public void flushTimeouts(
            BulkMessageWriter<MESSAGE_T> bulkMessageWriter
          , WriterConfiguration configurations
          , MessageGetStrategy messageGetStrategy
          ) throws Exception
  {
    // No need to do "all" sensorTypes here, just the ones that have data batched up.
    // Note queues with batchSize == 1 don't get batched, so they never persist in the sensorTupleMap.
    for (String sensorType : sensorTupleMap.keySet()) {
      long[] batchTimeoutInfo = batchTimeoutMap.get(sensorType);
      if (batchTimeoutInfo == null  //Shouldn't happen, but conservatively flush if so
          || clock.currentTimeMillis() - batchTimeoutInfo[LAST_CREATE_TIME_MS] >= batchTimeoutInfo[TIMEOUT_MS]) {
        flush(sensorType, bulkMessageWriter, configurations, messageGetStrategy
	            , sensorTupleMap.get(sensorType), sensorMessageMap.get(sensorType));
        return;
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
