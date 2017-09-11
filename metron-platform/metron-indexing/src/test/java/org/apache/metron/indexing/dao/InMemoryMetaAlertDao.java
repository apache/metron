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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InMemoryMetaAlertDao implements MetaAlertDao {

  private IndexDao indexDao;

  /**
   * {
   * "indices": ["metaalerts"],
   * "query": "alert|guid:${GUID}",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "guid",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String metaAlertsForAlertQuery;

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
    return indexDao.search(searchRequest);
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
    return indexDao.group(groupRequest);
  }

  @Override
  public void init(AccessConfig config) {
    // Do nothing
  }

  @Override
  public void init(IndexDao indexDao, String threatSort) {
    this.indexDao = indexDao;
    // Ignore threatSort for test.
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    indexDao.update(update, index);
  }

  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException {
    return indexDao.getCommonColumnMetadata(indices);
  }

  @Override
  public Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    return indexDao.getLatestResult(request);
  }

  @Override
  public void patch(PatchRequest request, Optional<Long> timestamp)
      throws OriginalNotFoundException, IOException {
    indexDao.patch(request, timestamp);
  }

  @Override
  public void replace(ReplaceRequest request, Optional<Long> timestamp) throws IOException {
    indexDao.replace(request, timestamp);
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException {
    SearchRequest request;
    try {
      String replacedQuery = metaAlertsForAlertQuery.replace("${GUID}", guid);
      request = JSONUtils.INSTANCE.load(replacedQuery, SearchRequest.class);
    } catch (IOException e) {
      throw new InvalidSearchException("Unable to process query:", e);
    }
    return search(request);
  }

  @SuppressWarnings("unchecked")
  @Override
  public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException {
    if (request.getGuidToIndices().isEmpty()) {
      MetaAlertCreateResponse response = new MetaAlertCreateResponse();
      response.setCreated(false);
      return response;
    }
    // Build meta alert json.  Give it a reasonable GUID
    JSONObject metaAlert = new JSONObject();
    metaAlert.put(Constants.GUID,
        "meta_" + (InMemoryDao.BACKING_STORE.get(MetaAlertDao.METAALERTS_INDEX).size() + 1));

    JSONArray groupsArray = new JSONArray();
    groupsArray.addAll(request.getGroups());
    metaAlert.put(MetaAlertDao.GROUPS_FIELD, groupsArray);

    // Retrieve the alert for each guid
    // For the purpose of testing, we're just using guids for the alerts field and grabbing the scores.
    JSONArray alertArray = new JSONArray();
    List<Double> threatScores = new ArrayList<>();
    for (Map.Entry<String, String> entry : request.getGuidToIndices().entrySet()) {
      SearchRequest searchRequest = new SearchRequest();
      searchRequest.setIndices(ImmutableList.of(entry.getValue()));
      searchRequest.setQuery("guid:" + entry.getKey());
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
          alertArray.add(result.getSource());
          Double threatScore = Double
              .parseDouble(result.getSource().getOrDefault(THREAT_FIELD_DEFAULT, "0").toString());

          threatScores.add(threatScore);
        }
      } catch (InvalidSearchException e) {
        throw new InvalidCreateException("Unable to find guid: " + entry.getKey(), e);
      }
    }

    metaAlert.put(MetaAlertDao.ALERT_FIELD, alertArray);
    metaAlert.putAll(new MetaScores(threatScores).getMetaScores());

    // Add the alert to the store, but make sure not to overwrite existing results
    InMemoryDao.BACKING_STORE.get(MetaAlertDao.METAALERTS_INDEX).add(metaAlert.toJSONString());

    MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
    createResponse.setCreated(true);
    return createResponse;
  }
}
