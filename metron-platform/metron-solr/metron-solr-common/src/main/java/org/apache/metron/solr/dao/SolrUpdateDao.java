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

import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * An {@link UpdateDao} for Solr.
 */
public class SolrUpdateDao implements UpdateDao {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private transient SolrClient client;
  private transient SolrRetrieveLatestDao retrieveLatestDao;
  private AccessConfig config;
  private SolrDocumentBuilder documentBuilder;

  public SolrUpdateDao(SolrClient client, SolrRetrieveLatestDao retrieveLatestDao, AccessConfig config) {
    this.client = client;
    this.retrieveLatestDao = retrieveLatestDao;
    this.config = config;
    this.documentBuilder = new SolrDocumentBuilder();
  }

  @Override
  public Document update(Document update, Optional<String> rawIndex) throws IOException {
    Document newVersion = new Document(update);

    Optional<String> index = SolrUtilities.getIndex(config.getIndexSupplier(), newVersion.getSensorType(), rawIndex);
    if(index.isPresent()) {
      SolrDocument solrDocument = documentBuilder.fromDocument(newVersion);
      writeDocuments(index.get(), solrDocument);

    } else {
      throw new IllegalStateException("Index must be specified or inferred.");
    }

    return newVersion;
  }

  @Override
  public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    // updates with a collection specified
    Map<String, Collection<SolrDocument>> solrCollectionUpdates = new HashMap<>();
    Set<String> collectionsUpdated = new HashSet<>();

    for (Entry<Document, Optional<String>> entry : updates.entrySet()) {
      SolrDocument solrInputDocument = documentBuilder.fromDocument(entry.getKey());
      Optional<String> index = SolrUtilities
              .getIndex(config.getIndexSupplier(), entry.getKey().getSensorType(), entry.getValue());
      if (index.isPresent()) {
        Collection<SolrDocument> solrInputDocuments = solrCollectionUpdates.getOrDefault(index.get(), new ArrayList<>());
        solrInputDocuments.add(solrInputDocument);

        solrCollectionUpdates.put(index.get(), solrInputDocuments);
        collectionsUpdated.add(index.get());
      } else {
        String lookupIndex = config.getIndexSupplier().apply(entry.getKey().getSensorType());
        Collection<SolrDocument> solrInputDocuments = solrCollectionUpdates.getOrDefault(lookupIndex, new ArrayList<>());
        solrInputDocuments.add(solrInputDocument);
        solrCollectionUpdates.put(lookupIndex, solrInputDocuments);
        collectionsUpdated.add(lookupIndex);
      }
    }

    for (Entry<String, Collection<SolrDocument>> entry : solrCollectionUpdates.entrySet()) {
      String index = entry.getKey();
      Collection<SolrDocument> documents = entry.getValue();
      writeDocuments(index, documents);
    }

    return updates;
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return addCommentToAlert(request, latest);
  }

  @Override
  public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException {
    if (latest == null || latest.getDocument() == null) {
      throw new IOException(format("Unable to add comment. Document with guid %s cannot be found.", request.getGuid()));
    }

    AlertComment toAdd = new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp());
    Document updatedDocument = new Document(latest);
    updatedDocument.addComment(toAdd);

    // persist the changes
    return update(updatedDocument, Optional.empty());
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException {
    Document latest = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
    return removeCommentFromAlert(request, latest);
  }

  @Override
  public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest)
      throws IOException {
    if (latest == null || latest.getDocument() == null) {
      throw new IOException(format("Unable to remove comment. Document with guid %s cannot be found.", request.getGuid()));
    }

    // remove the comment from the existing comments
    AlertComment toRemove = new AlertComment(request.getComment(), request.getUsername(), request.getTimestamp());
    Document updatedDocument = new Document(latest);
    boolean wasRemoved = updatedDocument.removeComment(toRemove);
    if(!wasRemoved) {
      throw new IOException(String.format("Unable to remove comment. Document with guid %s has no comments.", request.getGuid()));
    }

    // persist the changes
    return update(updatedDocument, Optional.empty());
  }

  /**
   * Writes a {@link SolrDocument} to a Solr index (aka Collection).
   *
   * @param documents The document to write.
   * @param index THe index/collection to write to.
   */
  private void writeDocuments(String index, SolrDocument ... documents) throws IOException {
    try {
      // add each document to the batch
      for(SolrDocument document: documents) {
        SolrInputDocument input = documentBuilder.toSolrInputDocument(document);
        client.add(index, input);
      }

      // commit all pending documents
      client.commit(index);

    } catch (SolrException | SolrServerException | IOException e) {
      LOG.error("Unable to add document; index={}", index, e);
      throw new IOException(e);
    }
  }

  private void writeDocuments(String index, Collection<SolrDocument> documents) throws IOException {
    writeDocuments(index, documents.toArray(new SolrDocument[documents.size()]));
  }
}
