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
package org.apache.metron.hbase.writer;

import backtype.storm.tuple.Tuple;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.common.interfaces.MessageWriter;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public abstract class HBaseWriter implements MessageWriter<JSONObject>, Serializable {

  private String tableName;
  private String connectorImpl;
  private TableProvider provider;
  private HTableInterface table;

  public HBaseWriter(String tableName) {
    this.tableName = tableName;
  }

  public HBaseWriter withProviderImpl(String connectorImpl) {
    this.connectorImpl = connectorImpl;
    return this;
  }

  @Override
  public void init() {
    final Configuration config = HBaseConfiguration.create();
    try {
      provider = ReflectionUtils.createInstance(connectorImpl, new HTableProvider());
      table = provider.getTable(config, tableName);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void write(String sourceType, Configurations configurations, Tuple tuple, JSONObject message) throws Exception {
    Put put = new Put(getKey(tuple, message));
    Map<String, byte[]> values = getValues(tuple, message);
    for(String column: values.keySet()) {
      String[] columnParts = column.split(":");
      long timestamp = getTimestamp(tuple, message);
      if (timestamp > -1) {
        put.addColumn(Bytes.toBytes(columnParts[0]), Bytes.toBytes(columnParts[1]), timestamp, values.get(column));
      } else {
        put.addColumn(Bytes.toBytes(columnParts[0]), Bytes.toBytes(columnParts[1]), values.get(column));
      }
    }
    table.put(put);
  }

  @Override
  public void close() throws Exception {
    table.close();
  }

  public abstract byte[] getKey(Tuple tuple, JSONObject message);
  public abstract long getTimestamp(Tuple tuple, JSONObject message);
  public abstract Map<String, byte[]> getValues(Tuple tuple, JSONObject message);
}
