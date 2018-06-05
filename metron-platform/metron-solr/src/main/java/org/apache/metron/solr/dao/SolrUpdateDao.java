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
package org.apache.metron.solr.dao;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrUpdateDao implements UpdateDao {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient SolrClient client;
  private AccessConfig config;
  private transient SolrRetrieveLatestDao retrieveLatestDao;

  public SolrUpdateDao(SolrClient client, SolrRetrieveLatestDao retrieveLatestDao, AccessConfig config) {
    this.client = client;
    this.retrieveLatestDao = retrieveLatestDao;
    this.config = config;
  }

  @Override
  public void update(Document update, Optional<String> rawIndex) throws IOException {
    Document newVersion = update;
    // Handle any case where we're given comments in Map form, instead of raw String
    Object commentsObj = update.getDocument().get(COMMENTS_FIELD);
    if ( commentsObj instanceof List &&
        ((List<Object>) commentsObj).size() > 0 &&
      ((List<Object>) commentsObj).get(0) instanceof Map) {
      newVersion = new Document(update);
      convertCommentsToRaw(newVersion.getDocument());
    }
    try {
      SolrInputDocument solrInputDocument = SolrUtilities.toSolrInputDocument(newVersion);
      Optional<String> index = SolrUtilities
          .getIndex(config.getIndexSupplier(), newVersion.getSensorType(), rawIndex);
      if (index.isPresent()) {
        this.client.add(index.get(), solrInputDocument);
        this.client.commit(index.get());
      } else {
        throw new IllegalStateException("Index must be specified or inferred.");
      }
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    // updates with a collection specified
    Map<String, Collection<SolrInputDocument>> solrCollectionUpdates = new HashMap<>();
    Set<String> collectionsUpdated = new HashSet<>();

    for (Entry<Document, Optional<String>> entry : updates.entrySet()) {
      SolrInputDocument solrInputDocument = SolrUtilities.toSolrInputDocument(entry.getKey());
      Optional<String> index = SolrUtilities
          .getIndex(config.getIndexSupplier(), entry.getKey().getSensorType(), entry.getValue());
      if (index.isPresent()) {
        Collection<SolrInputDocument> solrInputDocuments = solrCollectionUpdates
            .getOrDefault(index.get(), new ArrayList<>());
        solrInputDocuments.add(solrInputDocument);
        solrCollectionUpdates.put(index.get(), solrInputDocuments);
        collectionsUpdated.add(index.get());
      } else {
        String lookupIndex = config.getIndexSupplier().apply(entry.getKey().getSensorType());
        Collection<SolrInputDocument> solrInputDocuments = solrCollectionUpdates
            .getOrDefault(lookupIndex, new ArrayList<>());
        solrInputDocuments.add(solrInputDocument);
        solrCollectionUpdates.put(lookupIndex, solrInputDocuments);
        collectionsUpdated.add(lookupIndex);
      }
    }
    try {
      for (Entry<String, Collection<SolrInputDocument>> entry : solrCollectionUpdates
          .entrySet()) {
        this.client.add(entry.getKey(), entry.getValue());
      }
      for (String collection : collectionsUpdated) {
        this.client.commit(collection);
      }
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    addCommentToAlert(request, latest);
  }

  @Override
  public void addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> comments = (List<Map<String, Object>>) latest.getDocument()
        .getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    List<Map<String, Object>> originalComments = new ArrayList<>(comments);

    // Convert all comments back to raw JSON before updating.
    List<String> commentStrs = new ArrayList<>();
    for (Map<String, Object> comment : originalComments) {
      commentStrs.add(new AlertComment(comment).asJson());
    }
    commentStrs.add(new AlertComment(
        request.getComment(),
        request.getUsername(),
        request.getTimestamp()
    ).asJson());

    Document newVersion = new Document(latest);
    newVersion.getDocument().put(COMMENTS_FIELD, commentStrs);
    update(newVersion, Optional.empty());
  }

  @Override
  public void removeCommentFromAlert(CommentAddRemoveRequest request)
      throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    removeCommentFromAlert(request, latest);
  }

  @Override
  public void removeCommentFromAlert(CommentAddRemoveRequest request, Document latest)
      throws IOException {
    if (latest == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> commentMap = (List<Map<String, Object>>) latest.getDocument()
        .get(COMMENTS_FIELD);
    // Can't remove anything if there's nothing there
    if (commentMap == null) {
      LOG.debug("Provided alert had no comments to be able to remove from");
      return;
    }
    List<Map<String, Object>> originalComments = new ArrayList<>(commentMap);
    List<AlertComment> comments = new ArrayList<>();
    for (Map<String, Object> commentStr : originalComments) {
      comments.add(new AlertComment(commentStr));
    }

    comments.remove(
        new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp()));
    List<String> commentsAsJson = comments.stream().map(AlertComment::asJson)
        .collect(Collectors.toList());
    Document newVersion = new Document(latest);
    newVersion.getDocument().put(COMMENTS_FIELD, commentsAsJson);
    update(newVersion, Optional.empty());
  }

  public void convertCommentsToRaw(Map<String,Object> source) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> comments = (List<Map<String, Object>>) source.get(COMMENTS_FIELD);
    if (comments == null || comments.isEmpty()) {
      return;
    }
    List<String> asJson = new ArrayList<>();
    for (Map<String, Object> comment : comments) {
      asJson.add((new AlertComment(comment)).asJson());
    }
    source.put(COMMENTS_FIELD, asJson);
  }
}
