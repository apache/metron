/*
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrRetrieveLatestDao implements RetrieveLatestDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private transient SolrClient client;
  private AccessConfig config;

  public SolrRetrieveLatestDao(SolrClient client, AccessConfig config) {
    this.client = client;
    this.config = config;
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    try {
      Optional<String> index = SolrUtilities
          .getIndex(config.getIndexSupplier(), sensorType, Optional.empty());
      if (!index.isPresent()) {
        LOG.debug("Unable to find index for sensorType {}", sensorType);
        return null;
      }

      SolrDocument solrDocument = client.getById(index.get(), guid);
      if (solrDocument == null) {
        LOG.debug("Unable to find document for sensorType {} and guid {}", sensorType, guid);
        return null;
      }
      return SolrUtilities.toDocument(solrDocument);
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    Map<String, Collection<String>> collectionIdMap = new HashMap<>();
    for (GetRequest getRequest : getRequests) {
      Optional<String> index = SolrUtilities
          .getIndex(config.getIndexSupplier(), getRequest.getSensorType(), getRequest.getIndex());
      if (index.isPresent()) {
        Collection<String> ids = collectionIdMap.getOrDefault(index.get(), new HashSet<>());
        ids.add(getRequest.getGuid());
        collectionIdMap.put(index.get(), ids);
      } else {
        LOG.debug("Unable to find index for sensorType {}", getRequest.getSensorType());
      }
    }
    try {
      List<Document> documents = new ArrayList<>();
      for (String collection : collectionIdMap.keySet()) {
        SolrDocumentList solrDocumentList = client.getById(collectionIdMap.get(collection),
            new SolrQuery().set("collection", collection));
        documents.addAll(
            solrDocumentList.stream().map(SolrUtilities::toDocument).collect(Collectors.toList()));
      }
      return documents;
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }
}
