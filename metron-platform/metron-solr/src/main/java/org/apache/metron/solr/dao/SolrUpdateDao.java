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
import java.util.function.Function;

import org.apache.metron.indexing.dao.AccessConfig;
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
  private Function<String, String> indexSupplier;
  public SolrUpdateDao(SolrClient client, AccessConfig config) {
    this.client = client;
    this.config = config;
    this.indexSupplier = config.getIndexSupplier();
  }

  private Optional<String> getIndex(String sensorName, Optional<String> index) {
    if(index.isPresent()) {
      return Optional.ofNullable(index.get());
    }
    else {
      String realIndex = indexSupplier.apply(sensorName);
      return Optional.ofNullable(realIndex);
    }
  }

  @Override
  public void update(Document update, Optional<String> rawIndex) throws IOException {
    try {
      SolrInputDocument solrInputDocument = SolrUtilities.toSolrInputDocument(update);
      Optional<String> index = getIndex(update.getSensorType(), rawIndex);
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
      Optional<String> index = getIndex(entry.getKey().getSensorType(), entry.getValue());
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
}
