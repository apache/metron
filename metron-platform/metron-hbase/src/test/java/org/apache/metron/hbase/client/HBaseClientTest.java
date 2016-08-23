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

package org.apache.metron.hbase.client;

import backtype.storm.tuple.Tuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.Widget;
import org.apache.metron.hbase.WidgetMapper;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.bolt.mapper.HBaseProjectionCriteria;
import org.apache.storm.hbase.common.ColumnList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the HBaseClient
 */
public class HBaseClientTest {

  private static final String tableName = "widgets";

  private static HBaseTestingUtility util;
  private HBaseClient client;
  private HTableInterface table;
  private Tuple tuple1;
  private Tuple tuple2;
  private Widget widget1;
  private Widget widget2;
  private HBaseMapper mapper;

  @BeforeClass
  public static void startHBase() throws Exception {
    Configuration config = HBaseConfiguration.create();
    config.set("hbase.master.hostname", "localhost");
    config.set("hbase.regionserver.hostname", "localhost");
    util = new HBaseTestingUtility(config);
    util.startMiniCluster();
  }

  @AfterClass
  public static void stopHBase() throws Exception {
    util.shutdownMiniCluster();
    util.cleanupTestDir();
  }

  @Before
  public void setupTuples() throws Exception {

    // setup the first tuple
    widget1 = new Widget("widget1", 100);
    tuple1 = mock(Tuple.class);
    when(tuple1.getValueByField(eq("widget"))).thenReturn(widget1);

    // setup the second tuple
    widget2 = new Widget("widget2", 200);
    tuple2 = mock(Tuple.class);
    when(tuple2.getValueByField(eq("widget"))).thenReturn(widget2);
  }

  @Before
  public void setup() throws Exception {

    // create a mapper
    mapper = new WidgetMapper();

    // create the table
    table = util.createTable(Bytes.toBytes(tableName), WidgetMapper.CF);

    // setup the client
    client = new HBaseClient((c,t) -> table, table.getConfiguration(), tableName);
  }

  @After
  public void tearDown() throws Exception {
    util.deleteTable(tableName);
  }

  @Test
  public void testWrite() throws Exception {

    // add a tuple to the batch
    byte[] rowKey1 = mapper.rowKey(tuple1);
    ColumnList cols1 = mapper.columns(tuple1);
    List<Mutation> mutations1 = client.constructMutationReq(rowKey1, cols1, Durability.SYNC_WAL);
    client.batchMutate(mutations1);

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(WidgetMapper.CF_STRING);

    // read back the tuple
    Get get1 = client.constructGetRequests(rowKey1, criteria);
    Result[] results = client.batchGet(Arrays.asList(get1));
    Assert.assertEquals(1, results.length);
  }

  @Test
  public void testBatchWrite() throws Exception {

    // add a tuple to the batch
    byte[] rowKey1 = mapper.rowKey(tuple1);
    ColumnList cols1 = mapper.columns(tuple1);
    List<Mutation> mutations1 = client.constructMutationReq(rowKey1, cols1, Durability.SYNC_WAL);
    client.batchMutate(mutations1);

    // add another tuple to the batch
    byte[] rowKey2 = mapper.rowKey(tuple1);
    ColumnList cols2 = mapper.columns(tuple1);
    List<Mutation> mutations2 = client.constructMutationReq(rowKey2, cols2, Durability.SYNC_WAL);
    client.batchMutate(mutations2);

    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria();
    criteria.addColumnFamily(WidgetMapper.CF_STRING);

    // read back both tuples
    Get get1 = client.constructGetRequests(rowKey1, criteria);
    Get get2 = client.constructGetRequests(rowKey2, criteria);
    Result[] results = client.batchGet(Arrays.asList(get1, get2));
    Assert.assertEquals(2, results.length);
  }
}
