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

import backtype.storm.Constants;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.Widget;
import org.apache.metron.hbase.WidgetMapper;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests the HBaseBolt.
 */
public class HBaseBoltTest extends BaseBoltTest {

  private static final String tableName = "widgets";

  @Mock
  private HBaseClient client;

  @Mock
  private Tuple tuple1;

  @Mock
  private Tuple tuple2;

  private Widget widget1;

  private Widget widget2;

  private HBaseMapper mapper;


  @Before
  public void setupTuples() throws Exception {

    // setup the first tuple
    widget1 = new Widget("widget1", 100);
    when(tuple1.getValueByField(eq("widget"))).thenReturn(widget1);

    // setup the second tuple
    widget2 = new Widget("widget2", 200);
    when(tuple2.getValueByField(eq("widget"))).thenReturn(widget2);
  }

  @Before
  public void setup() throws Exception {

    // create a mapper
    mapper = new WidgetMapper();

  }

  /**
   * Create a ProfileBuilderBolt to test
   */
  private HBaseBolt createBolt() throws IOException {
    HBaseBolt bolt = new HBaseBolt(tableName, mapper)
            .withBatchSize(2);

    bolt.prepare(Collections.emptyMap(), topologyContext, outputCollector);
    bolt.setClient(client);
    return bolt;
  }

  /**
   * What happens if the batch is full?
   *
   * If the batch size is 2 and we have received 2 tuples the batch should be flushed.
   */
  @Test
  public void testBatchReady() throws Exception {
    HBaseBolt bolt = createBolt();
    bolt.execute(tuple1);
    bolt.execute(tuple2);
    verify(client, times(1)).batchMutate(any(List.class));
  }

  /**
   * What happens if the batch is not full?
   *
   * If the batch size is 2 and we have only received 2 tuple, the batch should not be flushed.
   */
  @Test
  public void testBatchNotReady() throws Exception {
    HBaseBolt bolt = createBolt();
    bolt.execute(tuple1);
    verify(client, times(0)).batchMutate(any(List.class));
  }

  /**
   * What happens if the batch timeout is reached?
   */
  @Test
  public void testTimeFlush() throws Exception {
    HBaseBolt bolt = createBolt();

    // the batch is not ready to write
    bolt.execute(tuple1);
    verify(client, times(0)).batchMutate(any(List.class));

    // the batch should be written after the tick tuple
    bolt.execute(mockTickTuple());
    verify(client, times(1)).batchMutate(any(List.class));
  }

  private static Tuple mockTuple(String componentId, String streamId) {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getSourceComponent()).thenReturn(componentId);
    when(tuple.getSourceStreamId()).thenReturn(streamId);
    return tuple;
  }

  private static Tuple mockTickTuple() {
    return mockTuple(Constants.SYSTEM_COMPONENT_ID, Constants.SYSTEM_TICK_STREAM_ID);
  }
}
