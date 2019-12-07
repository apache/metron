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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.collections.MapUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

  protected static final int MAX_RETRIES = 10;
  protected static final int SLEEP_MS = 500;
  protected static final String SENSOR_NAME = "test";
  private static final String CF = "p";

  private IndexDao dao;

  @Test
  public void testUpdate() throws Exception {
    // create a document to update
    final String guid = UUID.randomUUID().toString();
    final Long timestamp = 1526306463050L;
    Document toUpdate = createDocument(guid, timestamp);

    // update the document and validate
    Document updated = getDao().update(toUpdate, Optional.of(SENSOR_NAME));
    assertEquals(toUpdate, updated);

    // ensure the document was updated in the index
    assertDocumentIndexed(toUpdate);
  }

  @Test
  public void testBatchUpdate() throws Exception {
    Map<Document, Optional<String>> toUpdate = new HashMap<>();

    // create the first document to update
    final String guid1 = UUID.randomUUID().toString();
    final Long timestamp1 = 1526306463050L;
    Document document1 = createDocument(guid1, timestamp1);
    toUpdate.put(document1, Optional.of(SENSOR_NAME));

    // create the second document to update
    final String guid2 = UUID.randomUUID().toString();
    final Long timestamp2 = 1526306463100L;
    Document document2 = createDocument(guid2, timestamp2);
    toUpdate.put(document2, Optional.of(SENSOR_NAME));

    // create the third document to update
    final String guid3 = UUID.randomUUID().toString();
    final Long timestamp3 = 1526306463300L;
    Document document3 = createDocument(guid3, timestamp3);
    toUpdate.put(document3, Optional.of(SENSOR_NAME));

    // update the documents as a batch and validate
    Map<Document, Optional<String>> updated = getDao().batchUpdate(toUpdate);
    assertThat(updated.keySet(), hasItem(document1));
    assertThat(updated.keySet(), hasItem(document2));
    assertThat(updated.keySet(), hasItem(document3));

    // ensure the documents were written to the index
    assertDocumentIndexed(document1);
    assertDocumentIndexed(document2);
    assertDocumentIndexed(document3);
  }

  @Test
  public void testAddComment() throws Exception {
    Document document = createAndIndexDocument("testAddCommentAndPatch");

    // comment on the document
    String commentText = "New Comment";
    String commentUser = "test_user";
    long commentTimestamp = 152630493050L;
    Document withComment = addAlertComment(document.getGuid(), commentText, commentUser, commentTimestamp);
    {
      // validate that the comment was made on the returned document
      List<AlertComment> comments = getComments(withComment);
      assertEquals(1, comments.size());
      assertEquals(commentText, comments.get(0).getComment());
      assertEquals(commentUser, comments.get(0).getUsername());
      assertEquals(commentTimestamp, comments.get(0).getTimestamp());
    }
    {
      // validate that the comment was made on the indexed document
      Document indexed = findUpdatedDoc(withComment.getDocument(), withComment.getGuid(), SENSOR_NAME);
      List<AlertComment> comments = getComments(indexed);
      assertEquals(1, comments.size());
      assertEquals(commentText, comments.get(0).getComment());
      assertEquals(commentUser, comments.get(0).getUsername());
      assertEquals(commentTimestamp, comments.get(0).getTimestamp());
    }
  }

  @Test
  public void testPatchDocumentThatHasComment() throws Exception {
    Document document = createAndIndexDocument("testPatchDocumentWithComment");

    // comment on the document
    String commentText = "New Comment";
    String commentUser = "test_user";
    long commentTimestamp = 152630493050L;
    Document withComment = addAlertComment(document.getGuid(), commentText, commentUser, commentTimestamp);

    // create a patch
    List<Map<String, Object>> patches = new ArrayList<>();
    Map<String, Object> patch = new HashMap<>();
    patch.put("op", "add");
    patch.put("path", "/project");
    patch.put("value", "metron");
    patches.add(patch);

    PatchRequest pr = new PatchRequest();
    pr.setGuid(withComment.getGuid());
    pr.setIndex(SENSOR_NAME);
    pr.setSensorType(SENSOR_NAME);
    pr.setPatch(patches);

    // patch the document that has been commented on
    Document patched = getDao().patch(getDao(), pr, Optional.of(withComment.getTimestamp()));
    assertEquals("metron", patched.getDocument().get("project"));

    // ensure the patch was made on the indexed document
    Document indexed = findUpdatedDoc(patched.getDocument(), patched.getGuid(), SENSOR_NAME);
    assertEquals("metron", indexed.getDocument().get("project"));
  }

  @Test
  public void testRemoveComments() throws Exception {
    String guid = "testRemoveComments";
    createAndIndexDocument(guid);

    // add a comment on the document
    Document withComments = addAlertComment(guid, "comment", "user1", 1526401584951L);
    assertEquals(1, getComments(withComments).size());

    // ensure the comment was added to the document in the index
    Document indexedWithComments = findUpdatedDoc(withComments.getDocument(), withComments.getGuid(), withComments.getSensorType());
    assertEquals(1, getComments(indexedWithComments).size());

    // remove a comment from the document
    AlertComment toRemove = getComments(withComments).get(0);
    Document noComments = removeAlertComment(guid, toRemove.getComment(), toRemove.getUsername(), toRemove.getTimestamp());
    assertEquals(0, getComments(noComments).size());

    // ensure the comment was removed from the index
    Document indexedNoComments = findUpdatedDoc(noComments.getDocument(), withComments.getGuid(), withComments.getSensorType());
    assertEquals(0, getComments(indexedNoComments).size());
  }

  protected Document addAlertComment(String guid, String comment, String username, long timestamp)
      throws IOException {
    CommentAddRemoveRequest request = buildAlertRequest(guid, comment, username, timestamp);
    return getDao().addCommentToAlert(request);
  }

  protected Document removeAlertComment(String guid, String comment, String username, long timestamp)
      throws IOException {
    CommentAddRemoveRequest request = buildAlertRequest(guid, comment, username, timestamp);
    return getDao().removeCommentFromAlert(request);
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

  /**
   * Ensures that a document was correctly indexed.
   * @param expected The document that should have been indexed.
   * @return The document that was retrieved from the index.
   */
  private Document assertDocumentIndexed(Document expected) throws Exception {
    // search the index for the document
    Document actual = findUpdatedDoc(expected.getDocument(), expected.getGuid(), expected.getSensorType());

    // most fields should match exactly, except the documentID
    assertEquals(expected.getGuid(), actual.getGuid());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getSensorType(), actual.getSensorType());
    assertEquals(expected.getDocument(), actual.getDocument());

    if(expected.getDocumentID().isPresent()) {
      // the documentID was already defined in 'expected', this ID should have been used when the document was indexed
      assertEquals(expected.getDocumentID().get(), actual.getDocumentID());

    } else {
      // if the documentID was not defined, the indexer should have created one
      assertNotNull(expected.getDocumentID());
    }

    return actual;
  }

  private Document createAndIndexDocument(String guid) throws Exception {
    // create the document
    Long timestamp = 1526306463050L;
    Document toCreate = createDocument(guid, timestamp);

    // index the document
    Document created = getDao().update(toCreate, Optional.of(SENSOR_NAME));
    assertEquals(toCreate, created);

    // ensure the document is indexed
    return assertDocumentIndexed(created);
  }

  protected Document createDocument(String guid, Long timestamp) {
    Map<String, Object> message1 = new HashMap<>();
    message1.put(Constants.GUID, guid);
    message1.put(Constants.SENSOR_TYPE, SENSOR_NAME);
    message1.put(Constants.Fields.TIMESTAMP.getName(), timestamp);

    return new Document(message1, guid, SENSOR_NAME, timestamp);
  }

  private static List<AlertComment> getComments(Document withComment) throws ParseException {
    return getComments(withComment.getDocument());
  }

  private static List<AlertComment> getComments(Map<String, Object> fields) throws ParseException {
    List<AlertComment> comments = new ArrayList<>();
    boolean hasComments = fields.containsKey(COMMENTS_FIELD);
    if(hasComments) {
      List<Object> commentsField = List.class.cast(fields.get(COMMENTS_FIELD));
      for (Object commentObject: commentsField) {
        if (commentObject instanceof Map) {
          // comments are stored as maps in Elasticsearch
          Map<String, Object> commentAsMap = (Map<String, Object>) commentObject;
          comments.add(new AlertComment(commentAsMap));

        } else if (commentObject instanceof String) {
          // comments are stored as json strings in Solr
          String commentAsString = (String) commentObject;
          comments.add(new AlertComment(commentAsString));

        } else {
          throw new IllegalArgumentException(String.format("Unexpected comment value; %s", commentObject));
        }
      }
    }

    return comments;
  }

  /**
   * Normalizes the format of stored comments.
   *
   * <p>Comments are serialized differently when stored in Elasticsearch and Solr.  Comments
   * are stored as maps in Elasticsearch and JSON strings in Solr.  This reformats all comments
   * as maps, so they look the same when validation occurs in the integration tests.
   * @param fields The fields of a document that may contain comments.
   */
  protected static void normalizeCommentsAsMap(Map<String, Object> fields) {
    @SuppressWarnings("unchecked")
    List<Object> commentValues = (List<Object>) fields.get(COMMENTS_FIELD);
    if (commentValues != null) {
      try {
        List<AlertComment> comments = getComments(fields);
        if(comments.size() > 0) {
          // overwrite the comments field
          List<Map<String, Object>> serializedComments = comments
                  .stream()
                  .map(AlertComment::asMap)
                  .collect(Collectors.toList());
          fields.put(COMMENTS_FIELD, serializedComments);

        } else {
          // there are no longer any comments
          fields.remove(COMMENTS_FIELD);
        }

      } catch (ParseException e) {
        throw new IllegalStateException("Unable to parse comment", e);
      }
    }
  }

  protected Document findUpdatedDoc(Map<String, Object> expected, String guid, String sensorType)
      throws InterruptedException, IOException, OriginalNotFoundException {
    // comments are stored differently in Solr and Elasticsearch
    normalizeCommentsAsMap(expected);

    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      Document found = getDao().getLatest(guid, sensorType);
      if (found != null && expected.equals(found.getDocument())) {
        return found;
      }
      if (t == MAX_RETRIES -1) {
        MapUtils.debugPrint(System.out, "Expected", expected);
        MapUtils.debugPrint(System.out, "Actual", found.getDocument());
      }
    }
    throw new OriginalNotFoundException("Count not find " + guid + " after " + MAX_RETRIES + " tries");
  }

  protected IndexDao getDao() {
    return dao;
  }

  protected void setDao(IndexDao dao) {
    this.dao = dao;
  }

  protected abstract String getIndexName();
  protected abstract void addTestData(String indexName, String sensorType, List<Map<String,Object>> docs) throws Exception;
  protected abstract List<Map<String,Object>> getIndexedTestData(String indexName, String sensorType) throws Exception;
}
