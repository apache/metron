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

import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link org.apache.metron.writer.FlushPolicy} implementation for Storm that handles tuple acking and error
 * reporting by handling flush events for writer responses.
 */
public class AckTuplesPolicy<MESSAGE_T> implements FlushPolicy<MESSAGE_T> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Tracks the messages from a tuple that have not been flushed
  private Map<Tuple, Collection<MessageId>> tupleMessageMap = new HashMap<>();

  // Tracks the errors that have been reported for a Tuple.  We only want to report an error once.
  private Map<Tuple, Set<Throwable>> tupleErrorMap = new HashMap<>();
  private OutputCollector collector;
  private MessageGetStrategy messageGetStrategy;

  public AckTuplesPolicy(OutputCollector collector, MessageGetStrategy messageGetStrategy) {
    this.collector = collector;
    this.messageGetStrategy = messageGetStrategy;
  }

  // Used only for unit testing
  protected Map<Tuple, Collection<MessageId>> getTupleMessageMap() {
    return tupleMessageMap;
  }

  // Used only for unit testing
  protected Map<Tuple, Set<Throwable>> getTupleErrorMap() {
    return tupleErrorMap;
  }

  @Override
  public boolean shouldFlush(String sensorType, WriterConfiguration configurations, List<BulkMessage<MESSAGE_T>> messages) {
    return false;
  }

  @Override
  public void onFlush(String sensorType, BulkWriterResponse response) {
    LOG.debug("Handling flushed messages for sensor {} with response: {}", sensorType, response);

    // Update tuple message map.  Tuple is ready to ack when all it's messages have been flushed.
    Collection<Tuple> tuplesToAck = new ArrayList<>();
    tupleMessageMap = tupleMessageMap.entrySet().stream()
            .map(entry -> {
              Tuple tuple = entry.getKey();
              Collection<MessageId> ids = new ArrayList<>(entry.getValue());

              // Remove successful messages from tuple message map
              ids.removeAll(response.getSuccesses());

              // Remove failed messages from tuple message map
              response.getErrors().forEach((throwable, failedIds) -> {
                if (ids.removeAll(failedIds)) {
                  // Add an error to be reported when a tuple is acked
                  Set<Throwable> errorList = tupleErrorMap.getOrDefault(tuple, new HashSet<>());
                  tupleErrorMap.put(tuple, errorList);
                  errorList.add(throwable);
                  handleError(sensorType, throwable, tuple);
                }
              });
              return new AbstractMap.SimpleEntry<>(tuple, ids);
            })
            .filter(entry -> {
              // Tuple is ready to be acked when all messages have succeeded/failed
              if (entry.getValue().isEmpty()) {
                tuplesToAck.add(entry.getKey());

                // Remove the tuple from tuple message map
                return false;
              }
              return true;
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    LOG.debug("Acking {} tuples for sensor {}", tuplesToAck.size(), sensorType);
    tuplesToAck.forEach(tuple -> {
      collector.ack(tuple);
    });

    // Determine which tuples failed
    Collection<Tuple> failedTuples = tuplesToAck.stream()
            .filter(tuple -> tupleErrorMap.containsKey(tuple))
            .collect(Collectors.toList());
    LOG.debug("Failing {} tuple(s) for sensorType {}", failedTuples.size(), sensorType);

    Set<Throwable> errorsToReport = new HashSet<>();
    failedTuples.forEach(tuple -> {
      // Add the error to the errorsToReport Set so duplicate errors are removed
      errorsToReport.addAll(tupleErrorMap.remove(tuple));
    });

    errorsToReport.forEach(throwable -> {
      // there is only one error to report for all of the failed tuples
      collector.reportError(throwable);
    });
  }

  /**
   * Adds a tuple to be acked when all messages have been processed (either as a successful write or a failure).
   * @param tuple
   * @param messageIds
   */
  public void addTupleMessageIds(Tuple tuple, Collection<String> messageIds) {
    LOG.debug("Adding tuple with messages ids: {}", String.join(",", messageIds));
    tupleMessageMap.put(tuple, messageIds.stream().map(MessageId::new).collect(Collectors.toSet()));
  }

  private void handleError(String sensorType, Throwable e, Tuple tuple) {
    MetronError error = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .addRawMessage(messageGetStrategy.get(tuple));
    collector.emit(Constants.ERROR_STREAM, new Values(error.getJSONObject()));
  }
}
