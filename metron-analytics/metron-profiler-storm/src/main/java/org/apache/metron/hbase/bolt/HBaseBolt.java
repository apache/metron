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

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.bolt.mapper.HBaseMapper;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.hbase.client.HBaseTableClient;
import org.apache.metron.hbase.client.HBaseTableClientFactory;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

/**
 * A bolt that writes to HBase.
 *
 * Each bolt defined within a topology can interact with only a single HBase table.
 */
public class HBaseBolt extends BaseRichBolt {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Should the write-ahead-log be used.
   */
  private boolean writeToWAL = true;

  /**
   * The interval in seconds at which the writes are flushed to Hbase.
   */
  private int flushIntervalSecs = 1;

  /**
   * The batch size.
   */
  private int batchSize = 1000;

  /**
   * The name of the HBase table.  Each bolt communicates with a single HBase table.
   */
  protected String tableName;

  /**
   * The mapper which defines how tuple fields are mapped to HBase.
   */
  protected HBaseMapper mapper;

  /**
   * Defines when a batch needs flushed.
   */
  private BatchHelper batchHelper;

  /**
   * Establishes a connection to HBase.
   */
  private HBaseConnectionFactory connectionFactory;

  /**
   * Creates the {@link HBaseTableClient} used by this bolt.
   */
  private HBaseClientFactory hBaseClientFactory;

  /**
   * Used to write to HBase.
   */
  protected transient HBaseClient hbaseClient;

  protected OutputCollector collector;

  public HBaseBolt(String tableName, HBaseMapper mapper) {
    this.tableName = tableName;
    this.mapper = mapper;
    this.connectionFactory = new HBaseConnectionFactory();
    this.hBaseClientFactory = new HBaseTableClientFactory();
  }

  public HBaseBolt writeToWAL(boolean writeToWAL) {
    this.writeToWAL = writeToWAL;
    return this;
  }

  public HBaseBolt withBatchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  public HBaseBolt withFlushIntervalSecs(int flushIntervalSecs) {
    this.flushIntervalSecs = flushIntervalSecs;
    return this;
  }

  public HBaseBolt withConnectionFactory(HBaseConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
    return this;
  }

  public HBaseBolt withConnectionFactory(String connectionFactoryImpl) {
    this.connectionFactory = HBaseConnectionFactory.byName(connectionFactoryImpl);
    return this;
  }

  public HBaseBolt withClientFactory(HBaseClientFactory clientFactory) {
    this.hBaseClientFactory = clientFactory;
    return this;
  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    LOG.debug("Tick tuples expected every {} second(s)", flushIntervalSecs);
    Config conf = new Config();
    conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, flushIntervalSecs);
    return conf;
  }

  @Override
  public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
    this.collector = collector;
    this.batchHelper = new BatchHelper(batchSize, collector);
    this.hbaseClient = hBaseClientFactory.create(connectionFactory, HBaseConfiguration.create(), tableName);
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
    // nothing emitted
  }

  @Override
  public void execute(Tuple tuple) {
    LOG.trace("Received a tuple.");
    try {
      if (batchHelper.shouldHandle(tuple)) {
        save(tuple);
      }
      if (batchHelper.shouldFlush()) {
        flush();
      }

    } catch (Exception e) {
      batchHelper.fail(e);
      hbaseClient.clearMutations();
    }
  }

  /**
   * Saves an operation for later.
   * @param tuple Contains the data elements that need written to HBase.
   */
  private void save(Tuple tuple) {
    byte[] rowKey = mapper.rowKey(tuple);
    ColumnList cols = mapper.columns(tuple);
    Durability durability = writeToWAL ? Durability.SYNC_WAL : Durability.SKIP_WAL;
    Optional<Long> ttl = mapper.getTTL(tuple);
    if(ttl.isPresent()) {
      hbaseClient.addMutation(rowKey, cols, durability, ttl.get());
    } else {
      hbaseClient.addMutation(rowKey, cols, durability);
    }

    batchHelper.addBatch(tuple);
    LOG.debug("Added mutation to the batch; size={}", batchHelper.getBatchSize());
  }

  /**
   * Flush all saved operations.
   */
  private void flush() {
    LOG.debug("About to flush a batch of {} mutation(s)", batchHelper.getBatchSize());
    this.hbaseClient.mutate();
    batchHelper.ack();
  }
}
