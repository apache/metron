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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HBaseDaoIntegrationTest {

  private static final String TABLE_NAME = "metron_update";
  private static final String COLUMN_FAMILY = "cf";
  private static final String SENSOR_TYPE = "test";

  private static IndexDao hbaseDao;

  @BeforeClass
  public static void startHBase() throws Exception {
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

  @Test
  public void shouldGetLatest() throws Exception {
    // Load alerts
    List<Document> alerts = buildAlerts(3);
    alerts.stream().collect(Collectors.toMap(Document::getGuid, document -> Optional.empty()));
    Map<Document, Optional<String>> updates = alerts.stream()
        .collect(Collectors.toMap(document -> document, document -> Optional.empty()));
    hbaseDao.batchUpdate(updates);

    Document actualDocument = hbaseDao.getLatest("message_1", SENSOR_TYPE);
    Document expectedDocument = alerts.get(1);
    Assert.assertEquals(expectedDocument, actualDocument);
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
    List<String> guids = new ArrayList<>();
    for(int i = 1; i < expectedCount + 1; i ++) {
      guids.add("message_" + i);
    }
    Iterator<Document> results = hbaseDao.getAllLatest(guids,
        Collections.singleton(SENSOR_TYPE)).iterator();

    for (int i = 0; i < expectedCount; i++) {
      Document expectedDocument = alerts.get(i + 1);
      Document actualDocument = results.next();
      Assert.assertEquals(expectedDocument, actualDocument);
    }

    Assert.assertFalse("Result size should be 12 but was greater", results.hasNext());
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

}
