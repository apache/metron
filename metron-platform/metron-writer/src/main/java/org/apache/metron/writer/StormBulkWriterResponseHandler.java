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
import org.apache.metron.common.error.MetronError;
import org.apache.metron.common.message.MessageGetStrategy;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

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
 * A {@link org.apache.metron.writer.BulkWriterResponseHandler} implementation for Storm.  This class handles tuple acking and error
 * reporting by handling flush events for writer responses.
 */
public class StormBulkWriterResponseHandler implements BulkWriterResponseHandler {

  private Map<Tuple, Collection<String>> tupleMessageMap = new HashMap<>();
  private Map<Tuple, Set<Throwable>> tupleErrorMap = new HashMap<>();
  private OutputCollector collector;
  private MessageGetStrategy messageGetStrategy;

  public StormBulkWriterResponseHandler(OutputCollector collector, MessageGetStrategy messageGetStrategy) {
    this.collector = collector;
    this.messageGetStrategy = messageGetStrategy;
  }

  // Used only for unit testing
  protected Map<Tuple, Collection<String>> getTupleMessageMap() {
    return tupleMessageMap;
  }

  // Used only for unit testing
  protected Map<Tuple, Set<Throwable>> getTupleErrorMap() {
    return tupleErrorMap;
  }

  /**
   * Adds a tuple to be acked when all messages have been processed (either as a successful write or a failure).
   * @param tuple
   * @param messageIds
   */
  public void addTupleMessageIds(Tuple tuple, Collection<String> messageIds) {
    tupleMessageMap.put(tuple, messageIds);
  }

  @Override
  public void handleFlush(String sensorType, BulkWriterResponse response) {
    Collection<Tuple> tuplesToAck = new ArrayList<>();
    tupleMessageMap = tupleMessageMap.entrySet().stream()
            .map(entry -> {
              Tuple tuple = entry.getKey();
              Collection<String> ids = new ArrayList<>(entry.getValue());
              ids.removeAll(response.getSuccesses());
              response.getErrors().forEach((throwable, failedIds) -> {
                if (ids.removeAll(failedIds)) {
                  Set<Throwable> errorList = tupleErrorMap.getOrDefault(tuple, new HashSet<>());
                  tupleErrorMap.put(tuple, errorList);
                  errorList.add(throwable);
                  handleError(sensorType, throwable, tuple);
                }
              });
              return new AbstractMap.SimpleEntry<>(tuple, ids);
            })
            .filter(entry -> {
              if (entry.getValue().isEmpty()) {
                tuplesToAck.add(entry.getKey());
                return false;
              }
              return true;
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Set<Throwable> errorsToReport = new HashSet<>();
    tuplesToAck.forEach(tuple -> {
      collector.ack(tuple);
      if (tupleErrorMap.containsKey(tuple)) {
        errorsToReport.addAll(tupleErrorMap.remove(tuple));
      }

    });
    errorsToReport.forEach(throwable -> {
      // there is only one error to report for all of the failed tuples
      collector.reportError(throwable);
    });
  }

  public void handleError(String sensorType, Throwable e, Tuple tuple) {
    MetronError error = new MetronError()
            .withSensorType(Collections.singleton(sensorType))
            .withErrorType(Constants.ErrorType.INDEXING_ERROR)
            .withThrowable(e)
            .addRawMessage(messageGetStrategy.get(tuple));
    collector.emit(Constants.ERROR_STREAM, new Values(error.getJSONObject()));
  }
}
