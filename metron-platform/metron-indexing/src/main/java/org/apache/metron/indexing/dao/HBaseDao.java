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

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;

/**
 * The HBaseDao is an index dao which only supports the following actions:
 * * Update
 * * Get document
 *
 * The mechanism here is that updates to documents will be added to a HBase Table as a write-ahead log.
 * The Key for a row supporting a given document will be the GUID, which should be sufficiently distributed.
 * Every new update will have a column added (column qualifier will be the timestamp of the update).
 * Upon retrieval, the most recent column will be returned.
 *
 */
public class HBaseDao implements IndexDao {
  public static final String SOURCE_TYPE = Constants.SENSOR_TYPE.replace('.', ':');
  public static String HBASE_TABLE = "update.hbase.table";
  public static String HBASE_CF = "update.hbase.cf";
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
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return null;
  }

  @Override
  public synchronized void init(AccessConfig config) {
    if(this.tableInterface == null) {
      this.config = config;
      Map<String, Object> globalConfig = config.getGlobalConfigSupplier().get();
      if(globalConfig == null) {
        throw new IllegalStateException("Cannot find the global config.");
      }
      String table = (String)globalConfig.get(HBASE_TABLE);
      String cf = (String) config.getGlobalConfigSupplier().get().get(HBASE_CF);
      if(table == null || cf == null) {
        throw new IllegalStateException("You must configure " + HBASE_TABLE + " and " + HBASE_CF + " in the global config.");
      }
      try {
        tableInterface = config.getTableProvider().getTable(HBaseConfiguration.create(), table);
        this.cf = cf.getBytes();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to initialize HBaseDao: " + e.getMessage(), e);
      }
    }
  }

  public HTableInterface getTableInterface() {
    if(tableInterface == null) {
      init(config);
    }
    return tableInterface;
  }

  @Override
  public synchronized Document getLatest(String guid, String sensorType) throws IOException {
    Get get = new Get(buildRowKey(guid, sensorType));
    get.addFamily(cf);
    Result result = getTableInterface().get(get);
    return getDocumentFromResult(result);
  }

  @Override
  public Iterable<Document> getAllLatest(
      List<GetRequest> getRequests) throws IOException {
    List<Get> gets = new ArrayList<>();
    for (GetRequest getRequest: getRequests) {
      gets.add(buildGet(getRequest));
    }
    Result[] results = getTableInterface().get(gets);
    List<Document> allLatest = new ArrayList<>();
    for (Result result: results) {
      Document d = getDocumentFromResult(result);
      if (d != null) {
        allLatest.add(d);
      }
    }
    return allLatest;
  }

  private Document getDocumentFromResult(Result result) throws IOException {
    NavigableMap<byte[], byte[]> columns = result.getFamilyMap( cf);
    if(columns == null || columns.size() == 0) {
      return null;
    }
    Map.Entry<byte[], byte[]> entry= columns.lastEntry();
    Long ts = Bytes.toLong(entry.getKey());
    if(entry.getValue()!= null) {
      Map<String, Object> json = JSONUtils.INSTANCE.load(new String(entry.getValue()),
          new TypeReference<Map<String, Object>>() {});
      ByteArrayInputStream baos = new ByteArrayInputStream(result.getRow());
      DataInputStream w = new DataInputStream(baos);
      try {
        String guid = w.readUTF();
        String sensorType = w.readUTF();
        return new Document(json, guid, sensorType, ts);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert row key to a document", e);
      }
    }
    else {
      return null;
    }
  }

  @Override
  public synchronized void update(Document update, Optional<String> index) throws IOException {
    Put put = buildPut(update);
    getTableInterface().put(put);
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    List<Put> puts = new ArrayList<>();
    for (Map.Entry<Document, Optional<String>> updateEntry : updates.entrySet()) {
      Document update = updateEntry.getKey();

      Put put = buildPut(update);
      puts.add(put);
    }
    getTableInterface().put(puts);
  }

  protected Get buildGet(GetRequest getRequest) throws IOException {
    Get get = new Get(buildRowKey(getRequest.getGuid(), getRequest.getSensorType()));
    get.addFamily(cf);
    return get;
  }

  protected Put buildPut(Document update) throws IOException {
    Put put = new Put(buildRowKey(update.getGuid(), update.getSensorType()));
    long ts = update.getTimestamp() == null ? System.currentTimeMillis() : update.getTimestamp();
    byte[] columnQualifier = Bytes.toBytes(ts);
    byte[] doc = JSONUtils.INSTANCE.toJSONPretty(update.getDocument());
    put.addColumn(cf, columnQualifier, doc);
    return put;
  }

  public static byte[] buildRowKey(String guid, String sensorType) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream w = new DataOutputStream(baos);
    w.writeUTF(guid);
    w.writeUTF(sensorType);
    w.flush();
    return baos.toByteArray();
  }

  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws IOException {
    return null;
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException {
    return null;
  }
}
