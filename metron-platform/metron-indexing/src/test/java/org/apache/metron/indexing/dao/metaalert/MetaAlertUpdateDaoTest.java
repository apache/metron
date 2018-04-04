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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetaAlertUpdateDaoTest {

  @Mock
  IndexDao indexDao;

  private class TestMetaAlertUpdateDao implements MetaAlertUpdateDao {

    Map<String, Document> documents = new HashMap<>();

    TestMetaAlertUpdateDao() {
      Document active = new Document(new HashMap<>(), "active", "metaalert", 0L);
      documents.put("active", active);

      Document inactive = new Document(new HashMap<>(), "active", "metaalert", 0L);
      documents.put("inactive", inactive);
    }

    @Override
    public Document getLatest(String guid, String sensorType) {
      return documents.get(guid);
    }

    @Override
    public Iterable<Document> getAllLatest(List<GetRequest> getRequests) {
      return null;
    }

    @Override
    public IndexDao getIndexDao() {
      return indexDao;
    }

    @Override
    public String getMetAlertSensorName() {
      return "metaalert";
    }

    @Override
    public String getMetaAlertIndex() {
      return "metaalert_index";
    }

    @Override
    public void update(Document update, Optional<String> index) {

    }
  }

  /**
   {
   "guid": "meta_alert",
   "index": "metaalert_index",
   "patch": [
   {
   "op": "add",
   "path": "/alert",
   "value": []
   }
   ],
   "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String alertPatchRequest;

  /**
   {
   "guid": "meta_alert",
   "index": "metaalert_index",
   "patch": [
   {
   "op": "add",
   "path": "/status",
   "value": []
   }
   ],
   "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String statusPatchRequest;

  /**
   {
   "guid": "meta_alert",
   "index": "metaalert_index",
   "patch": [
   {
   "op": "add",
   "path": "/name",
   "value": []
   }
   ],
   "sensorType": "metaalert"
   }
   */
  @Multiline
  public static String namePatchRequest;

  @Test(expected = UnsupportedOperationException.class)
  public void testBatchUpdateThrowsException() {
    new TestMetaAlertUpdateDao().batchUpdate(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchNotAllowedAlert() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(alertPatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertFalse(new TestMetaAlertUpdateDao().isPatchAllowed(pr));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchNotAllowedStatus() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(statusPatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertFalse(new TestMetaAlertUpdateDao().isPatchAllowed(pr));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testPatchAllowedName() throws ParseException {
    PatchRequest pr = new PatchRequest();
    Map<String, Object> patch = (JSONObject) new JSONParser().parse(namePatchRequest);
    pr.setPatch(Collections.singletonList((JSONObject) ((JSONArray) patch.get("patch")).get(0)));
    assertTrue(new TestMetaAlertUpdateDao().isPatchAllowed(pr));
  }

  @Test
  public void testUpdateSingle() throws IOException {
    TestMetaAlertUpdateDao updateDao = new TestMetaAlertUpdateDao();
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document document = new Document(new HashMap<>(), "guid", "sensor", 0L);
    updates.put(document, Optional.empty());
    updateDao.update(updates);
    verify(indexDao, times(1)).update(document, Optional.empty());
  }

  @Test
  public void testUpdateMultiple() throws IOException {
    TestMetaAlertUpdateDao updateDao = new TestMetaAlertUpdateDao();
    Map<Document, Optional<String>> updates = new HashMap<>();
    Document documentOne = new Document(new HashMap<>(), "guid", "sensor", 0L);
    updates.put(documentOne, Optional.empty());
    Document documentTwo = new Document(new HashMap<>(), "guid2", "sensor", 0L);
    updates.put(documentTwo, Optional.empty());
    updateDao.update(updates);
    verify(indexDao, times(1)).batchUpdate(updates);
  }

  @Test(expected = IllegalStateException.class)
  public void testAddAlertsToMetaAlertInactive() throws IOException {
    new TestMetaAlertUpdateDao().addAlertsToMetaAlert("inactive", null);
  }
}
