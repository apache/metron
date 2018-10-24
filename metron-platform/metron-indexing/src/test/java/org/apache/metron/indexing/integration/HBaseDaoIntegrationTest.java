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

package org.apache.metron.indexing.integration;

import static org.apache.metron.indexing.dao.HBaseDao.HBASE_CF;
import static org.apache.metron.indexing.dao.HBaseDao.HBASE_TABLE;
import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HBaseDaoIntegrationTest extends UpdateIntegrationTest  {

  private static final String TABLE_NAME = "metron_update";
  private static final String COLUMN_FAMILY = "cf";
  private static final String SENSOR_TYPE = "test";

  private static IndexDao hbaseDao;
  private static byte[] expectedKeySerialization = new byte[] {
          (byte)0xf5,0x53,0x76,(byte)0x96,0x67,0x3a,
          (byte)0xc1,(byte)0xaf,(byte)0xff,0x41,0x33,(byte)0x9d,
          (byte)0xac,(byte)0xb9,0x1a,(byte)0xb0,0x00,0x04,
          0x67,0x75,0x69,0x64,0x00,0x0a,
          0x73,0x65,0x6e,0x73,0x6f,0x72,
          0x54,0x79,0x70,0x65
  };

  @Before
  public void startHBase() throws Exception {
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setMaxSearchResults(1000);
    accessConfig.setMaxSearchGroups(1000);
    accessConfig.setGlobalConfigSupplier(() -> new HashMap<String, Object>() {{
      put(HBASE_TABLE, TABLE_NAME);
      put(HBASE_CF, COLUMN_FAMILY);
    }});
    MockHBaseTableProvider.addToCache(TABLE_NAME, COLUMN_FAMILY);
    accessConfig.setTableProvider(new MockHBaseTableProvider());

    hbaseDao = new HBaseDao();
    hbaseDao.init(accessConfig);
  }

  @After
  public void clearTable() throws Exception {
    MockHBaseTableProvider.clear();
  }

  /**
   * IF this test fails then you have broken the key serialization in that your change has
   * caused a key to change serialization, so keys from previous releases will not be able to be found
   * under your scheme.  Please either provide a migration plan or undo this change.  DO NOT CHANGE THIS
   * TEST BLITHELY!
   * @throws Exception
   */
  @Test
  public void testKeySerializationRemainsConstant() throws IOException {
    HBaseDao.Key k = new HBaseDao.Key("guid", "sensorType");
    byte[] raw = k.toBytes();
    Assert.assertArrayEquals(raw, expectedKeySerialization);
  }


  @Test
  public void testKeySerialization() throws Exception {
    HBaseDao.Key k = new HBaseDao.Key("guid", "sensorType");
    Assert.assertEquals(k, HBaseDao.Key.fromBytes(HBaseDao.Key.toBytes(k)));
  }

  @Test(expected = IllegalStateException.class)
  public void testKeySerializationWithInvalidGuid() throws Exception {
    HBaseDao.Key k = new HBaseDao.Key(null, "sensorType");
    Assert.assertEquals(k, HBaseDao.Key.fromBytes(HBaseDao.Key.toBytes(k)));
  }

  @Test(expected = IllegalStateException.class)
  public void testKeySerializationWithInvalidSensorType() throws Exception {
    HBaseDao.Key k = new HBaseDao.Key("guid", null);
    Assert.assertEquals(k, HBaseDao.Key.fromBytes(HBaseDao.Key.toBytes(k)));
  }

  @Test
  public void shouldGetLatest() throws Exception {
    // Load alerts
    List<Document> alerts = buildAlerts(3);
    Map<Document, Optional<String>> updates = alerts.stream()
        .collect(Collectors.toMap(document -> document, document -> Optional.empty()));
    hbaseDao.batchUpdate(updates);

    Document actualDocument = hbaseDao.getLatest("message_1", SENSOR_TYPE);
    Document expectedDocument = alerts.get(1);
    Assert.assertEquals(expectedDocument, actualDocument);
  }

  @Test
  public void shouldGetLatestWithInvalidTimestamp() throws Exception {
    // Load alert
    Document alert = buildAlerts(1).get(0);
    hbaseDao.update(alert, Optional.empty());

    Document actualDocument = hbaseDao.getLatest("message_0", SENSOR_TYPE);
    Assert.assertEquals(alert, actualDocument);

    alert.getDocument().put("field", "value");
    alert.setTimestamp(0L);
    hbaseDao.update(alert, Optional.empty());

    actualDocument = hbaseDao.getLatest("message_0", SENSOR_TYPE);
    Assert.assertEquals(alert.getDocument(), actualDocument.getDocument());
  }

  @Test
  public void shouldGetAllLatest() throws Exception {
    // Load alerts
    List<Document> alerts = buildAlerts(15);
    alerts.stream().collect(Collectors.toMap(Document::getGuid, document -> Optional.empty()));
    Map<Document, Optional<String>> updates = alerts.stream()
        .collect(Collectors.toMap(document -> document, document -> Optional.empty()));
    hbaseDao.batchUpdate(updates);

    int expectedCount = 12;
    List<GetRequest> getRequests = new ArrayList<>();
    for(int i = 1; i < expectedCount + 1; i ++) {
      getRequests.add(new GetRequest("message_" + i, SENSOR_TYPE));
    }
    Iterator<Document> results = hbaseDao.getAllLatest(getRequests).iterator();

    for (int i = 0; i < expectedCount; i++) {
      Document expectedDocument = alerts.get(i + 1);
      Document actualDocument = results.next();
      Assert.assertEquals(expectedDocument, actualDocument);
    }

    Assert.assertFalse("Result size should be 12 but was greater", results.hasNext());
  }

  @Override
  public void test() {
    // The main test ensures a variety of things not implemented by HBase run alongside
    // HBaseDao itself.
    // Therefore, just don't do anything for this test.
  }

  protected List<Document> buildAlerts(int count) throws IOException {
    List<Document> alerts = new ArrayList<>();
    for (int i = 0; i < count; ++i) {
      String guid = "message_" + i;
      String json = "{\"guid\":\"message_" + i + "\", \"source:type\":\"test\"}";
      Document alert = new Document(json, guid, SENSOR_TYPE, System.currentTimeMillis());
      alerts.add(alert);
    }
    return alerts;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRemoveComments() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("guid", "add_comment");
    fields.put("source.type", SENSOR_NAME);

    Document document = new Document(fields, "add_comment", SENSOR_NAME, 1526401584951L);
    hbaseDao.update(document, Optional.of(SENSOR_NAME));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    addAlertComment("add_comment", "New Comment", "test_user", 1526401584951L);
    // Ensure we have the first comment
    ArrayList<AlertComment> comments = new ArrayList<>();
    comments.add(new AlertComment("New Comment", "test_user", 1526401584951L));
    document.getDocument().put(COMMENTS_FIELD, comments.stream().map(AlertComment::asMap).collect(
        Collectors.toList()));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    addAlertComment("add_comment", "New Comment 2", "test_user_2", 1526401584952L);
    // Ensure we have the second comment
    comments.add(new AlertComment("New Comment 2", "test_user_2", 1526401584952L));
    document.getDocument().put(COMMENTS_FIELD, comments.stream().map(AlertComment::asMap).collect(
        Collectors.toList()));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    removeAlertComment("add_comment", "New Comment 2", "test_user_2", 1526401584952L);
    // Ensure we only have the first comments
    comments = new ArrayList<>();
    comments.add(new AlertComment(commentOne));
    document.getDocument().put(COMMENTS_FIELD, comments.stream().map(AlertComment::asMap).collect(
        Collectors.toList()));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    removeAlertComment("add_comment", "New Comment", "test_user", 1526401584951L);
    // Ensure we have no comments
    document.getDocument().remove(COMMENTS_FIELD);
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);
  }

  @Override
  protected IndexDao getDao() {
    return hbaseDao;
  }

  @Override
  protected String getIndexName() {
    return null;
  }

  @Override
  protected MockHTable getMockHTable() {
    return null;
  }

  @Override
  protected void addTestData(String indexName, String sensorType, List<Map<String, Object>> docs) {
  }

  @Override
  protected List<Map<String, Object>> getIndexedTestData(String indexName, String sensorType) {
    return null;
  }
}
