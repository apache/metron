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

import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiIndexDaoTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private MultiIndexDao multiIndexDao;
  private IndexDao dao1;
  private IndexDao dao2;

  @Before
  public void setup() {
    dao1 = mock(IndexDao.class);
    dao2 = mock(IndexDao.class);
    multiIndexDao = new MultiIndexDao(dao1, dao2);
  }

  @Test
  public void getLatestShouldReturnLatestAlert() throws Exception {
    Document document1 = new Document(new HashMap<>(), "guid", "bro", 1L);
    Document document2 = new Document(new HashMap<>(), "guid", "bro", 2L);

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.getLatest("guid", "bro")).thenReturn(document1);
    when(dao2.getLatest("guid", "bro")).thenReturn(document2);


    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    Assert.assertEquals(expected, multiIndexDao.getLatest("guid", "bro"));
  }

  @Test
  public void addCommentShouldAddCommentToAlert() throws Exception {
    Document latest = mock(Document.class);
    Document document1 = new Document(new HashMap<>(), "guid", "bro", 1L);
    Document document2 = new Document(new HashMap<>(), "guid", "bro", 2L);

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.addCommentToAlert(request, latest)).thenReturn(document1);
    when(dao2.addCommentToAlert(request, latest)).thenReturn(document2);


    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    Assert.assertEquals(expected, multiIndexDao.addCommentToAlert(request, latest));
  }

  @Test
  public void removeCommentShouldRemoveCommentFromAlert() throws Exception {
    Document latest = mock(Document.class);
    Document document1 = new Document(new HashMap<>(), "guid", "bro", 1L);
    Document document2 = new Document(new HashMap<>(), "guid", "bro", 2L);

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    when(dao1.removeCommentFromAlert(request, latest)).thenReturn(document1);
    when(dao2.removeCommentFromAlert(request, latest)).thenReturn(document2);


    Document expected = new Document(new HashMap<>(), "guid", "bro", 2L);
    Assert.assertEquals(expected, multiIndexDao.removeCommentFromAlert(request, latest));
  }
}
