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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The tests in this class are common among all UpdateDao implementations.
 */
public abstract class UpdateDaoTest {
  @Test
  public void addCommentShouldThrowExceptionOnMissingAlert() {
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    IOException e = assertThrows(IOException.class, () -> getUpdateDao().addCommentToAlert(request, null));
    assertEquals("Unable to add comment. Document with guid guid cannot be found.", e.getMessage());
  }

  @Test
  public void removeCommentShouldThrowExceptionOnMissingAlert() {
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");

    IOException e = assertThrows(IOException.class, () -> getUpdateDao().removeCommentFromAlert(request, null));
    assertEquals("Unable to remove comment. Document with guid guid cannot be found.", e.getMessage());
  }

  @Test
  public void removeCommentShouldThrowExceptionOnEmptyComments() throws Exception {
    CommentAddRemoveRequest request = new CommentAddRemoveRequest();
    request.setGuid("guid");
    Document latest = new Document(new HashMap<>(), "guid", "bro", System.currentTimeMillis());

    IOException e = assertThrows(IOException.class, () -> getUpdateDao().removeCommentFromAlert(request, latest));
    assertEquals("Unable to remove comment. Document with guid guid has no comments.", e.getMessage());
  }

  public abstract UpdateDao getUpdateDao();
}
