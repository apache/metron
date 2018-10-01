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
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;

/**
 * The tests in this class are common among all UpdateDao implementations.
 */
public abstract class UpdateDaoTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void addCommentShouldThrowExceptionOnMissingAlert() throws Exception {
    exception.expect(IOException.class);
    exception.expectMessage("Unable to add comment. Document with guid guid cannot be found.");

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    getUpdateDao().addCommentToAlert(request, null);
  }

  @Test
  public void removeCommentShouldThrowExceptionOnMissingAlert() throws Exception {
    exception.expect(IOException.class);
    exception.expectMessage("Unable to remove comment. Document with guid guid cannot be found.");

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    getUpdateDao().removeCommentFromAlert(request, null);
  }

  @Test
  public void removeCommentShouldThrowExceptionOnEmptyComments() throws Exception {
    exception.expect(IOException.class);
    exception.expectMessage("Unable to remove comment. Document with guid guid has no comments.");

    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    Document latest = new Document(new HashMap<>(), "guid", "bro", System.currentTimeMillis());

    getUpdateDao().removeCommentFromAlert(request, latest);
  }

  public abstract UpdateDao getUpdateDao();
}
