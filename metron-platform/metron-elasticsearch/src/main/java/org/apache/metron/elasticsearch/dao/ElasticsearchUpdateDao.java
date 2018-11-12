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

import org.apache.metron.elasticsearch.bulk.BulkDocumentWriter;
import org.apache.metron.elasticsearch.bulk.ElasticsearchBulkDocumentWriter;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.jets3t.service.model.cloudfront.Origin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

public class ElasticsearchUpdateDao implements UpdateDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private AccessConfig accessConfig;
  private ElasticsearchRetrieveLatestDao retrieveLatestDao;
  private BulkDocumentWriter<Document> documentWriter;
  private int failures;
  private Throwable lastException;

  public ElasticsearchUpdateDao(ElasticsearchClient client,
      AccessConfig accessConfig,
      ElasticsearchRetrieveLatestDao searchDao) {
    this.accessConfig = accessConfig;
    this.retrieveLatestDao = searchDao;
    this.documentWriter = new ElasticsearchBulkDocumentWriter<>(client);
  }

  @Override
  public Document update(Document update, Optional<String> index) throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<>();
    updates.put(update, index);

    Map<Document, Optional<String>> results = batchUpdate(updates);
    return results.keySet().iterator().next();
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    Map<String, Object> globalConfig = accessConfig.getGlobalConfigSupplier().get();
    String indexPostfix = ElasticsearchUtils.getIndexFormat(globalConfig).format(new Date());

    for (Map.Entry<Document, Optional<String>> entry : updates.entrySet()) {
      Document document = entry.getKey();
      String indexName = entry.getValue().orElse(getIndexName(document, indexPostfix));
      documentWriter.addDocument(document, indexName);
    }

    // record failures so that a checked exception can be thrown later; cannot throw checked exception in listener
    failures = 0;
    lastException = null;
    documentWriter.onFailure((document, cause, message) -> {
      failures++;
      lastException = cause;
      LOG.error(message, cause);
    });

    // write the documents. if any document fails, raise an exception.
    documentWriter.write();
    if(failures > 0) {
      int batchSize = updates.entrySet().size();
      String error = format("Failed to update all documents; %d of %d failed", failures, batchSize);
      throw new IOException(error, lastException);
    }

    return updates;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return addCommentToAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null || latest.getDocument() == null) {
      throw new IOException(String.format("Unable to add comment. Document with guid %s cannot be found.",
              request.getGuid()));
    }
    List<Map<String, Object>> commentsField = (List<Map<String, Object>>) latest.getDocument()
        .getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    List<Map<String, Object>> originalComments = new ArrayList<>(commentsField);

    originalComments.add(
        new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp())
            .asMap());

    Document newVersion = new Document(latest);
    newVersion.getDocument().put(COMMENTS_FIELD, originalComments);
    return update(newVersion, Optional.empty());
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return removeCommentFromAlert(request, latest);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
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

    return update(newVersion, Optional.empty());
  }

  /**
   * Replace a document in an index.
   * @param request The replacement request.
   * @param optionalTimestamp The timestamp (optional) of the update.  If not specified, then current time will be used.
   * @return The replaced document.
   * @throws IOException If an error occurs during replacement.
   */
  @Override
  public Document replace(ReplaceRequest request, Optional<Long> optionalTimestamp)
          throws IOException, OriginalNotFoundException {

    Map<String, Object> source = request.getReplacement();
    String guid = request.getGuid();
    String sensorType = request.getSensorType();
    Long timestamp = optionalTimestamp.orElse(System.currentTimeMillis());
    Optional<String> documentID = findDocumentID(guid, sensorType);
    Optional<String> index = Optional.ofNullable(request.getIndex());

    Document replacement = new Document(source, guid, sensorType, timestamp, documentID);
    return update(replacement, index);
  }

  private Optional<String> findDocumentID(String guid, String sensorType)
          throws IOException, OriginalNotFoundException {

    Document document = retrieveLatestDao.getLatest(guid, sensorType);
    if(document == null) {
      String error = format("Cannot find document to replace; guid=%s, sensorType=%s", guid, sensorType);
      throw new OriginalNotFoundException(error);
    }

    return document.getDocumentID();
  }

  protected String getIndexName(Document update, String indexPostFix) throws IOException {
    return findIndexNameByGUID(update.getGuid(), update.getSensorType())
            .orElse(ElasticsearchUtils.getIndexName(update.getSensorType(), indexPostFix, null));
  }

  protected Optional<String> findIndexNameByGUID(String guid, String sensorType) throws IOException {
    return retrieveLatestDao.searchByGuid(guid,
        sensorType,
        hit -> Optional.ofNullable(hit.getIndex())
    );
  }
}
