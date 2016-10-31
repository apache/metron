/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.hbase.bolt;

import org.apache.storm.Constants;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides functionality for handling batch writes to HBase.
 */
public class BatchHelper {

  private static final Logger LOG = LoggerFactory.getLogger(BatchHelper.class);

  /**
   * The batch size.  Defaults to 15,000.
   */
  private int batchSize = 15000;

  /**
   * A batch of tuples.
   */
  private List<Tuple> tupleBatch;

  private boolean forceFlush = false;
  private OutputCollector collector;

  /**
   * @param batchSize The batch size.
   * @param collector The output collector.
   */
  public BatchHelper(int batchSize, OutputCollector collector) {
    if (batchSize > 0) {
      this.batchSize = batchSize;
    }
    this.collector = collector;
    this.tupleBatch = new LinkedList<>();
  }

  /**
   * Fail the batch.
   * @param e The exception which caused the failure.
   */
  public void fail(Exception e) {
    collector.reportError(e);
    for (Tuple t : tupleBatch) {
      collector.fail(t);
    }
    tupleBatch.clear();
    forceFlush = false;
  }

  /**
   * Ack the batch.
   */
  public void ack() {
    for (Tuple t : tupleBatch) {
      collector.ack(t);
    }
    tupleBatch.clear();
    forceFlush = false;
  }

  public boolean shouldHandle(Tuple tuple) {
    if (isTick(tuple)) {
      LOG.debug("TICK received! current batch status [{}/{}]", tupleBatch.size(), batchSize);
      forceFlush = true;
      return false;
    } else {
      return true;
    }
  }

  /**
   * Adds a tuple to the batch.
   * @param tuple
   */
  public void addBatch(Tuple tuple) {
    tupleBatch.add(tuple);
    if (tupleBatch.size() >= batchSize) {
      forceFlush = true;
    }
  }

  public List<Tuple> getBatchTuples() {
    return this.tupleBatch;
  }

  public int getBatchSize() {
    return this.batchSize;
  }

  public boolean shouldFlush() {
    return forceFlush && !tupleBatch.isEmpty();
  }

  public boolean isTick(Tuple tuple) {
    return tuple != null &&
            Constants.SYSTEM_COMPONENT_ID.equals(tuple.getSourceComponent()) &&
            Constants.SYSTEM_TICK_STREAM_ID.equals(tuple.getSourceStreamId());
  }

}
