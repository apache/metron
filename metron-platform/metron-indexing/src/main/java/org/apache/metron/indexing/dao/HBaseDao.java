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

package org.apache.metron.indexing.dao;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

public class HBaseDao implements IndexDao {
  private HTableInterface tableInterface;
  private byte[] cf;
  private AccessConfig config;
  public HBaseDao() {

  }

  @Override
  public synchronized SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return null;
  }

  @Override
  public synchronized void init(AccessConfig config) {
    if(this.config == null) {
      System.out.println("Initializing " + config.getTable());
      this.config = config;
      try {
        tableInterface = config.getTableProvider().getTable(HBaseConfiguration.create(), config.getTable());
        cf = config.getColumnFamily().getBytes();
        System.out.println("Initialized " + config.getTable());
      } catch (IOException e) {
        throw new IllegalStateException("Unable to initialize HBaseDao: " + e.getMessage(), e);
      }
    }
  }

  @Override
  public synchronized Document getLatest(String uuid, String sensorType) throws IOException {
    Get get = new Get(uuid.getBytes());
    get.addFamily(cf);
    Result result = tableInterface.get(get);
    NavigableMap<byte[], byte[]> columns = result.getFamilyMap( cf);
    if(columns == null || columns.size() == 0) {
      return null;
    }
    Map.Entry<byte[], byte[]> entry= columns.lastEntry();
    Long ts = Bytes.toLong(entry.getKey());
    if(entry.getValue()!= null) {
      String json = new String(entry.getValue());
      return new Document(json, uuid, sensorType, ts);
    }
    else {
      return null;
    }
  }

  @Override
  public synchronized void update(Document update, Optional<String> index) throws IOException {
    Put put = new Put(update.getUuid().getBytes());
    long ts = update.getTimestamp() == null?System.currentTimeMillis():update.getTimestamp();
    byte[] columnQualifier = Bytes.toBytes(ts);
    byte[] doc = JSONUtils.INSTANCE.toJSON(update.getDocument());
    put.addColumn(cf, columnQualifier, doc);
    tableInterface.put(put);
  }
}
