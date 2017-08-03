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

package org.apache.metron.indexing.dao;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InMemoryMetaAlertDao extends InMemoryDao implements MetaAlertDao {

  //  private static InMemoryDao dao = null;
  private static final String META_INDEX = "metaalert";


  /**
   * {
   * "indices": ["metaalert"],
   * "query": { guid : "1" },
   * "from": 0,
   * "size": 10,
   * }
   */
  @Multiline
  public static String metaAlertsForAlertQuery;

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    SearchRequest request;
    try {
      request = JSONUtils.INSTANCE.load(metaAlertsForAlertQuery, SearchRequest.class);
    } catch (IOException e) {
      throw new InvalidSearchException("Unable to process query:", e);
    }
    return search(request);
  }

  @SuppressWarnings("unchecked")
  @Override
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    Map<String, List<String>> backingStore = BACKING_STORE;
    List<String> results = new ArrayList<>();
    // Build meta alert json
    JSONObject metaAlert = new JSONObject();

    JSONArray groupsArray = new JSONArray();
    groupsArray.addAll(request.getGroups());
    metaAlert.put(MetaAlertDao.GROUPS_FIELD, groupsArray);

    Map<String, String> guidsToIndices = request.getGuidToIndices();
    // Retrieve the alert for each guid
    // For the purpose of testing, we're just using guids for the alerts field and grabbing the scores.
    List<String> guids = new ArrayList<>();
    List<Double> threatScores = new ArrayList<>();
    for (Map.Entry<String, String> entry : guidsToIndices.entrySet()) {
      SearchRequest searchRequest = new SearchRequest();
      searchRequest.setIndices(ImmutableList.of(entry.getValue()));
      searchRequest.setQuery("\"query\": \"guid:" + entry.getKey() + "\"");
      try {
        SearchResponse searchResponse = search(searchRequest);
        List<SearchResult> searchResults = searchResponse.getResults();
        if (searchResults.size() > 1) {
          throw new InvalidCreateException(
              "Found more than one result for: " + entry.getKey() + ". Values: " + searchResults
          );
        }

        if (searchResults.size() == 1) {
          SearchResult result = searchResults.get(0);
          Double threatScore = Double.parseDouble(result.getSource().getOrDefault(THREAT_FIELD_DEFAULT, "0").toString());
          guids.add(result.getId());
          threatScores.add(threatScore);
        }
      } catch (InvalidSearchException e) {
        throw new InvalidCreateException("Unable to find guid: " + entry.getKey(), e);
      }
    }

    metaAlert.putAll(new MetaScores(threatScores).getMetaScores());
    results.add(metaAlert.toJSONString());

    backingStore.put(META_INDEX, results);

    MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
    createResponse.setCreated(true);
    return createResponse;
  }

  @Override
  public void init(IndexDao indexDao) {
//    if (!(indexDao instanceof InMemoryDao)) {
//      throw new IllegalArgumentException("InMemoryMetaAlertDao can only be used with InMemoryDao");
//    }
//    this.dao = (InMemoryDao) indexDao;
    // Do nothing.  We don't need the underlying DAO
  }
}
