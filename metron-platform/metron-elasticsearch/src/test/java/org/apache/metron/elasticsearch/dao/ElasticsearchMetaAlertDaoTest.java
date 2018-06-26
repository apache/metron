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

import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERTS_INDEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.Test;

public class ElasticsearchMetaAlertDaoTest {



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
      public Iterable<Document> getAllLatest(
          List<GetRequest> getRequests) throws IOException {
        return null;
      }

      @Override
      public void update(Document update, Optional<String> index) throws IOException {
      }

      @Override
      public void batchUpdate(Map<Document, Optional<String>> updates) throws IOException {
      }

      @Override
      public Map<String, FieldType> getColumnMetadata(List<String> indices)
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
    List<Document> alerts = new ArrayList<Document>() {{
      add(new Document(alertOne, "", "", 0L));
    }};

    // Actually build the doc
    Document actual = emaDao.buildCreateDocument(alerts, groups);

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

    // Build the second response from the multiget
    Map<String, Object> alertTwo = new HashMap<>();
    alertTwo.put(Constants.GUID, "alert_one");
    alertTwo.put(MetaAlertDao.THREAT_FIELD_DEFAULT, 5.0d);
    List<Document> alerts = new ArrayList<Document>() {{
      add(new Document(alertOne, "", "", 0L));
      add(new Document(alertTwo, "", "", 0L));
    }};

    // Actually build the doc
    Document actual = emaDao.buildCreateDocument(alerts, groups);

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
    createRequest.setAlerts(Collections.singletonList(new GetRequest("don't", "care")));
    emaDao.createMetaAlert(createRequest);
  }

  @Test
  public void testCalculateMetaScoresList() {
    final double delta = 0.001;
    List<Map<String, Object>> alertList = new ArrayList<>();

    // add an alert with a threat score
    alertList.add( Collections.singletonMap(ElasticsearchMetaAlertDao.THREAT_TRIAGE_FIELD, 10.0f));

    // add a second alert with a threat score
    alertList.add( Collections.singletonMap(ElasticsearchMetaAlertDao.THREAT_TRIAGE_FIELD, 20.0f));

    // add a third alert with NO threat score
    alertList.add( Collections.singletonMap("alert3", "has no threat score"));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertDao.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    // calculate the threat score for the metaalert
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.calculateMetaScores(metaalert);
    Object threatScore = metaalert.getDocument().get(ElasticsearchMetaAlertDao.THREAT_TRIAGE_FIELD);

    // the metaalert must contain a summary of all child threat scores
    assertEquals(20D, (Double) metaalert.getDocument().get("max"), delta);
    assertEquals(10D, (Double) metaalert.getDocument().get("min"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("average"), delta);
    assertEquals(2L, metaalert.getDocument().get("count"));
    assertEquals(30D, (Double) metaalert.getDocument().get("sum"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("median"), delta);

    // it must contain an overall threat score; a float to match the type of the threat score of the other sensor indices
    assertTrue(threatScore instanceof Float);

    // by default, the overall threat score is the sum of all child threat scores
    assertEquals(30.0F, threatScore);
  }

  @Test
  public void testCalculateMetaScoresWithDifferentFieldName() {
    List<Map<String, Object>> alertList = new ArrayList<>();

    // add an alert with a threat score
    alertList.add( Collections.singletonMap(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0f));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertDao.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    // Configure a different threat triage score field name
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() -> new HashMap<String, Object>() {{
      put(MetaAlertDao.THREAT_FIELD_PROPERTY, MetaAlertDao.THREAT_FIELD_DEFAULT);
    }});
    ElasticsearchDao elasticsearchDao = new ElasticsearchDao();
    elasticsearchDao.setAccessConfig(accessConfig);

    // calculate the threat score for the metaalert
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.init(elasticsearchDao);
    metaAlertDao.calculateMetaScores(metaalert);
    assertNotNull(metaalert.getDocument().get(MetaAlertDao.THREAT_FIELD_DEFAULT));
  }

  @Test
  public void testUpdateShouldUpdateOnMissingMetaAlertIndex() throws Exception {
    ElasticsearchDao elasticsearchDao = mock(ElasticsearchDao.class);
    ElasticsearchMetaAlertDao emaDao = spy(new ElasticsearchMetaAlertDao(elasticsearchDao));

    doThrow(new IndexNotFoundException(METAALERTS_INDEX)).when(emaDao).getMetaAlertsForAlert("alert_one");

    Document update = new Document(new HashMap<>(), "alert_one", "", 0L);
    emaDao.update(update, Optional.empty());

    Map<Document, Optional<String>> expectedUpdate = new HashMap<Document, Optional<String>>() {{
      put(update, Optional.empty());
    }};
    verify(elasticsearchDao).batchUpdate(expectedUpdate);
  }

  @Test(expected = IndexNotFoundException.class)
  public void testUpdateShouldThrowExceptionOnMissingSensorIndex() throws Exception {
    ElasticsearchMetaAlertDao emaDao = spy(new ElasticsearchMetaAlertDao());

    doThrow(new IndexNotFoundException("bro")).when(emaDao).getMetaAlertsForAlert("alert_one");

    Document update = new Document(new HashMap<>(), "alert_one", "", 0L);
    emaDao.update(update, Optional.empty());
  }
}
