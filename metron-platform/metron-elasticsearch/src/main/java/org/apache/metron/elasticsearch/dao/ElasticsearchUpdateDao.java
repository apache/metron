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
package org.apache.metron.elasticsearch.dao;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse.ShardInfo;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticsearchUpdateDao implements UpdateDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient TransportClient client;
  private AccessConfig accessConfig;
  private ElasticsearchRetrieveLatestDao retrieveLatestDao;

  public ElasticsearchUpdateDao(TransportClient client,
      AccessConfig accessConfig,
      ElasticsearchRetrieveLatestDao searchDao) {
    this.client = client;
    this.accessConfig = accessConfig;
    this.retrieveLatestDao = searchDao;
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    String indexPostfix = ElasticsearchUtils
        .getIndexFormat(accessConfig.getGlobalConfigSupplier().get()).format(new Date());
    String sensorType = update.getSensorType();
    String indexName = getIndexName(update, index, indexPostfix);

    IndexRequest indexRequest = buildIndexRequest(update, sensorType, indexName);
    try {
      IndexResponse response = client.index(indexRequest).get();

      ShardInfo shardInfo = response.getShardInfo();
      int failed = shardInfo.getFailed();
      if (failed > 0) {
        throw new IOException(
            "ElasticsearchDao index failed: " + Arrays.toString(shardInfo.getFailures()));
      }
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    String indexPostfix = ElasticsearchUtils
        .getIndexFormat(accessConfig.getGlobalConfigSupplier().get()).format(new Date());

    BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

    // Get the indices we'll actually be using for each Document.
    for (Map.Entry<Document, Optional<String>> updateEntry : updates.entrySet()) {
      Document update = updateEntry.getKey();
      String sensorType = update.getSensorType();
      String indexName = getIndexName(update, updateEntry.getValue(), indexPostfix);
      IndexRequest indexRequest = buildIndexRequest(
          update,
          sensorType,
          indexName
      );

      bulkRequestBuilder.add(indexRequest);
    }

    BulkResponse bulkResponse = bulkRequestBuilder.get();
    if (bulkResponse.hasFailures()) {
      LOG.error("Bulk Request has failures: {}", bulkResponse.buildFailureMessage());
      throw new IOException(
          "ElasticsearchDao upsert failed: " + bulkResponse.buildFailureMessage());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    addCommentToAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null) {
      return;
    }
    List<Map<String, Object>> commentsField = (List<Map<String, Object>>) latest.getDocument()
        .getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    List<Map<String, Object>> originalComments = new ArrayList<>(commentsField);

    originalComments.add(
        new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp())
            .asMap());

    Document newVersion = new Document(latest);
    newVersion.getDocument().put(COMMENTS_FIELD, originalComments);
    update(newVersion, Optional.empty());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    removeCommentFromAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null) {
      return;
    }
    List<Map<String, Object>> commentsField = (List<Map<String, Object>>) latest.getDocument()
        .getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    List<Map<String, Object>> originalComments = new ArrayList<>(commentsField);

    List<AlertComment> alertComments = new ArrayList<>();
    for (Map<String, Object> commentRaw : originalComments) {
      alertComments.add(new AlertComment(commentRaw));
    }

    alertComments.remove(
        new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp()));
    List<Map<String, Object>> commentsFinal = alertComments.stream().map(AlertComment::asMap)
        .collect(Collectors.toList());
    Document newVersion = new Document(latest);
    if (commentsFinal.size() > 0) {
      newVersion.getDocument().put(COMMENTS_FIELD, commentsFinal);
      update(newVersion, Optional.empty());
    } else {
      newVersion.getDocument().remove(COMMENTS_FIELD);
    }

    update(newVersion, Optional.empty());
  }

  protected String getIndexName(Document update, Optional<String> index, String indexPostFix) {
    return index.orElse(getIndexName(update.getGuid(), update.getSensorType())
        .orElse(ElasticsearchUtils.getIndexName(update.getSensorType(), indexPostFix, null))
    );
  }

  protected Optional<String> getIndexName(String guid, String sensorType) {
    return retrieveLatestDao.searchByGuid(guid,
        sensorType,
        hit -> Optional.ofNullable(hit.getIndex())
    );
  }

  protected IndexRequest buildIndexRequest(Document update, String sensorType, String indexName) {
    String type = sensorType + "_doc";
    Object ts = update.getTimestamp();
    IndexRequest indexRequest = new IndexRequest(indexName, type, update.getGuid())
        .source(update.getDocument());
    if (ts != null) {
      indexRequest = indexRequest.timestamp(ts.toString());
    }

    return indexRequest;
  }
}
