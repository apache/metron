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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.Test;

public class AbstractMetaAlertDaoTest {

  private class TestMetaAlertDao extends AbstractMetaAlertDao {

    @Override
    public SearchResponse getAllMetaAlertsForAlert(String guid) {
      return null;
    }

    @Override
    public MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request) {
      return null;
    }

    @Override
    public void init(IndexDao indexDao, Optional<String> threatSort) {
    }

    @Override
    public int getPageSize() {
      return 0;
    }

    @Override
    public void setPageSize(int pageSize) {

    }

    @Override
    public void init(AccessConfig config) {
    }

    @Override
    public SearchResponse search(SearchRequest searchRequest) {
      return null;
    }

    @Override
    public GroupResponse group(GroupRequest groupRequest) {
      return null;
    }


    @Override
    public void update(Document update, Optional<String> index) {
    }

    @Override
    public Map<String, FieldType> getColumnMetadata(List<String> indices) {
      return null;
    }
  }

  @Test
  public void testCalculateMetaScoresList() {
    final double delta = 0.001;
    List<Map<String, Object>> alertList = new ArrayList<>();

    // add an alert with a threat score
    alertList.add(Collections.singletonMap(MetaAlertDao.THREAT_FIELD_DEFAULT, 10.0f));

    // add a second alert with a threat score
    alertList.add(Collections.singletonMap(MetaAlertDao.THREAT_FIELD_DEFAULT, 20.0f));

    // add a third alert with NO threat score
    alertList.add(Collections.singletonMap("alert3", "has no threat score"));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertDao.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertDao.METAALERT_TYPE, 0L);

    // calculate the threat score for the metaalert
    TestMetaAlertDao metaAlertDao = new TestMetaAlertDao();
    metaAlertDao.calculateMetaScores(metaalert);

    // the metaalert must contain a summary of all child threat scores
    assertEquals(20D, (Double) metaalert.getDocument().get("max"), delta);
    assertEquals(10D, (Double) metaalert.getDocument().get("min"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("average"), delta);
    assertEquals(2L, metaalert.getDocument().get("count"));
    assertEquals(30D, (Double) metaalert.getDocument().get("sum"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("median"), delta);

    // it must contain an overall threat score; a float to match the type of the threat score of
    // the other sensor indices
    Object threatScore = metaalert.getDocument().get(TestMetaAlertDao.THREAT_FIELD_DEFAULT);
    assertTrue(threatScore instanceof Float);

    // by default, the overall threat score is the sum of all child threat scores
    assertEquals(30.0F, threatScore);
  }

  @Test
  public void testBuildCreateDocumentSingleAlert() {
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
    TestMetaAlertDao metaAlertDao = new TestMetaAlertDao();
    Document actual = metaAlertDao.buildCreateDocument(alerts, groups);

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
  public void testBuildCreateDocumentMultipleAlerts() {
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
    List<Document> alerts = new ArrayList<>();
    alerts.add(new Document(alertOne, "", "", 0L));
    alerts.add(new Document(alertTwo, "", "", 0L));

    // Actually build the doc
    TestMetaAlertDao metaAlertDao = new TestMetaAlertDao();
    Document actual = metaAlertDao.buildCreateDocument(alerts, groups);

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
}
