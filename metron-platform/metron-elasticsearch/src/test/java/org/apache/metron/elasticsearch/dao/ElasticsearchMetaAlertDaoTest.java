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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConfig;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaScores;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.Test;

public class ElasticsearchMetaAlertDaoTest {


  @Test(expected = IllegalArgumentException.class)
  public void testInvalidInit() {
    IndexDao dao = new IndexDao() {
      @Override
      public SearchResponse search(SearchRequest searchRequest) {
        return null;
      }

      @Override
      public GroupResponse group(GroupRequest groupRequest) {
        return null;
      }

      @Override
      public void init(AccessConfig config) {
      }

      @Override
      public Document getLatest(String guid, String sensorType) {
        return null;
      }

      @Override
      public Iterable<Document> getAllLatest(
          List<GetRequest> getRequests) {
        return null;
      }

      @Override
      public void update(Document update, Optional<String> index) {
      }

      @Override
      public void batchUpdate(Map<Document, Optional<String>> updates) {
      }

      @Override
      public Map<String, FieldType> getColumnMetadata(List<String> indices) {
        return null;
      }

      @Override
      public void addCommentToAlert(CommentAddRemoveRequest request) {
      }

      @Override
      public void removeCommentFromAlert(CommentAddRemoveRequest request) {
      }

      @Override
      public void addCommentToAlert(CommentAddRemoveRequest request, Document latest) {
      }

      @Override
      public void removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) {
      }
    };
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.init(dao);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitInvalidDao() {
    HBaseDao dao = new HBaseDao();
    ElasticsearchMetaAlertDao esDao = new ElasticsearchMetaAlertDao();
    esDao.init(dao, Optional.empty());
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
    MultiIndexDao miDao = new MultiIndexDao(esDao);
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(miDao);

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
    docMap.put(MetaAlertConstants.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertConstants.METAALERT_TYPE, 0L);

    // calculate the threat score for the metaalert
    MetaScores.calculateMetaScores(metaalert, MetaAlertConstants.THREAT_FIELD_DEFAULT, MetaAlertConstants.THREAT_SORT_DEFAULT);
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
    alertList.add( Collections.singletonMap(MetaAlertConstants.THREAT_FIELD_DEFAULT, 10.0f));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertConstants.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertConstants.METAALERT_TYPE, 0L);

    // Configure a different threat triage score field name
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() -> new HashMap<String, Object>() {{
      put(Constants.THREAT_SCORE_FIELD_PROPERTY, MetaAlertConstants.THREAT_FIELD_DEFAULT);
    }});
    ElasticsearchDao elasticsearchDao = new ElasticsearchDao();
    elasticsearchDao.setAccessConfig(accessConfig);

    // calculate the threat score for the metaalert
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.init(elasticsearchDao);
    MetaScores.calculateMetaScores(metaalert, MetaAlertConstants.THREAT_FIELD_DEFAULT, MetaAlertConstants.THREAT_SORT_DEFAULT);
    assertNotNull(metaalert.getDocument().get(MetaAlertConstants.THREAT_FIELD_DEFAULT));
  }

  @Test
  public void testUpdateShouldUpdateOnMissingMetaAlertIndex() throws Exception {
    ElasticsearchDao elasticsearchDao = mock(ElasticsearchDao.class);
    ElasticsearchMetaAlertRetrieveLatestDao elasticsearchMetaAlertRetrieveLatestDao = mock(ElasticsearchMetaAlertRetrieveLatestDao.class);
    MetaAlertConfig metaAlertConfig = mock(MetaAlertConfig.class);
    ElasticsearchMetaAlertUpdateDao emauDao = spy(new ElasticsearchMetaAlertUpdateDao(elasticsearchDao, elasticsearchMetaAlertRetrieveLatestDao, metaAlertConfig, 1));

    doThrow(new IndexNotFoundException(ElasticsearchMetaAlertDao.METAALERTS_INDEX)).when(emauDao).getMetaAlertsForAlert("alert_one");

    Document update = new Document(new HashMap<>(), "alert_one", "", 0L);
    emauDao.update(update, Optional.empty());

    Map<Document, Optional<String>> expectedUpdate = new HashMap<Document, Optional<String>>() {{
      put(update, Optional.empty());
    }};
    verify(elasticsearchDao).batchUpdate(expectedUpdate);
  }

  @Test(expected = IndexNotFoundException.class)
  public void testUpdateShouldThrowExceptionOnMissingSensorIndex() throws Exception {
    ElasticsearchDao elasticsearchDao = mock(ElasticsearchDao.class);
    ElasticsearchMetaAlertRetrieveLatestDao elasticsearchMetaAlertRetrieveLatestDao = mock(ElasticsearchMetaAlertRetrieveLatestDao.class);
    MetaAlertConfig metaAlertConfig = mock(MetaAlertConfig.class);
    ElasticsearchMetaAlertUpdateDao emauDao = spy(new ElasticsearchMetaAlertUpdateDao(elasticsearchDao, elasticsearchMetaAlertRetrieveLatestDao, metaAlertConfig, 1));

    doThrow(new IndexNotFoundException("bro")).when(emauDao).getMetaAlertsForAlert("alert_one");

    Document update = new Document(new HashMap<>(), "alert_one", "", 0L);
    emauDao.update(update, Optional.empty());
  }
}
