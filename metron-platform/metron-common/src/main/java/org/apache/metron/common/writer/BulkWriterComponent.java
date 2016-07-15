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
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.writer.EnrichmentWriterConfiguration;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.BulkMessageWriter;
import org.apache.metron.common.utils.ErrorUtils;
import org.apache.metron.common.utils.MessageUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

public class BulkWriterComponent<MESSAGE_T> {
  public static final Logger LOG = LoggerFactory.getLogger(BulkWriterComponent.class);
  private Map<String, Collection<Tuple>> sensorTupleMap = new HashMap<>();
  private Map<String, List<MESSAGE_T>> sensorMessageMap = new HashMap<>();
  private OutputCollector collector;
  private boolean handleCommit = true;
  private boolean handleError = true;
  private Long lastFlushTime;
  private Long flushIntervalInMs;
  private boolean flush = false;
  private int currentBatchSize=0;

  public BulkWriterComponent(OutputCollector collector) {
    this.collector = collector;
    this.lastFlushTime = System.currentTimeMillis();
    this.flush = false;
  }

  public BulkWriterComponent(OutputCollector collector, boolean handleCommit, boolean handleError) {
    this(collector);
    this.handleCommit = handleCommit;
    this.handleError = handleError;
    this.lastFlushTime = System.currentTimeMillis();
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
    if(!Iterables.isEmpty(tuples)) {
      LOG.error("Failing " + Iterables.size(tuples) + " tuples", e);
      ErrorUtils.handleError(collector, e, Constants.ERROR_STREAM);
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

  public void write( String sensorType, Tuple tuple, MESSAGE_T message, BulkMessageWriter<MESSAGE_T> bulkMessageWriter, WriterConfiguration configurations) throws Exception
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

    if(configurations.getGlobalConfig()!=null&&configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG)!=null)
    {
      this.setFlush(Boolean.parseBoolean(configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG).toString()));
      if (configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS) != null)
      {
        this.setFlushIntervalInMs(Long.parseLong(configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS).toString()));
        LOG.trace("Setting time based flushing  to " +configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG)+" with timeout of"+ configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS).toString());
      }
    }

    sensorTupleMap.put(sensorType, tupleList);
    sensorMessageMap.put(sensorType, messageList);

    if(tupleList.size() >= batchSize || (flush && (System.currentTimeMillis() >= (lastFlushTime + flushIntervalInMs))))
    {
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
        if (flush) {
          lastFlushTime = System.currentTimeMillis();
        }
      }
    }
  }

  public void writeGlobalBatch(String sensorType,Tuple tuple, BulkMessageWriter<MESSAGE_T> bulkMessageWriter, EnrichmentWriterConfiguration configurations) throws Exception {
    {
      int batchSize = Integer.parseInt(configurations.getGlobalConfig().get(Constants.GLOBAL_BATCH_SIZE).toString());
      Collection<Tuple> tupleList = sensorTupleMap.get(sensorType);
      if (tupleList == null) {
        tupleList = createTupleCollection();
      }
      tupleList.add(tuple);

      if(configurations.getGlobalConfig()!=null&&configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG)!=null)
      {
        this.setFlush(Boolean.parseBoolean(configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG).toString()));
        if (configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS) != null)
        {
          this.setFlushIntervalInMs(Long.parseLong(configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS).toString()));
          LOG.trace("Setting time based flushing  to " +configurations.getGlobalConfig().get(Constants.TIME_FLUSH_FLAG)+" with timeout of"+ configurations.getGlobalConfig().get(Constants.FLUSH_INTERVAL_IN_MS).toString());
        }
      }

      sensorTupleMap.put(sensorType, tupleList);
      currentBatchSize++;

      if(currentBatchSize >= batchSize || (flush && (System.currentTimeMillis() >= (lastFlushTime + flushIntervalInMs))))
      {
        try {
          bulkMessageWriter.writeGlobalBatch(sensorTupleMap, configurations,collector);
          if(handleCommit) {
            for(Collection tupList:sensorTupleMap.values()){
              commit(tupList);
            }
          }

        } catch (Throwable e) {
          if(handleError) {
            for(Collection tupList:sensorTupleMap.values()){
              error(e, tupList);
            }
          }
          else {
            throw e;
          }
        }
        finally {
          sensorTupleMap.clear();
          currentBatchSize=0;
          if (flush) {
            lastFlushTime = System.currentTimeMillis();
          }
        }
      }
    }
  }
}