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

package org.apache.metron.elasticsearch.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class ElasticsearchMetaAlertDaoTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildUpdatedMetaAlertSingleAlert() throws IOException, ParseException {
    // Construct the expected result
    JSONObject expected = new JSONObject();
    expected.put("average", 5.0);
    expected.put("min", 5.0);
    expected.put("median", 5.0);
    expected.put("max", 5.0);
    expected.put("count", 1L);
    expected.put(Constants.GUID, "m1");
    expected.put("sum", 5.0);
    expected.put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    JSONArray expectedAlerts = new JSONArray();
    JSONObject expectedAlert = new JSONObject();
    expectedAlert.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 5L);
    expectedAlert.put("fakekey", "fakevalue");
    expectedAlerts.add(expectedAlert);
    expected.put(MetaAlertDao.ALERT_FIELD, expectedAlerts);

    // Construct the meta alert object
    Map<String, Object> metaSource = new HashMap<>();
    metaSource.put(Constants.GUID, "m1");
    metaSource.put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    List<Double> alertScores = new ArrayList<>();
    alertScores.add(10d);
    metaSource.putAll(new MetaScores(alertScores).getMetaScores());
    SearchHit metaHit = mock(SearchHit.class);
    when(metaHit.getSource()).thenReturn(metaSource);

    // Construct the inner alert
    HashMap<String, Object> innerAlertSource = new HashMap<>();
    innerAlertSource.put(Constants.GUID, "a1");
    innerAlertSource.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10d);

    Map<String, Object> innerHits = new HashMap<>();
    innerHits.put(MetaAlertDao.ALERT_FIELD, Collections.singletonList(innerAlertSource));
    when(metaHit.sourceAsMap()).thenReturn(innerHits);

    // Construct  the updated Document
    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 5);
    updateMap.put("fakekey", "fakevalue");
    Document update = new Document(updateMap, "a1", "bro_doc", 0L);

    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);
    XContentBuilder builder = emaDao.buildUpdatedMetaAlert(update, metaHit);
    JSONParser parser = new JSONParser();
    Object obj = parser.parse(builder.string());
    JSONObject actual = (JSONObject) obj;

    assertEquals(expected, actual);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testBuildUpdatedMetaAlertMultipleAlerts() throws IOException, ParseException {
    // Construct the expected result
    JSONObject expected = new JSONObject();
    expected.put("average", 7.5);
    expected.put("min", 5.0);
    expected.put("median", 7.5);
    expected.put("max", 10.0);
    expected.put("count", 2L);
    expected.put(Constants.GUID, "m1");
    expected.put("sum", 15.0);
    expected.put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    JSONArray expectedAlerts = new JSONArray();
    JSONObject expectedAlertOne = new JSONObject();
    expectedAlertOne.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 5d);
    expectedAlertOne.put("fakekey", "fakevalue");
    expectedAlerts.add(expectedAlertOne);
    JSONObject expectedAlertTwo = new JSONObject();
    expectedAlertTwo.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10d);
    String guidTwo = "a2";
    expectedAlertTwo.put(Constants.GUID, guidTwo);
    expectedAlerts.add(expectedAlertTwo);
    expected.put(MetaAlertDao.ALERT_FIELD, expectedAlerts);

    // Construct the meta alert object
    Map<String, Object> metaSource = new HashMap<>();
    metaSource.put(Constants.GUID, "m1");
    metaSource.put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    double threatValueOne = 5d;
    double threatValueTwo = 10d;
    List<Double> alertScores = new ArrayList<>();
    alertScores.add(threatValueOne);
    alertScores.add(threatValueTwo);
    metaSource.putAll(new MetaScores(alertScores).getMetaScores());
    SearchHit metaHit = mock(SearchHit.class);
    when(metaHit.getSource()).thenReturn(metaSource);

    // Construct the inner alerts
    HashMap<String, Object> innerAlertSourceOne = new HashMap<>();
    String guidOne = "a1";
    innerAlertSourceOne.put(Constants.GUID, guidOne);
    innerAlertSourceOne.put(MetaAlertDao.THREAT_FIELD_DEFAULT, threatValueTwo);

    HashMap<String, Object> innerAlertSourceTwo = new HashMap<>();
    innerAlertSourceTwo.put(Constants.GUID, guidTwo);
    innerAlertSourceTwo.put(MetaAlertDao.THREAT_FIELD_DEFAULT, threatValueTwo);

    Map<String, Object> innerHits = new HashMap<>();
    innerHits.put(MetaAlertDao.ALERT_FIELD, Arrays.asList(innerAlertSourceOne, innerAlertSourceTwo));
    when(metaHit.sourceAsMap()).thenReturn(innerHits);

    // Construct  the updated Document
    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put(MetaAlertDao.THREAT_FIELD_DEFAULT, threatValueOne);
    updateMap.put("fakekey", "fakevalue");
    Document update = new Document(updateMap, guidOne, "bro_doc", 0L);

    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    MultiIndexDao multiIndexDao = new MultiIndexDao(esDao);
    emaDao.init(multiIndexDao);
    XContentBuilder builder = emaDao.buildUpdatedMetaAlert(update, metaHit);

    JSONParser parser = new JSONParser();
    Object obj = parser.parse(builder.string());
    JSONObject actual = (JSONObject) obj;

    assertEquals(expected, actual);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInit() {
    IndexDao dao = new IndexDao() {
      @Override
      public SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException {
        return null;
      }

      @Override
      public GroupResponse group(GroupRequest groupRequest) throws InvalidSearchException {
        return null;
      }

      @Override
      public void init(AccessConfig config) {
      }

      @Override
      public Document getLatest(String guid, String sensorType) throws IOException {
        return null;
      }

      @Override
      public void update(Document update, Optional<String> index) throws IOException {
      }

      @Override
      public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices)
          throws IOException {
        return null;
      }

      @Override
      public Map<String, FieldType> getCommonColumnMetadata(List<String> indices)
          throws IOException {
        return null;
      }
    };
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.init(dao);
  }

  @Test
  public void testBuildCreateDocumentSingleAlert() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);

    List<String> groups = new ArrayList<>();
    groups.add("group_one");
    groups.add("group_two");

    // Build the first response from the multiget
    Map<String, Object> alertOne = new HashMap<>();
    alertOne.put(Constants.GUID, "alert_one");
    alertOne.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
    GetResponse getResponseOne = mock(GetResponse.class);
    when(getResponseOne.isExists()).thenReturn(true);
    when(getResponseOne.getSource()).thenReturn(alertOne);
    MultiGetItemResponse multiGetItemResponseOne = mock(MultiGetItemResponse.class);
    when(multiGetItemResponseOne.getResponse()).thenReturn(getResponseOne);

    // Add it to the iterator
    @SuppressWarnings("unchecked")
    Iterator<MultiGetItemResponse> mockIterator = mock(Iterator.class);
    when(mockIterator.hasNext()).thenReturn(true, false);
    when(mockIterator.next()).thenReturn(multiGetItemResponseOne);

    // Add it to the response
    MultiGetResponse mockResponse = mock(MultiGetResponse.class);
    when(mockResponse.iterator()).thenReturn(mockIterator);

    // Actually build the doc
    Document actual = emaDao.buildCreateDocument(mockResponse, groups);

    ArrayList<Map<String, Object>> alertList = new ArrayList<>();
    alertList.add(alertOne);

    Map<String, Object> actualDocument = actual.getDocument();
    assertEquals(
        MetaAlertStatus.ACTIVE.getStatusString(),
        actualDocument.get(MetaAlertDao.STATUS_FIELD)
    );
    assertEquals(
        alertList,
        actualDocument.get(MetaAlertDao.ALERT_FIELD)
    );
    assertEquals(
        groups,
        actualDocument.get(MetaAlertDao.GROUPS_FIELD)
    );

    // Don't care about the result, just that it's a UUID. Exception will be thrown if not.
    UUID.fromString((String) actualDocument.get(Constants.GUID));
  }

  @Test
  public void testBuildCreateDocumentMultipleAlerts() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);

    List<String> groups = new ArrayList<>();
    groups.add("group_one");
    groups.add("group_two");

    // Build the first response from the multiget
    Map<String, Object> alertOne = new HashMap<>();
    alertOne.put(Constants.GUID, "alert_one");
    alertOne.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
    GetResponse getResponseOne = mock(GetResponse.class);
    when(getResponseOne.isExists()).thenReturn(true);
    when(getResponseOne.getSource()).thenReturn(alertOne);
    MultiGetItemResponse multiGetItemResponseOne = mock(MultiGetItemResponse.class);
    when(multiGetItemResponseOne.getResponse()).thenReturn(getResponseOne);

    // Build the second response from the multiget
    Map<String, Object> alertTwo = new HashMap<>();
    alertTwo.put(Constants.GUID, "alert_one");
    alertTwo.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 5.0d);
    GetResponse getResponseTwo = mock(GetResponse.class);
    when(getResponseTwo.isExists()).thenReturn(true);
    when(getResponseTwo.getSource()).thenReturn(alertTwo);
    MultiGetItemResponse multiGetItemResponseTwo = mock(MultiGetItemResponse.class);
    when(multiGetItemResponseTwo.getResponse()).thenReturn(getResponseTwo);

    // Add it to the iterator
    @SuppressWarnings("unchecked")
    Iterator<MultiGetItemResponse> mockIterator = mock(Iterator.class);
    when(mockIterator.hasNext()).thenReturn(true, true, false);
    when(mockIterator.next()).thenReturn(multiGetItemResponseOne, multiGetItemResponseTwo);

    // Add them to the response
    MultiGetResponse mockResponse = mock(MultiGetResponse.class);
    when(mockResponse.iterator()).thenReturn(mockIterator);

    // Actually build the doc
    Document actual = emaDao.buildCreateDocument(mockResponse, groups);

    ArrayList<Map<String, Object>> alertList = new ArrayList<>();
    alertList.add(alertOne);
    alertList.add(alertTwo);

    Map<String, Object> actualDocument = actual.getDocument();
    assertNotNull(actualDocument.get(Fields.TIMESTAMP.getName()));
    assertEquals(
        alertList,
        actualDocument.get(MetaAlertDao.ALERT_FIELD)
    );
    assertEquals(
        groups,
        actualDocument.get(MetaAlertDao.GROUPS_FIELD)
    );

    // Don't care about the result, just that it's a UUID. Exception will be thrown if not.
    UUID.fromString((String) actualDocument.get(Constants.GUID));
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGuids() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    emaDao.createMetaAlert(createRequest);
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGroups() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    HashMap<String, String> guidsToGroups = new HashMap<>();
    guidsToGroups.put("don't", "care");
    createRequest.setGuidToIndices(guidsToGroups);
    emaDao.createMetaAlert(createRequest);
  }

  @Test
  public void testCalculateMetaScoresList() {
    List<Map<String, Object>> alertList = new ArrayList<>();
    Map<String, Object> alertMap = new HashMap<>();
    alertMap.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
    alertList.add(alertMap);
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertDao.ALERT_FIELD, alertList);

    Document doc = new Document(docMap, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    List<Double> scores = new ArrayList<>();
    scores.add(10.0d);
    MetaScores expected = new MetaScores(scores);

    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    MetaScores actual = metaAlertDao.calculateMetaScores(doc);
    assertEquals(expected.getMetaScores(), actual.getMetaScores());
  }

  @Test
  public void testHandleMetaUpdateNonAlert() throws IOException {
    ElasticsearchDao mockEsDao= mock(ElasticsearchDao.class);

    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertDao.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());
    Document update = new Document(docMap, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao(mockEsDao);
    metaAlertDao.handleMetaUpdate(update, Optional.of(MetaAlertDao.METAALERTS_INDEX));
    verify(mockEsDao, times(1))
        .update(update, Optional.of(MetaAlertDao.METAALERTS_INDEX));
  }

  @Test
  public void testHandleMetaUpdateAlert() throws IOException {
    ElasticsearchDao mockEsDao= mock(ElasticsearchDao.class);

    Map<String, Object> alertMap = new HashMap<>();
    alertMap.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
    List<Map<String, Object>> alertList = new ArrayList<>();
    alertList.add(alertMap);

    Map<String, Object> docMapBefore = new HashMap<>();
    docMapBefore.put(MetaAlertDao.ALERT_FIELD, alertList);
    Document before = new Document(docMapBefore, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    Map<String, Object> docMapAfter = new HashMap<>();
    docMapAfter.putAll(docMapBefore);
    docMapAfter.put("average", 10.0d);
    docMapAfter.put("min", 10.0d);
    docMapAfter.put("median", 10.0d);
    docMapAfter.put("max", 10.0d);
    docMapAfter.put("count", 1L);
    docMapAfter.put("sum", 10.0d);
    docMapAfter.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0d);
    Document after = new Document(docMapAfter, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao(mockEsDao);
    metaAlertDao.handleMetaUpdate(before, Optional.of(MetaAlertDao.METAALERTS_INDEX));

    verify(mockEsDao, times(1))
        .update(after, Optional.of(MetaAlertDao.METAALERTS_INDEX));
  }
}
