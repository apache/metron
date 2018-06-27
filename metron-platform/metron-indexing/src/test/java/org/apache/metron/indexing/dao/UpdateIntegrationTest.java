/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.metron.indexing.dao;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.junit.Assert;
import org.junit.Test;

public abstract class UpdateIntegrationTest {

  /**
   * {
   *   "comment":"New Comment",
   *   "username":"test_user",
   *   "timestamp":1526401584951
   *   }
   */
  @Multiline
  protected String commentOne;

  /**
   * {
   *   "comment":"New Comment 2",
   *   "username":"test_user_2",
   *   "timestamp":1526401584952
   *   }
   */
  @Multiline
  protected String commentTwo;

  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;
  protected static final String SENSOR_NAME = "test";
  private static final String CF = "p";

  private MultiIndexDao dao;

  @Test
  public void test() throws Exception {
    List<Map<String, Object>> inputData = new ArrayList<>();
    for(int i = 0; i < 10;++i) {
      final String name = "message" + i;
      inputData.add(
          new HashMap<String, Object>() {{
            put("source.type", SENSOR_NAME);
            put("name" , name);
            put("timestamp", System.currentTimeMillis());
            put(Constants.GUID, name);
          }}
      );
    }
    addTestData(getIndexName(), SENSOR_NAME, inputData);
    List<Map<String,Object>> docs = null;
    for(int t = 0;t < MAX_RETRIES;++t, Thread.sleep(SLEEP_MS)) {
      docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
      if(docs.size() >= 10) {
        break;
      }
    }
    Assert.assertEquals(10, docs.size());
    //modify the first message and add a new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron");
      }};
      String guid = "" + message0.get(Constants.GUID);
      getDao().replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());

      Assert.assertEquals(1, getMockHTable().size());
      findUpdatedDoc(message0, guid, SENSOR_NAME);
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
        Assert.assertEquals(message0
            , JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
              .stream()
              .filter(d -> message0.get("new-field").equals(d.get("new-field")))
              .count();
        }
        Assert.assertNotEquals("Data store is not updated!", cnt, 0);
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron2");
      }};
      String guid = "" + message0.get(Constants.GUID);
      getDao().replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
        setIndex(getIndexName());
      }}, Optional.empty());
      Assert.assertEquals(1, getMockHTable().size());
      Document doc = getDao().getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      findUpdatedDoc(message0, guid, SENSOR_NAME);
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = getMockHTable().get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(2, columns.size());
        Assert.assertEquals(message0, JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
            , JSONUtils.MAP_SUPPLIER)
        );
        Assert.assertNotEquals(message0, JSONUtils.INSTANCE.load(new String(columns.firstEntry().getValue())
            , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t,Thread.sleep(SLEEP_MS)) {
          docs = getIndexedTestData(getIndexName(), SENSOR_NAME);
          cnt = docs
              .stream()
              .filter(d -> message0.get("new-field").equals(d.get("new-field")))
              .count();
        }

        Assert.assertNotEquals("Data store is not updated!", cnt, 0);
      }
    }
  }

  @Test
  public void testAddCommentAndPatch() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("guid", "add_comment");
    fields.put("source.type", SENSOR_NAME);

    Document document = new Document(fields, "add_comment", SENSOR_NAME, 1526306463050L);
    getDao().update(document, Optional.of(SENSOR_NAME));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    addAlertComment("add_comment", "New Comment", "test_user", 1526306463050L);
    // Ensure we have the first comment
    ArrayList<AlertComment> comments = new ArrayList<>();
    comments.add(new AlertComment("New Comment", "test_user", 1526306463050L));
    document.getDocument().put(COMMENTS_FIELD, comments.stream().map(AlertComment::asMap).collect(
        Collectors.toList()));
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);

    List<Map<String, Object>> patchList = new ArrayList<>();
    Map<String, Object> patch = new HashMap<>();
    patch.put("op", "add");
    patch.put("path", "/project");
    patch.put("value", "metron");
    patchList.add(patch);

    PatchRequest pr = new PatchRequest();
    pr.setGuid("add_comment");
    pr.setIndex(SENSOR_NAME);
    pr.setSensorType(SENSOR_NAME);
    pr.setPatch(patchList);
    getDao().patch(getDao(), pr, Optional.of(new Date().getTime()));

    document.getDocument().put("project", "metron");
    findUpdatedDoc(document.getDocument(), "add_comment", SENSOR_NAME);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRemoveComments() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("guid", "add_comment");
    fields.put("source.type", SENSOR_NAME);

    Document document = new Document(fields, "add_comment", SENSOR_NAME, 1526401584951L);
    getDao().update(document, Optional.of(SENSOR_NAME));
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

  protected void addAlertComment(String guid, String comment, String username, long timestamp)
      throws IOException {
    CommentAddRemoveRequest request = buildAlertRequest(guid, comment, username, timestamp);
    getDao().addCommentToAlert(request);
  }

  protected void removeAlertComment(String guid, String comment, String username, long timestamp)
      throws IOException {
    CommentAddRemoveRequest request = buildAlertRequest(guid, comment, username, timestamp);
    getDao().removeCommentFromAlert(request);
  }

  private CommentAddRemoveRequest buildAlertRequest(String guid, String comment, String username,
      long timestamp) {
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid(guid);
    request.setComment(comment);
    request.setUsername(username);
    request.setTimestamp(timestamp);
    request.setSensorType(SENSOR_NAME);
    return request;
  }

  protected void findUpdatedDoc(Map<String, Object> message0, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document doc = getDao().getLatest(guid, sensorType);
      if (doc != null && message0.equals(doc.getDocument())) {
        return;
      }
      if (t == MAX_RETRIES -1) {
        MapUtils.debugPrint(System.out, "Expected", message0);
        MapUtils.debugPrint(System.out, "actual", doc.getDocument());
      }
    }
    throw new OriginalNotFoundException("Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }

  protected IndexDao getDao() {
    return dao;
  }

  protected void setDao(MultiIndexDao dao) {
    this.dao = dao;
  }

  protected abstract String getIndexName();
  protected abstract MockHTable getMockHTable();
  protected abstract void addTestData(String indexName, String sensorType, List<Map<String,Object>> docs) throws Exception;
  protected abstract List<Map<String,Object>> getIndexedTestData(String indexName, String sensorType) throws Exception;
}
