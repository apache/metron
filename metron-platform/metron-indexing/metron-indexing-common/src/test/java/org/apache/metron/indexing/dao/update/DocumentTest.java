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
package org.apache.metron.indexing.dao.update;

import org.apache.metron.indexing.dao.search.AlertComment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link Document} class.
 */
public class DocumentTest {

  @Test
  public void shouldGetNoComments() {
    Document document = document();
    assertEquals(Collections.emptyList(), document.getComments());
  }

  @Test
  public void shouldAddComment() {
    // some comments from jane
    List<AlertComment> expected = new ArrayList<AlertComment>() {{
      new AlertComment("comment1", "jane", 1234L);
      new AlertComment("comment2", "jane", 2345L);
      new AlertComment("comment3", "jane", 3456L);
      new AlertComment("comment4", "jane", 4567L);
      new AlertComment("comment5", "jane", 5678L);
    }};

    // add the comments to the document
    Document document = document();
    for(AlertComment comment: expected) {
      document.addComment(comment);
    }

    assertEquals(expected, document.getComments());
  }

  @Test
  public void shouldRemoveComment() {
    Document document = document();

    // add some comments
    AlertComment comment1 = new AlertComment("comment1", "jane", 1234L);
    document.addComment(comment1);

    AlertComment comment2 = new AlertComment("comment2", "jane", 5678L);
    document.addComment(comment2);

    // remove a comment
    document.removeComment(comment2);
    assertEquals(1, document.getComments().size());
    assertEquals(comment1, document.getComments().get(0));

    // remove another comment
    document.removeComment(comment1);
    assertEquals(0, document.getComments().size());
  }

  @Test
  public void shouldRemoveLastComment() {
    Document document = document();

    // add some comments
    AlertComment comment1 = new AlertComment("comment1", "jane", 1234L);
    document.addComment(comment1);

    // remove the last comment
    document.removeComment(comment1);
    assertEquals(0, document.getComments().size());

    // if there are no comments, there should be no comment field in the document
    assertFalse(document.getDocument().containsKey(COMMENTS_FIELD));
  }

  @Test
  public void shouldHandleCommentsAsMap() {
    AlertComment comment1 = new AlertComment("comment1", "jane", 1234L);
    AlertComment comment2 = new AlertComment("comment2", "jane", 1234L);

    // forcibly write the comments as a list of maps
    Document document = document();
    document.getDocument().put(COMMENTS_FIELD, Arrays.asList(comment1.asMap(), comment2.asMap()));

    assertTrue(document.getComments().contains(comment1));
    assertTrue(document.getComments().contains(comment2));
  }

  @Test
  public void shouldHandleCommentsAsJson() {
    AlertComment comment1 = new AlertComment("comment1", "jane", 1234L);
    AlertComment comment2 = new AlertComment("comment2", "jane", 1234L);

    // forcibly write the comments as a list of json strings
    Document document = document();
    document.getDocument().put(COMMENTS_FIELD, Arrays.asList(comment1.asJson(), comment2.asJson()));

    assertTrue(document.getComments().contains(comment1));
    assertTrue(document.getComments().contains(comment2));
  }

  private Document document() {
    return new Document(fields(), UUID.randomUUID().toString(), "bro", System.currentTimeMillis());
  }

  private Map<String, Object> fields() {
    return new HashMap<String, Object>() {{
      put("ip_src_addr", "192.168.1.1");
      put("ip_dst_addr", "10.0.0.1");
      put("timestamp", System.currentTimeMillis());
    }};
  }
}
