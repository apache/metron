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

import org.apache.metron.hbase.TableProvider;
import org.apache.storm.Constants;
import org.apache.storm.tuple.Tuple;
import org.apache.metron.hbase.bolt.mapper.Widget;
import org.apache.metron.hbase.bolt.mapper.WidgetMapper;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the HBaseBolt.
 */
public class HBaseBoltTest extends BaseBoltTest {

  private static final String tableName = "widgets";
  private HBaseClient client;
  private Tuple tuple1;
  private Tuple tuple2;
  private Widget widget1;
  private Widget widget2;
  private TableProvider provider;

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
    tuple1 = mock(Tuple.class);
    tuple2 = mock(Tuple.class);
    client = mock(HBaseClient.class);
    provider = mock(TableProvider.class);
  }

  /**
   * Create a ProfileBuilderBolt to test
   */
  private HBaseBolt createBolt(int batchSize, WidgetMapper mapper) throws IOException {
    HBaseBolt bolt = new HBaseBolt(tableName, mapper)
            .withBatchSize(batchSize).withTableProviderInstance(provider);
    bolt.prepare(Collections.emptyMap(), topologyContext, outputCollector);
    bolt.setClient(client);
    return bolt;
  }

  /**
   * What happens if the batch is ready to flush?
   *
   * If the batch size is 2 and we have received 2 tuples the batch should be flushed.
   */
  @Test
  public void testBatchReady() throws Exception {
    HBaseBolt bolt = createBolt(2, new WidgetMapper());
    bolt.execute(tuple1);
    bolt.execute(tuple2);

    // batch size is 2, received 2 tuples - flush the batch
    verify(client, times(2)).addMutation(any(), any(), any());
    verify(client, times(1)).mutate();
  }

  /**
   * If the batch size is NOT reached, the batch should NOT be flushed.
   */
  @Test
  public void testBatchNotReady() throws Exception {
    HBaseBolt bolt = createBolt(2, new WidgetMapper());
    bolt.execute(tuple1);

    // 1 put was added to the batch, but nothing was flushed
    verify(client, times(1)).addMutation(any(), any(), any());
    verify(client, times(0)).mutate();
  }

  /**
   * What happens if the batch timeout is reached?
   */
  @Test
  public void testTimeFlush() throws Exception {
    HBaseBolt bolt = createBolt(2, new WidgetMapper());

    // the batch is not ready to write
    bolt.execute(tuple1);
    verify(client, times(1)).addMutation(any(), any(), any());
    verify(client, times(0)).mutate();

    // the batch should be flushed after the tick tuple
    bolt.execute(mockTickTuple());
    verify(client, times(1)).mutate();
  }

  /**
   * The mapper can define a TTL that the HBaseBolt uses to determine
   * if the Put to Hbase needs the TTL set.
   */
  @Test
  public void testWriteWithTTL() throws Exception {

    // setup - create a mapper with a TTL set
    final Long expectedTTL = 2000L;
    WidgetMapper mapperWithTTL = new WidgetMapper(expectedTTL);

    // execute
    HBaseBolt bolt = createBolt(2, mapperWithTTL);
    bolt.execute(tuple1);
    bolt.execute(tuple2);

    // used to grab the actual TTL
    ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);

    // validate - ensure the Puts written with the TTL
    verify(client, times(2)).addMutation(any(), any(), any(), ttlCaptor.capture());
    Assert.assertEquals(expectedTTL, ttlCaptor.getValue());
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
