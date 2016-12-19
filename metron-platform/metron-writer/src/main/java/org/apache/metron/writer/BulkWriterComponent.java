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

import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import com.google.common.collect.Iterables;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BulkWriterComponent<MESSAGE_T> {
  public static final Logger LOG = LoggerFactory
            .getLogger(BulkWriterComponent.class);
  private Map<String, Collection<Tuple>> sensorTupleMap = new HashMap<>();
  private Map<String, List<MESSAGE_T>> sensorMessageMap = new HashMap<>();
  private OutputCollector collector;
  private boolean handleCommit = true;
  private boolean handleError = true;
  public BulkWriterComponent(OutputCollector collector) {
    this.collector = collector;
  }

  public BulkWriterComponent(OutputCollector collector, boolean handleCommit, boolean handleError) {
    this(collector);
    this.handleCommit = handleCommit;
    this.handleError = handleError;
  }

  public void commit(Iterable<Tuple> tuples) {
    tuples.forEach(t -> collector.ack(t));
    if(LOG.isDebugEnabled()) {
      LOG.debug("Acking " + Iterables.size(tuples) + " tuples");
    }
  }

  public void commit(BulkWriterResponse response) {
    commit(response.getSuccesses());
  }

  public void error(Throwable e, Iterable<Tuple> tuples) {
    tuples.forEach(t -> collector.ack(t));
    if(!Iterables.isEmpty(tuples)) {
      LOG.error("Failing " + Iterables.size(tuples) + " tuples", e);
      ErrorUtils.handleError(collector, e, Constants.ERROR_STREAM);
    }
  }

  public void error(BulkWriterResponse errors) {
    Map<Throwable, Collection<Tuple>> errorMap = errors.getErrors();
    for(Map.Entry<Throwable, Collection<Tuple>> entry : errorMap.entrySet()) {
      error(entry.getKey(), entry.getValue());
    }
  }

  protected Collection<Tuple> createTupleCollection() {
    return new ArrayList<>();
  }


  public void errorAll(Throwable e) {
    for(Map.Entry<String, Collection<Tuple>> kv : sensorTupleMap.entrySet()) {
      error(e, kv.getValue());
      sensorTupleMap.remove(kv.getKey());
      sensorMessageMap.remove(kv.getKey());
    }
  }

  public void errorAll(String sensorType, Throwable e) {
    error(e, Optional.ofNullable(sensorTupleMap.get(sensorType)).orElse(new ArrayList<>()));
    sensorTupleMap.remove(sensorType);
    sensorMessageMap.remove(sensorType);
  }
  public void write( String sensorType
                   , Tuple tuple
                   , MESSAGE_T message
                   , BulkMessageWriter<MESSAGE_T> bulkMessageWriter
                   , WriterConfiguration configurations
                   ) throws Exception
  {
    int batchSize = configurations.getBatchSize(sensorType);
    Collection<Tuple> tupleList = sensorTupleMap.get(sensorType);
    if (tupleList == null) {
      tupleList = createTupleCollection();
    }
    tupleList.add(tuple);
    List<MESSAGE_T> messageList = sensorMessageMap.get(sensorType);
    if (messageList == null) {
      messageList = new ArrayList<>();
    }
    messageList.add(message);

    if (tupleList.size() < batchSize) {
      sensorTupleMap.put(sensorType, tupleList);
      sensorMessageMap.put(sensorType, messageList);
    } else {
      long startTime = System.nanoTime();
      try {
        BulkWriterResponse response = bulkMessageWriter.write(sensorType, configurations, tupleList, messageList);

        // Commit or error piecemeal.
        if(handleCommit) {
          commit(response);
        }

        if(handleError) {
          error(response);
        } else if (response.hasErrors()) {
          throw new IllegalStateException("Unhandled bulk errors in response: " + response.getErrors());
        }
      } catch (Throwable e) {
        if(handleError) {
          error(e, tupleList);
        }
        else {
          throw e;
        }
      }
      finally {
        sensorTupleMap.remove(sensorType);
        sensorMessageMap.remove(sensorType);
      }
      long endTime = System.nanoTime();
      long elapsed = endTime - startTime;
      LOG.debug("Bulk batch completed in ~" + elapsed + " ns");
    }
  }
}
