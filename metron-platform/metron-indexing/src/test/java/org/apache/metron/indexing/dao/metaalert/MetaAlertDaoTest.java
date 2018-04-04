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

package org.apache.metron.indexing.dao.metaalert;

import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.ALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.GROUPS_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.STATUS_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_FIELD_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.metron.common.Constants;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.Test;

public class MetaAlertDaoTest {

  private class TestMetaAlertDao implements MetaAlertDao {

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
    public IndexDao getIndexDao() {
      return null;
    }

    @Override
    public String getMetAlertSensorName() {
      return METAALERT_TYPE;
    }

    @Override
    public String getMetaAlertIndex() {
      return "metaalert_index";
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

    public String getChildField() {
      return ALERT_FIELD;
    }

    @Override
    public Document getLatest(String guid, String sensorType) throws IOException {
      return null;
    }

    @Override
    public Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException {
      return null;
    }
  }

  @Test
  public void testBuildCreateDocumentSingleAlert() {
    List<String> groups = new ArrayList<>();
    groups.add("group_one");
    groups.add("group_two");

    // Build the first response from the multiget
    Map<String, Object> alertOne = new HashMap<>();
    alertOne.put(Constants.GUID, "alert_one");
    alertOne.put(THREAT_FIELD_DEFAULT, 10.0d);
    List<Document> alerts = new ArrayList<Document>() {{
      add(new Document(alertOne, "", "", 0L));
    }};

    // Actually build the doc
    TestMetaAlertDao metaAlertDao = new TestMetaAlertDao();
    Document actual = metaAlertDao
        .buildCreateDocument(alerts, groups, metaAlertDao.getChildField());

    ArrayList<Map<String, Object>> alertList = new ArrayList<>();
    alertList.add(alertOne);

    Map<String, Object> actualDocument = actual.getDocument();
    assertEquals(
        MetaAlertStatus.ACTIVE.getStatusString(),
        actualDocument.get(STATUS_FIELD)
    );
    assertEquals(
        alertList,
        actualDocument.get(ALERT_FIELD)
    );
    assertEquals(
        groups,
        actualDocument.get(GROUPS_FIELD)
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
    alertOne.put(THREAT_FIELD_DEFAULT, 10.0d);

    // Build the second response from the multiget
    Map<String, Object> alertTwo = new HashMap<>();
    alertTwo.put(Constants.GUID, "alert_one");
    alertTwo.put(THREAT_FIELD_DEFAULT, 5.0d);
    List<Document> alerts = new ArrayList<>();
    alerts.add(new Document(alertOne, "", "", 0L));
    alerts.add(new Document(alertTwo, "", "", 0L));

    // Actually build the doc
    TestMetaAlertDao metaAlertDao = new TestMetaAlertDao();
    Document actual = metaAlertDao
        .buildCreateDocument(alerts, groups, metaAlertDao.getChildField());

    ArrayList<Map<String, Object>> alertList = new ArrayList<>();
    alertList.add(alertOne);
    alertList.add(alertTwo);

    Map<String, Object> actualDocument = actual.getDocument();
    assertNotNull(actualDocument.get(Fields.TIMESTAMP.getName()));
    assertEquals(
        alertList,
        actualDocument.get(ALERT_FIELD)
    );
    assertEquals(
        groups,
        actualDocument.get(GROUPS_FIELD)
    );

    // Don't care about the result, just that it's a UUID. Exception will be thrown if not.
    UUID.fromString((String) actualDocument.get(Constants.GUID));
  }
}
