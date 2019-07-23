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
package org.apache.metron.hbase.coprocessor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.client.HBaseTableClient;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.MockHBaseConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link HBaseCacheWriter}.
 */
public class HBaseCacheWriterTest {

  private final String tableName = "table";
  private final String columnFamily = "colFamily";
  private final String columnQualifier = "colQualifier";

  private MockHBaseConnectionFactory connectionFactory;
  private HBaseCacheWriter cacheWriter;
  private Configuration conf;

  @Before
  public void setup() {
    connectionFactory = new MockHBaseConnectionFactory();
    conf = HBaseConfiguration.create();
  }

  @Test
  public void test() {
    HBaseClient client = mock(HBaseTableClient.class);

    // the creator needs to return the mock HBaseClient
    HBaseClientFactory creator = mock(HBaseClientFactory.class);
    when(creator.create(any(), any(), any())).thenReturn(client);

    cacheWriter = new HBaseCacheWriter(creator, connectionFactory, conf, tableName, columnFamily, columnQualifier);
    cacheWriter.write("key1", "value1");
    cacheWriter.write("key2", "value2");
    cacheWriter.write("key3", "value3");

    // nothing cached by key0, expect key1, key2, key3
    verify(client, times(0)).addMutation(eq(Bytes.toBytes("key0")), any());
    verify(client, times(1)).addMutation(eq(Bytes.toBytes("key1")), any());
    verify(client, times(1)).addMutation(eq(Bytes.toBytes("key2")), any());
    verify(client, times(1)).addMutation(eq(Bytes.toBytes("key3")), any());
  }
}
