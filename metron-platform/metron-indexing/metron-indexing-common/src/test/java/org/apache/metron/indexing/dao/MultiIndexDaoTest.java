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

import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MultiIndexDaoTest {
  private MultiIndexDao multiIndexDao;
  private IndexDao dao1;
  private IndexDao dao2;

  private Document document1;
  private Document document2;

  @BeforeEach
  public void setup() {
    dao1 = mock(IndexDao.class);
    dao2 = mock(IndexDao.class);
    multiIndexDao = new MultiIndexDao(dao1, dao2);

    document1 = new Document(new HashMap<>(), "guid", "bro", 1L);
    document2 = new Document(new HashMap<>(), "guid", "bro", 2L);
  }

  @Test
  public void shouldUpdateAll() throws IOException {
    Document actual = multiIndexDao.update(document1, Optional.of("bro"));
    assertEquals(document1, actual);

    // both 'backing' daos should have received the update
    verify(dao1).update(eq(document1), eq(Optional.of("bro")));
    verify(dao2).update(eq(document1), eq(Optional.of("bro")));
  }

  @Test
  public void shouldThrowExceptionWithPartialFailureOnUpdate() throws IOException {
    // dao2 will throw an exception causing the 'partial failure'
    when(dao2.update(any(), any())).thenThrow(new IllegalStateException());

    assertThrows(IOException.class, () -> multiIndexDao.update(document1, Optional.of("bro")));
  }

  @Test
  public void shouldBatchUpdateAll() throws IOException {
    Map<Document, Optional<String>> updates = new HashMap<Document, Optional<String>>() {{
      put(document1, Optional.of("bro"));
      put(document2, Optional.of("bro"));
    }};

    Map<Document, Optional<String>> actual = multiIndexDao.batchUpdate(updates);
    assertEquals(updates, actual);

    // both 'backing' daos should have received the updates
    verify(dao1).batchUpdate(eq(updates));
    verify(dao2).batchUpdate(eq(updates));
  }

  @Test
  public void shouldThrowExceptionWithPartialFailureOnBatchUpdate() throws IOException {
    // dao2 will throw an exception causing the 'partial failure'
    when(dao2.batchUpdate(any())).thenThrow(new IllegalStateException());

    Map<Document, Optional<String>> updates = new HashMap<Document, Optional<String>>() {{
      put(document1, Optional.of("bro"));
      put(document2, Optional.of("bro"));
    }};

    assertThrows(IOException.class, () -> multiIndexDao.batchUpdate(updates));
  }

  @Test
  public void getLatestShouldReturnLatestAlert() throws Exception {
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.getLatest("guid", "bro")).thenReturn(document1);
    when(dao2.getLatest("guid", "bro")).thenReturn(document2);

    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    assertEquals(expected, multiIndexDao.getLatest("guid", "bro"));
  }

  @Test
  public void addCommentShouldAddCommentToAlert() throws Exception {
    Document latest = mock(Document.class);

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.addCommentToAlert(request, latest)).thenReturn(document1);
    when(dao2.addCommentToAlert(request, latest)).thenReturn(document2);

    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    assertEquals(expected, multiIndexDao.addCommentToAlert(request, latest));
  }

  @Test
  public void shouldThrowExceptionWithPartialFailureOnAddComment() throws Exception {
    Document latest = mock(Document.class);
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    // dao2 will throw an exception
    when(dao1.addCommentToAlert(request, latest)).thenReturn(document1);
    when(dao2.addCommentToAlert(request, latest)).thenThrow(new IllegalStateException());

    assertThrows(IOException.class, () -> multiIndexDao.addCommentToAlert(request, latest));
  }

  @Test
  public void removeCommentShouldRemoveCommentFromAlert() throws Exception {
    Document latest = mock(Document.class);

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.removeCommentFromAlert(request, latest)).thenReturn(document1);
    when(dao2.removeCommentFromAlert(request, latest)).thenReturn(document2);

    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    assertEquals(expected, multiIndexDao.removeCommentFromAlert(request, latest));
  }

  @Test
  public void shouldThrowExceptionWithPartialFailureOnRemoveComment() throws Exception {
    Document latest = mock(Document.class);
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    // dao2 will throw an exception
    when(dao1.removeCommentFromAlert(request, latest)).thenReturn(document1);
    when(dao2.removeCommentFromAlert(request, latest)).thenThrow(new IllegalStateException());

    assertThrows(IOException.class, () -> multiIndexDao.removeCommentFromAlert(request, latest));
  }

  @Test
  public void shouldGetColumnMetadata() throws Exception {
    List<String> indices = Collections.singletonList("bro");

    Map<String, FieldType> expected = new HashMap<String, FieldType>() {{
      put("bro", FieldType.TEXT);
    }};

    when(dao1.getColumnMetadata(eq(indices))).thenReturn(null);
    when(dao2.getColumnMetadata(eq(indices))).thenReturn(expected);

    Map<String, FieldType> actual = multiIndexDao.getColumnMetadata(indices);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldGetColumnMetadataWithNulls() throws Exception {
    List<String> indices = Collections.singletonList("bro");

    // both 'backing' DAOs respond with null
    when(dao1.getColumnMetadata(eq(indices))).thenReturn(null);
    when(dao2.getColumnMetadata(eq(indices))).thenReturn(null);

    Map<String, FieldType> actual = multiIndexDao.getColumnMetadata(indices);
    assertNull(actual);
  }

  @Test
  public void shouldSearch() throws Exception {
    SearchRequest request = new SearchRequest();
    SearchResponse expected = new SearchResponse();

    when(dao1.search(eq(request))).thenReturn(null);
    when(dao2.search(eq(request))).thenReturn(expected);

    SearchResponse actual = multiIndexDao.search(request);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldSearchWithNulls() throws Exception {
    SearchRequest request = new SearchRequest();

    when(dao1.search(eq(request))).thenReturn(null);
    when(dao2.search(eq(request))).thenReturn(null);

    SearchResponse actual = multiIndexDao.search(request);
    assertNull(actual);
  }

  @Test
  public void shouldGroup() throws Exception {
    GroupRequest request = new GroupRequest();
    GroupResponse expected = new GroupResponse();

    when(dao1.group(eq(request))).thenReturn(null);
    when(dao2.group(eq(request))).thenReturn(expected);

    GroupResponse actual = multiIndexDao.group(request);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldGroupWithNulls() throws Exception {
    GroupRequest request = new GroupRequest();

    when(dao1.group(eq(request))).thenReturn(null);
    when(dao2.group(eq(request))).thenReturn(null);

    GroupResponse actual = multiIndexDao.group(request);
    assertNull(actual);
  }
}
