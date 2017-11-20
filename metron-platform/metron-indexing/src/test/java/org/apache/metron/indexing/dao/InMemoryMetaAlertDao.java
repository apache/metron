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

import static org.apache.metron.common.Constants.GUID;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
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

  public static Map<String, Collection<String>> METAALERT_STORE = new HashMap<>();

  private IndexDao indexDao;

  /**
   * {
   * "indices": ["metaalert"],
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
  public void init(IndexDao indexDao, Optional<String> threatSort) {
    this.indexDao = indexDao;
    // Ignore threatSort for test.
  }

  @Override
  public Document getLatest(String guid, String sensorType) throws IOException {
    return indexDao.getLatest(guid, sensorType);
  }

  @Override
  public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
    return indexDao.getAllLatest(getRequests);
  }

  @Override
  public void update(Document update, Optional<String> index) throws IOException {
    indexDao.update(update, index);
  }

  @Override
  public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
    throw new UnsupportedOperationException("InMemoryMetaAlertDao can't do bulk updates");
  }

  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices)
      throws IOException {
    return indexDao.getColumnMetadata(indices);
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
    List<GetRequest> alertRequests = request.getAlerts();
    if (alertRequests.isEmpty()) {
      MetaAlertCreateResponse response = new MetaAlertCreateResponse();
      response.setCreated(false);
      return response;
    }
    // Build meta alert json.  Give it a reasonable GUID
    JSONObject metaAlert = new JSONObject();
    String metaAlertGuid = "meta_" + (InMemoryDao.BACKING_STORE.get(MetaAlertDao.METAALERTS_INDEX).size() + 1);
    metaAlert.put(GUID, metaAlertGuid);

    JSONArray groupsArray = new JSONArray();
    groupsArray.addAll(request.getGroups());
    metaAlert.put(MetaAlertDao.GROUPS_FIELD, groupsArray);

    // Retrieve the alert for each guid
    // For the purpose of testing, we're just using guids for the alerts field and grabbing the scores.
    JSONArray alertArray = new JSONArray();
    List<Double> threatScores = new ArrayList<>();
    Collection<String> alertGuids = new ArrayList<>();
    for (GetRequest alertRequest : alertRequests) {
      SearchRequest searchRequest = new SearchRequest();
      searchRequest.setIndices(ImmutableList.of(alertRequest.getIndex().get()));
      searchRequest.setQuery("guid:" + alertRequest.getGuid());
      try {
        SearchResponse searchResponse = search(searchRequest);
        List<SearchResult> searchResults = searchResponse.getResults();
        if (searchResults.size() > 1) {
          throw new InvalidCreateException(
              "Found more than one result for: " + alertRequest.getGuid() + ". Values: " + searchResults
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
        throw new InvalidCreateException("Unable to find guid: " + alertRequest.getGuid(), e);
      }
      alertGuids.add(alertRequest.getGuid());
    }

    metaAlert.put(MetaAlertDao.ALERT_FIELD, alertArray);
    metaAlert.putAll(new MetaScores(threatScores).getMetaScores());
    metaAlert.put(STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    // Add the alert to the store, but make sure not to overwrite existing results
    InMemoryDao.BACKING_STORE.get(MetaAlertDao.METAALERTS_INDEX).add(metaAlert.toJSONString());

    METAALERT_STORE.put(metaAlertGuid, new HashSet<>(alertGuids));

    MetaAlertCreateResponse createResponse = new MetaAlertCreateResponse();
    createResponse.setGuid(metaAlertGuid);
    createResponse.setCreated(true);
    return createResponse;
  }

  @Override
  public boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests) throws IOException {
    Collection<String> currentAlertGuids = METAALERT_STORE.get(metaAlertGuid);
    if (currentAlertGuids == null) {
      return false;
    }
    Collection<String> alertGuids = alertRequests.stream().map(GetRequest::getGuid).collect(Collectors.toSet());
    boolean added = currentAlertGuids.addAll(alertGuids);
    if (added) {
      METAALERT_STORE.put(metaAlertGuid, currentAlertGuids);
    }
    return added;
  }

  @Override
  public boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests) throws IOException {
    Collection<String> currentAlertGuids = METAALERT_STORE.get(metaAlertGuid);
    if (currentAlertGuids == null) {
      return false;
    }
    Collection<String> alertGuids = alertRequests.stream().map(GetRequest::getGuid).collect(Collectors.toSet());
    boolean removed = currentAlertGuids.removeAll(alertGuids);
    if (removed) {
      METAALERT_STORE.put(metaAlertGuid, currentAlertGuids);
    }
    return removed;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException {
    boolean statusChanged = false;
    List<String> metaAlerts = InMemoryDao.BACKING_STORE.get(MetaAlertDao.METAALERTS_INDEX);
    for (String metaAlert: metaAlerts) {
      JSONObject metaAlertJSON = JSONUtils.INSTANCE.load(metaAlert, JSONObject.class);
      if (metaAlertGuid.equals(metaAlertJSON.get(GUID))) {
        statusChanged = !status.getStatusString().equals(metaAlertJSON.get(STATUS_FIELD));
        if (statusChanged) {
          metaAlertJSON.put(STATUS_FIELD, status.getStatusString());
          metaAlerts.remove(metaAlert);
          metaAlerts.add(metaAlertJSON.toJSONString());
          InMemoryDao.BACKING_STORE.put(MetaAlertDao.METAALERTS_INDEX, metaAlerts);
        }
        break;
      }
    }
    return statusChanged;
  }

  public static void clear() {
    InMemoryDao.clear();
    METAALERT_STORE.clear();
  }

}
