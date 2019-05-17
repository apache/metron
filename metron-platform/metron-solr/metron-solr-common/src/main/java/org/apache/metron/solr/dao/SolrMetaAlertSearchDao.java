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

import static org.apache.metron.solr.dao.SolrMetaAlertDao.METAALERTS_COLLECTION;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertSearchDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrMetaAlertSearchDao implements MetaAlertSearchDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  transient SolrSearchDao solrSearchDao;
  transient SolrClient solrClient;
  private MetaAlertConfig config;

  public SolrMetaAlertSearchDao(SolrClient solrClient, SolrSearchDao solrSearchDao, MetaAlertConfig config) {
    this.solrClient = solrClient;
    this.solrSearchDao = solrSearchDao;
    this.config = config;
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    if (guid == null || guid.trim().isEmpty()) {
      throw new InvalidSearchException("Guid cannot be empty");
    }

    // Searches for all alerts containing the meta alert guid in it's "metalerts" array
    // The query has to match the parentFilter to avoid errors.  Guid must also be explicitly
    // included.
    String activeClause =
        MetaAlertConstants.STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString();
    String guidClause = Constants.GUID + ":" + guid;
    String fullClause = "{!parent which=" + activeClause + "}" + guidClause;
    String metaalertTypeClause = config.getSourceTypeField() + ":" + MetaAlertConstants.METAALERT_TYPE;
    SolrQuery solrQuery = new SolrQuery()
        .setQuery(fullClause)
        .setFields("*", "[child parentFilter=" + metaalertTypeClause + " limit=999]")
        .addSort(Constants.GUID,
            SolrQuery.ORDER.asc); // Just do basic sorting to track where we are

    // Use Solr's Cursors to handle the paging, rather than doing it manually.
    List<SearchResult> allResults = new ArrayList<>();
    try {
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      boolean done = false;
      while (!done) {
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse rsp = solrClient.query(METAALERTS_COLLECTION, solrQuery);
        String nextCursorMark = rsp.getNextCursorMark();
        rsp.getResults().stream()
            .map(solrDocument -> SolrUtilities.getSearchResult(solrDocument, null,
                    solrSearchDao.getAccessConfig().getIndexSupplier()))
            .forEachOrdered(allResults::add);
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
      }
    } catch (IOException | SolrServerException e) {
      throw new InvalidSearchException("Unable to complete search", e);
    }

    SearchResponse searchResponse = new SearchResponse();
    searchResponse.setResults(allResults);
    searchResponse.setTotal(allResults.size());
    return searchResponse;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    // Need to wrap such that two things are true
    // 1. The provided query is true OR nested query on the alert field is true
    // 2. Metaalert is active OR it's not a metaalert

    String activeStatusClause =
        MetaAlertConstants.STATUS_FIELD + ":" + MetaAlertStatus.ACTIVE.getStatusString();

    String metaalertTypeClause = config.getSourceTypeField() + ":" + MetaAlertConstants.METAALERT_TYPE;
    // Use the 'v=' form in order to ensure complex clauses are properly handled.
    // Per the docs, the 'which=' clause should be used to identify all metaalert parents, not to
    //   filter
    // Status is a filter on parents and must be done outside the '!parent' construct
    String parentChildQuery =
        "(+" + activeStatusClause + " +" + "{!parent which=" + metaalertTypeClause + " v='"
            + searchRequest.getQuery() + "'})";

    // Put everything together to get our full query
    // The '-metaalert:[* TO *]' construct is to ensure the field doesn't exist on or is empty for
    //   plain alerts.
    // Also make sure that it's not a metaalert
    String fullQuery =
        "(" + searchRequest.getQuery() + " AND -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
            + " AND " + "-" + metaalertTypeClause + ")" + " OR " + parentChildQuery;

    LOG.debug("MetaAlert search query {}", fullQuery);

    searchRequest.setQuery(fullQuery);

    // Build the custom field list
    List<String> fields = searchRequest.getFields();
    String fieldList = "*";
    if (fields != null) {
      fieldList = StringUtils.join(fields, ",");
    }

    LOG.debug("MetaAlert Search Field list {}", fullQuery);

    SearchResponse results = solrSearchDao.search(searchRequest, fieldList);
    LOG.debug("MetaAlert Search Number of results {}", results.getResults().size());

    // Unfortunately, we can't get the full metaalert results at the same time
    // Get them in a second query.
    // However, we can only retrieve them if we have the source type field (either explicit or
    // wildcard).
    if (fieldList.contains("*") || fieldList.contains(config.getSourceTypeField())) {
      List<String> metaalertGuids = new ArrayList<>();
      for (SearchResult result : results.getResults()) {
        if (result.getSource().get(config.getSourceTypeField())
            .equals(MetaAlertConstants.METAALERT_TYPE)) {
          // Then we need to add it to the list to retrieve child alerts in a second query.
          metaalertGuids.add(result.getId());
        }
      }
      LOG.debug("MetaAlert Search guids requiring retrieval: {}", metaalertGuids);

      // If we have any metaalerts in our result, attach the full data.
      if (metaalertGuids.size() > 0) {
        Map<String, String> params = new HashMap<>();
        params.put("fl", fieldList + ",[child parentFilter=" + metaalertTypeClause + " limit=999]");
        SolrParams solrParams = new MapSolrParams(params);
        try {
          SolrDocumentList solrDocumentList = solrClient
              .getById(METAALERTS_COLLECTION, metaalertGuids, solrParams);
          Map<String, Document> guidToDocuments = new HashMap<>();
          for (SolrDocument doc : solrDocumentList) {
            Document document = SolrUtilities.toDocument(doc);
            guidToDocuments.put(document.getGuid(), document);
          }

          // Run through our results and update them with the full metaalert
          for (SearchResult result : results.getResults()) {
            Document fullDoc = guidToDocuments.get(result.getId());
            if (fullDoc != null) {
              result.setSource(fullDoc.getDocument());
            }
          }
        } catch (SolrServerException | IOException e) {
          throw new InvalidSearchException("Error when retrieving child alerts for metaalerts", e);
        }

      }
    }
    return results;
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Make sure to escape any problematic characters here
    String sourceType = ClientUtils.escapeQueryChars(config.getSourceTypeField());
    String baseQuery = groupRequest.getQuery();
    String adjustedQuery = baseQuery + " -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
        + " -" + sourceType + ":" + MetaAlertConstants.METAALERT_TYPE;
    LOG.debug("MetaAlert group adjusted query: {}", adjustedQuery);
    groupRequest.setQuery(adjustedQuery);
    return solrSearchDao.group(groupRequest);
  }
}
