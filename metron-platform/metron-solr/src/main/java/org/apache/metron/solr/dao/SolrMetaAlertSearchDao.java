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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertSearchDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.CursorMarkParams;

public class SolrMetaAlertSearchDao implements MetaAlertSearchDao {

  transient SolrSearchDao solrSearchDao;
  transient SolrClient solrClient;

  public SolrMetaAlertSearchDao(SolrClient solrClient, SolrSearchDao solrSearchDao) {
    this.solrClient = solrClient;
    this.solrSearchDao = solrSearchDao;
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
    SolrQuery solrQuery = new SolrQuery()
        .setQuery(fullClause)
        .setFields("*", "[child parentFilter=" + activeClause + " limit=999]")
        .addSort(Constants.GUID,
            SolrQuery.ORDER.asc); // Just do basic sorting to track where we are

    // Use Solr's Cursors to handle the paging, rather than doing it manually.
    List<SearchResult> allResults = new ArrayList<>();
    try {
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      boolean done = false;
      while (!done) {
        solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        // TODO grab this more reasonablly
        QueryResponse rsp = solrClient.query(METAALERTS_COLLECTION, solrQuery);
        String nextCursorMark = rsp.getNextCursorMark();
        rsp.getResults().stream()
            .map(solrDocument -> SolrUtilities.getSearchResult(solrDocument, Optional.empty()))
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

    String sensorType = ClientUtils.escapeQueryChars(MetaAlertConstants.SOURCE_TYPE);
    String metaalertTypeClause = sensorType + ":" + MetaAlertConstants.METAALERT_TYPE;
    // Use the 'v=' form in order to ensure complex clauses are properly handled.
    // Per the docs, the 'which=' clause should be used to identify all metaalert parents, not to
    //   filter
    // Status is a filter on parents and must be done outside the '!parent' construct
    String parentChildQuery =
        "(" + activeStatusClause + " AND " + "{!parent which=" + metaalertTypeClause + " v='"
            + searchRequest.getQuery() + "'})";

    // Put everything together to get our full query
    // The '-metaalert:[* TO *]' construct is to ensure the field doesn't exist on or is empty for
    //   plain alerts.
    // Also make sure that it's not a metaalert
    String fullQuery =
        "(" + searchRequest.getQuery() + " AND -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
            + " AND " + "-" + sensorType + ":" + MetaAlertConstants.METAALERT_TYPE + ")"
            + " OR " + parentChildQuery;

    searchRequest.setQuery(fullQuery);

    // Build the custom field list
    Optional<List<String>> fields = searchRequest.getFields();
    String fieldList = "*";
    if (fields.isPresent()) {
      fieldList = StringUtils.join(fields.get(), ",");
    }
    String childClause = fieldList + ",[child parentFilter=" + MetaAlertConstants.STATUS_FIELD + ":"
        + MetaAlertStatus.ACTIVE.getStatusString() + "]";

    return solrSearchDao.search(searchRequest, childClause);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    // Make sure to escape any problematic characters here
    String sourceType = ClientUtils.escapeQueryChars(MetaAlertConstants.SOURCE_TYPE);
    String baseQuery = groupRequest.getQuery();
    String adjustedQuery = baseQuery + " -" + MetaAlertConstants.METAALERT_FIELD + ":[* TO *]"
        + " -" + sourceType + ":" + MetaAlertConstants.METAALERT_TYPE;
    groupRequest.setQuery(adjustedQuery);
    return solrSearchDao.group(groupRequest);
  }
}
