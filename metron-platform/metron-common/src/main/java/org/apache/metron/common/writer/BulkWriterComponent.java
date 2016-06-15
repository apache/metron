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

package org.apache.metron.common.writer;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import com.google.common.collect.Iterables;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
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
  private Long currentTime;
  private Long flushIntervalInMs;
  private boolean flush;

  public BulkWriterComponent(OutputCollector collector) {
    this.collector = collector;
    this.currentTime = System.currentTimeMillis();
    this.flush = false;
  }

  public BulkWriterComponent(OutputCollector collector, boolean handleCommit, boolean handleError) {
    this(collector);
    this.handleCommit = handleCommit;
    this.handleError = handleError;
    this.currentTime = System.currentTimeMillis();
    this.flush = false;
  }

  public void setFlush(boolean flush) {
    this.flush = flush;
  }

  public void setFlushIntervalInMs(Long flushIntervalInMs) {
    this.flushIntervalInMs = flushIntervalInMs;
  }

  public void commit(Iterable<Tuple> tuples) {
    tuples.forEach(t -> collector.ack(t));
    if(LOG.isDebugEnabled()) {
      LOG.debug("Acking " + Iterables.size(tuples) + " tuples");
    }
  }

  public void error(Throwable e, Iterable<Tuple> tuples) {
    tuples.forEach(t -> collector.ack(t));
    LOG.error("Failing " + Iterables.size(tuples) + " tuples", e);
    ErrorUtils.handleError(collector, e, Constants.ERROR_STREAM);
  }

  protected Collection<Tuple> createTupleCollection() {
    return new ArrayList<>();
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
    if ((flush && (System.currentTimeMillis() >= currentTime + flushIntervalInMs))) {
      flushAllSensorTypes(bulkMessageWriter, configurations);
    } else if ((tupleList.size() >= batchSize)) {
        flush(sensorType, bulkMessageWriter, configurations, tupleList, messageList);
    } else {
      sensorTupleMap.put(sensorType, tupleList);
      sensorMessageMap.put(sensorType, messageList);
    }
  }

  private void flushAllSensorTypes (BulkMessageWriter<MESSAGE_T> bulkMessageWriter, WriterConfiguration configurations) throws Exception {
      for (Map.Entry<String, Collection<Tuple>> tupleEntry : sensorTupleMap.entrySet()) {
        try {
          flush(tupleEntry.getKey(), bulkMessageWriter, configurations, tupleEntry.getValue(), sensorMessageMap.get(tupleEntry.getKey()));
        } catch (Exception e) {
          LOG.warn("Exception thrown while flushing senson type " + tupleEntry.getKey(), e);
          LOG.warn("Continuing with next sensor type");
        }
      }
      if (flush) {
        currentTime = System.currentTimeMillis();
      }
  }

  private void flush(String sensorType, BulkMessageWriter<MESSAGE_T> bulkMessageWriter, WriterConfiguration configurations, Collection<Tuple> tupleList,
                     List<MESSAGE_T> messageList ) throws Exception {
      try {
        bulkMessageWriter.write(sensorType, configurations, tupleList, messageList);
        if(handleCommit) {
          commit(tupleList);
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
  }
}
