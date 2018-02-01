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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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

  public SolrUpdateDao(SolrClient client) {
    this.client = client;
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    try {
      SolrInputDocument solrInputDocument = toSolrInputDocument(update);
      if (index.isPresent()) {
        this.client.add(index.get(), solrInputDocument);
      } else {
        this.client.add(solrInputDocument);
      }
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    // updates with a collection specified
    Map<String, Collection<SolrInputDocument>> solrCollectionUpdates = new HashMap<>();

    // updates with no collection specified
    Collection<SolrInputDocument> solrUpdates = new ArrayList<>();

    for(Entry<Document, Optional<String>> entry: updates.entrySet()) {
      SolrInputDocument solrInputDocument = toSolrInputDocument(entry.getKey());
      Optional<String> index = entry.getValue();
      if (index.isPresent()) {
        Collection<SolrInputDocument> solrInputDocuments = solrCollectionUpdates.get(index.get());
        if (solrInputDocuments == null) {
          solrInputDocuments = new ArrayList<>();
        }
        solrInputDocuments.add(solrInputDocument);
        solrCollectionUpdates.put(index.get(), solrInputDocuments);
      } else {
        solrUpdates.add(solrInputDocument);
      }
    }
    try {
      if (!solrCollectionUpdates.isEmpty()) {
        for(Entry<String, Collection<SolrInputDocument>> entry: solrCollectionUpdates.entrySet()) {
          this.client.add(entry.getKey(), entry.getValue());
        }
      } else {
        this.client.add(solrUpdates);
      }
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  private SolrInputDocument toSolrInputDocument(Document document) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    document.getDocument().forEach(solrInputDocument::addField);
    return solrInputDocument;
  }
}
