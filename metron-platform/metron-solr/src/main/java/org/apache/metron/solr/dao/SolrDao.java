/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.metron.solr.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.search.SortOrder;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrDao implements IndexDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String ID_FIELD = "guid";
  private static final String ROOT_FIELD = "_root_";
  private static final String VERSION_FIELD = "_version_";

  private transient SolrClient client;
  //private transient ConcurrentUpdateSolrClient updateClient;

  private AccessConfig accessConfig;

  protected SolrDao(SolrClient client,
      AccessConfig config) {
    this.client = client;
    this.accessConfig = config;
  }

  public SolrDao() {
    //uninitialized.
  }

  private static Map<String, FieldType> solrTypeMap;

  static {
    Map<String, FieldType> fieldTypeMap = new HashMap<>();
    fieldTypeMap.put("string", FieldType.TEXT);
    fieldTypeMap.put("pint", FieldType.INTEGER);
    fieldTypeMap.put("plong", FieldType.LONG);
    fieldTypeMap.put("pfloat", FieldType.FLOAT);
    fieldTypeMap.put("pdouble", FieldType.DOUBLE);
    fieldTypeMap.put("boolean", FieldType.BOOLEAN);
    solrTypeMap = Collections.unmodifiableMap(fieldTypeMap);
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    if (searchRequest.getQuery() == null) {
      throw new InvalidSearchException("Search query is invalid: null");
    }
    if (client == null) {
      throw new InvalidSearchException("Uninitialized Dao!  You must call init() prior to use.");
    }
    if (searchRequest.getSize() > accessConfig.getMaxSearchResults()) {
      throw new InvalidSearchException(
          "Search result size must be less than " + accessConfig.getMaxSearchResults());
    }
    SolrQuery query = buildSearchRequest(searchRequest);
    try {
      QueryResponse response = client.query(query);
      return buildSearchResponse(searchRequest, response);
    } catch (IOException | SolrServerException e) {
      String msg = e.getMessage();
      LOG.error(msg, e);
      throw new InvalidSearchException(msg, e);
    }
  }

  private SolrQuery buildSearchRequest(
      SearchRequest searchRequest) throws InvalidSearchException {
    SolrQuery query = new SolrQuery()
        .setStart(searchRequest.getFrom())
        .setRows(searchRequest.getSize())
        .setQuery(searchRequest.getQuery());

    // handle sort fields
    for (SortField sortField : searchRequest.getSort()) {
      query.addSort(sortField.getField(), getSolrSortOrder(sortField.getSortOrder()));
    }

    // handle search fields
    Optional<List<String>> fields = searchRequest.getFields();
    if (fields.isPresent()) {
      fields.get().forEach(query::addField);
    }

    //handle facet fields
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      facetFields.get().forEach(query::addFacetField);
    }

    String collections = searchRequest.getIndices().stream().collect(Collectors.joining(","));
    query.set("collection", collections);

    return query;
  }

  private SearchResponse buildSearchResponse(
      SearchRequest searchRequest,
      QueryResponse solrResponse) throws InvalidSearchException {

    SearchResponse searchResponse = new SearchResponse();
    SolrDocumentList solrDocumentList = solrResponse.getResults();
    searchResponse.setTotal(solrDocumentList.getNumFound());

    // search hits --> search results
    List<SearchResult> results = solrDocumentList.stream()
        .map(solrDocument -> getSearchResult(solrDocument, searchRequest.getFields()))
        .collect(Collectors.toList());
    searchResponse.setResults(results);

    // handle facet fields
    Optional<List<String>> facetFields = searchRequest.getFacetFields();
    if (facetFields.isPresent()) {
      searchResponse.setFacetCounts(getFacetCounts(facetFields.get(), solrResponse));
    }

    if (LOG.isDebugEnabled()) {
      String response;
      try {
        response = JSONUtils.INSTANCE.toJSON(searchResponse, false);
      } catch (JsonProcessingException e) {
        response = "???";
      }

      LOG.debug("Built search response; response={}", response);
    }
    return searchResponse;
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return null;
  }

  @Override
  public void init(AccessConfig config) {
    if (this.client == null) {
      Map<String, Object> globalConfig = config.getGlobalConfigSupplier().get();
//      this.client = new HttpSolrClient.Builder()
//          .withBaseSolrUrl("http://192.168.99.100:8983/solr/bro").build();
//      this.updateClient = new ConcurrentUpdateSolrClient.Builder("http://192.168.99.100:8983/solr/bro").build();
      this.client = new CloudSolrClient.Builder().withZkHost((String) globalConfig.get("solr.zookeeper")).build();
      this.accessConfig = config;
    }
  }

  @Override
  public Document getLatest(String guid, String collection) throws IOException {
    try {
      SolrDocument solrDocument = client.getById(collection, guid);
      return toDocument(solrDocument);
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    Map<String, Collection<String>> collectionIdMap = new HashMap<>();
    for (GetRequest getRequest: getRequests) {
      Collection<String> ids = collectionIdMap.get(getRequest.getSensorType());
      if (ids == null) {
        ids = new HashSet<>();
      }
      ids.add(getRequest.getGuid());
      collectionIdMap.put(getRequest.getSensorType(), ids);
    }
    try {
      List<Document> documents = new ArrayList<>();
      for (String collection: collectionIdMap.keySet()) {
        SolrDocumentList solrDocumentList = client.getById(collectionIdMap.get(collection),
            new SolrQuery().set("collection", collection));
        documents.addAll(solrDocumentList.stream().map(this::toDocument).collect(Collectors.toList()));
      }
      return documents;
    } catch (SolrServerException e) {
      throw new IOException(e);
    }
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

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> indexColumnMetadata = new HashMap<>();
    Map<String, String> previousIndices = new HashMap<>();
    Set<String> fieldBlackList = new HashSet<>();

    for (String index : indices) {
      SolrClient client = new HttpSolrClient.Builder()
          .withBaseSolrUrl("http://192.168.99.100:8983/solr/" + index).build();
      try {
        SchemaRepresentation schemaRepresentation = new SchemaRequest().process(client)
            .getSchemaRepresentation();
        schemaRepresentation.getFields().stream().forEach(field -> {
          String name = (String) field.get("name");
          if (!ROOT_FIELD.equals(name) && !VERSION_FIELD.equals(name)) {
            FieldType type = toFieldType((String) field.get("type"));
            if (!indexColumnMetadata.containsKey(name)) {
              indexColumnMetadata.put(name, type);

              // record the last index in which a field exists, to be able to print helpful error message on type mismatch
              previousIndices.put(name, index);
            } else {
              FieldType previousType = indexColumnMetadata.get(name);
              if (!type.equals(previousType)) {
                String previousIndexName = previousIndices.get(name);
                LOG.error(String.format(
                    "Field type mismatch: %s.%s has type %s while %s.%s has type %s.  Defaulting type to %s.",
                    index, field, type.getFieldType(),
                    previousIndexName, field, previousType.getFieldType(),
                    FieldType.OTHER.getFieldType()));
                indexColumnMetadata.put(name, FieldType.OTHER);

                // the field is defined in multiple indices with different types; ignore the field as type has been set to OTHER
                fieldBlackList.add(name);
              }
            }
          }
        });
      } catch (SolrServerException e) {
        throw new IOException(e);
      } catch (RemoteSolrException e) {
        // 404 means an index is missing so continue
        if (e.code() != 404) {
          throw new IOException(e);
        }
      }
    }
    return indexColumnMetadata;
  }

  private SolrQuery.ORDER getSolrSortOrder(
      SortOrder sortOrder) {
    return sortOrder == SortOrder.DESC ?
        ORDER.desc : ORDER.asc;
  }

  private Map<String, Map<String, Long>> getFacetCounts(List<String> fields,
      QueryResponse solrResponse) {
    Map<String, Map<String, Long>> fieldCounts = new HashMap<>();
    for (String field : fields) {
      Map<String, Long> valueCounts = new HashMap<>();
      FacetField facetField = solrResponse.getFacetField(field);
      for (Count facetCount : facetField.getValues()) {
        valueCounts.put(facetCount.getName(), facetCount.getCount());
      }
      fieldCounts.put(field, valueCounts);
    }
    return fieldCounts;
  }

  private SearchResult getSearchResult(SolrDocument solrDocument, Optional<List<String>> fields) {
    SearchResult searchResult = new SearchResult();
    searchResult.setId((String) solrDocument.getFieldValue(ID_FIELD));
    Map<String, Object> source;
    if (fields.isPresent()) {
      source = new HashMap<>();
      fields.get().forEach(field -> source.put(field, solrDocument.getFieldValue(field)));
    } else {
      source = solrDocument.getFieldValueMap();
    }
    searchResult.setSource(source);
    return searchResult;
  }

  /**
   * Converts a string type to the corresponding FieldType.
   *
   * @param type The type to convert.
   * @return The corresponding FieldType or FieldType.OTHER, if no match.
   */
  private FieldType toFieldType(String type) {
    return solrTypeMap.getOrDefault(type, FieldType.OTHER);
  }

  private SolrInputDocument toSolrInputDocument(Document document) {
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    document.getDocument().entrySet().forEach(entry ->
        solrInputDocument.addField(entry.getKey(), entry.getValue()));
    return solrInputDocument;
  }

  private Document toDocument(SolrDocument solrDocument) {
    Map<String, Object> document = new HashMap<>();
    solrDocument.getFieldNames().stream()
        .filter(name -> !name.equals(VERSION_FIELD))
        .forEach(name -> document.put(name, solrDocument.getFieldValue(name)));
    return new Document(document,
        (String) solrDocument.getFieldValue(ID_FIELD),
        (String) solrDocument.getFieldValue("source:type"), 0L);
  }
}
