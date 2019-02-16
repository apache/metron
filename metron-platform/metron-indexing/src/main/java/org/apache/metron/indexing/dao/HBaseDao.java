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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.utils.KeyUtil;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;

/**
 * The HBaseDao is an index dao which only supports the following actions:
 * * Update
 * * Get document
 *
 * The mechanism here is that updates to documents will be added to a HBase Table as a write-ahead log.
 * The Key for a row supporting a given document will be the GUID plus the sensor type, which should be sufficiently distributed.
 * Every new update will have a column added (column qualifier will be the timestamp of the update).
 * Upon retrieval, the most recent column will be returned.
 *
 */
public class HBaseDao implements IndexDao {
  public static String HBASE_TABLE = "update.hbase.table";
  public static String HBASE_CF = "update.hbase.cf";
  private HTableInterface tableInterface;
  private byte[] cf;
  private AccessConfig config;

  /**
   * Implements the HBaseDao row key and exposes convenience methods for serializing/deserializing the row key.
   * The row key is made of a GUID and sensor type along with a prefix to ensure data is distributed evenly.
   */
  public static class Key {
    private String guid;
    private String sensorType;
    public Key(String guid, String sensorType) {
      this.guid = guid;
      this.sensorType = sensorType;
    }

    public String getGuid() {
      return guid;
    }

    public String getSensorType() {
      return sensorType;
    }

    public static Key fromBytes(byte[] buffer) throws IOException {
      ByteArrayInputStream baos = new ByteArrayInputStream(buffer);
      DataInputStream w = new DataInputStream(baos);
      baos.skip(KeyUtil.HASH_PREFIX_SIZE);
      return new Key(w.readUTF(), w.readUTF());
    }

    public byte[] toBytes() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if(getGuid() == null || getSensorType() == null) {
        throw new IllegalStateException("Guid and sensor type must not be null: guid = " + getGuid() + ", sensorType = " + getSensorType());
      }
      DataOutputStream w = new DataOutputStream(baos);
      w.writeUTF(getGuid());
      w.writeUTF(getSensorType());
      w.flush();
      byte[] key = baos.toByteArray();
      byte[] prefix = KeyUtil.INSTANCE.getPrefix(key);
      return KeyUtil.INSTANCE.merge(prefix, key);
    }

    public static byte[] toBytes(Key k) throws IOException {
      return k.toBytes();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key) o;

      if (getGuid() != null ? !getGuid().equals(key.getGuid()) : key.getGuid() != null) return false;
      return getSensorType() != null ? getSensorType().equals(key.getSensorType()) : key.getSensorType() == null;

    }

    @Override
    public int hashCode() {
      int result = getGuid() != null ? getGuid().hashCode() : 0;
      result = 31 * result + (getSensorType() != null ? getSensorType().hashCode() : 0);
      return result;
    }
  }

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
        this.cf = cf.getBytes(StandardCharsets.UTF_8);
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
    Key k = new Key(guid, sensorType);
    Get get = new Get(Key.toBytes(k));
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
      Map<String, Object> json = JSONUtils.INSTANCE.load(new String(entry.getValue(),
              StandardCharsets.UTF_8),
          JSONUtils.MAP_SUPPLIER);

      // Make sure comments are in the proper format
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> commentsMap = (List<Map<String, Object>>) json.get(COMMENTS_FIELD);
      try {
        if (commentsMap != null) {
          List<AlertComment> comments = new ArrayList<>();
          for (Map<String, Object> commentMap : commentsMap) {
            comments.add(new AlertComment(commentMap));
          }
          if (comments.size() > 0) {
            json.put(COMMENTS_FIELD,
                comments.stream().map(AlertComment::asMap).collect(Collectors.toList()));
          }
        }
        Key k = Key.fromBytes(result.getRow());
        return new Document(json, k.getGuid(), k.getSensorType(), ts);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert row key to a document", e);
      }
    }
    else {
      return null;
    }
  }

  @Override
  public synchronized Document update(Document update, Optional<String> index) throws IOException {
    Put put = buildPut(update);
    getTableInterface().put(put);
    return update;
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    List<Put> puts = new ArrayList<>();
    for (Map.Entry<Document, Optional<String>> updateEntry : updates.entrySet()) {
      Document update = updateEntry.getKey();

      Put put = buildPut(update);
      puts.add(put);
    }
    getTableInterface().put(puts);
    return updates;
  }

  protected Get buildGet(GetRequest getRequest) throws IOException {
    Key k = new Key(getRequest.getGuid(), getRequest.getSensorType());
    Get get = new Get(Key.toBytes(k));
    get.addFamily(cf);
    return get;
  }

  protected Put buildPut(Document update) throws IOException {
    Key k = new Key(update.getGuid(), update.getSensorType());
    Put put = new Put(Key.toBytes(k));
    long ts = update.getTimestamp() == null || update.getTimestamp() == 0 ? System.currentTimeMillis() : update.getTimestamp();
    byte[] columnQualifier = Bytes.toBytes(ts);
    byte[] doc = JSONUtils.INSTANCE.toJSONPretty(update.getDocument());
    put.addColumn(cf, columnQualifier, doc);
    return put;
  }


  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = getLatest(request.getGuid(), request.getSensorType());
    return addCommentToAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null || latest.getDocument() == null) {
      throw new IOException(String.format("Unable to add comment. Document with guid %s cannot be found.",
              request.getGuid()));
    }

    List<Map<String, Object>> comments = (List<Map<String, Object>>) latest.getDocument()
        .getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    List<Map<String, Object>> originalComments = new ArrayList<>(comments);

    // Convert all comments back to raw JSON before updating.
    List<Map<String, Object>> commentsMap = new ArrayList<>();
    for (Map<String, Object> comment : originalComments) {
      commentsMap.add(new AlertComment(comment).asMap());
    }
    commentsMap.add(new AlertComment(
        request.getComment(),
        request.getUsername(),
        request.getTimestamp())
        .asMap());

    Document newVersion = new Document(latest);
    newVersion.getDocument().put(COMMENTS_FIELD, commentsMap);
    return update(newVersion, Optional.empty());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document removeCommentFromAlert(CommentAddRemoveRequest request)
      throws IOException {
    Document latest = getLatest(request.getGuid(), request.getSensorType());
    return removeCommentFromAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest)
      throws IOException {
    if (latest == null || latest.getDocument() == null) {
      throw new IOException(String.format("Unable to remove comment. Document with guid %s cannot be found.",
              request.getGuid()));
    }
    List<Map<String, Object>> commentMap = (List<Map<String, Object>>) latest.getDocument().get(COMMENTS_FIELD);
    // Can't remove anything if there's nothing there
    if (commentMap == null) {
      throw new IOException(String.format("Unable to remove comment. Document with guid %s has no comments.",
              request.getGuid()));
    }
    List<Map<String, Object>> originalComments = new ArrayList<>(commentMap);
    List<AlertComment> comments = new ArrayList<>();
    for (Map<String, Object> commentStr : originalComments) {
      comments.add(new AlertComment(commentStr));
    }

    comments.remove(new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp()));
    Document newVersion = new Document(latest);
    if (comments.size() > 0) {
      List<Map<String, Object>> commentsAsMap = comments.stream().map(AlertComment::asMap)
          .collect(Collectors.toList());
      newVersion.getDocument().put(COMMENTS_FIELD, commentsAsMap);
      update(newVersion, Optional.empty());
    } else {
      newVersion.getDocument().remove(COMMENTS_FIELD);
    }

    return update(newVersion, Optional.empty());
  }
}
