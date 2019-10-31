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

package org.apache.metron.solr.dao;

import org.apache.metron.indexing.dao.*;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.search.*;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolrMetaAlertDaoTest {
  private static AccessConfig accessConfig = new AccessConfig();

  @BeforeAll
  public static void setupBefore() {
    accessConfig.setGlobalConfigSupplier(() ->
        new HashMap<String, Object>() {{
          put(SOLR_ZOOKEEPER, "zookeeper:2181");
        }}
    );
  }

  @Test
  public void testInvalidInit() {
    IndexDao dao = new IndexDao() {
      @Override
      public SearchResponse search(SearchRequest searchRequest) {
        return null;
      }

      @Override
      public GroupResponse group(GroupRequest groupRequest) {
        return null;
      }

      @Override
      public void init(AccessConfig config) {
      }

      @Override
      public Document getLatest(String guid, String sensorType) {
        return null;
      }

      @Override
      public Iterable<Document> getAllLatest(
          List<GetRequest> getRequests) {
        return null;
      }

      @Override
      public Document update(Document update, Optional<String> index) {
        return null;
      }

      @Override
      public Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) {
        return null;
      }

      @Override
      public Document addCommentToAlert(CommentAddRemoveRequest request) {
        return null;
      }

      @Override
      public Document removeCommentFromAlert(CommentAddRemoveRequest request) {
        return null;
      }

      @Override
      public Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) {
        return null;
      }

      @Override
      public Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) {
        return null;
      }

      @Override
      public Document patch(RetrieveLatestDao dao, PatchRequest request, Optional<Long> timestamp) {
        return null;
      }

      @Override
      public Map<String, FieldType> getColumnMetadata(List<String> indices) {
        return null;
      }
    };
    SolrMetaAlertDao metaAlertDao = new SolrMetaAlertDao();
    assertThrows(IllegalArgumentException.class, () -> metaAlertDao.init(dao));
  }

  @Test
  public void testInitInvalidDao() {
    HBaseDao dao = new HBaseDao();
    SolrMetaAlertDao solrDao = new SolrMetaAlertDao();
    assertThrows(IllegalArgumentException.class, () -> solrDao.init(dao, Optional.empty()));
  }

  @Test
  public void testCreateMetaAlertEmptyGuids() {
    SolrDao solrDao = new SolrDao();
    solrDao.init(accessConfig);
    SolrMetaAlertDao emaDao = new SolrMetaAlertDao();
    emaDao.init(solrDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    assertThrows(InvalidCreateException.class, () -> emaDao.createMetaAlert(createRequest));
  }

  @Test
  public void testCreateMetaAlertEmptyGroups() {
    SolrDao solrDao = new SolrDao();
    solrDao.init(accessConfig);
    MultiIndexDao miDao = new MultiIndexDao(solrDao);
    SolrMetaAlertDao emaDao = new SolrMetaAlertDao();
    emaDao.init(miDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    createRequest.setAlerts(Collections.singletonList(new GetRequest("don't", "care")));
    assertThrows(InvalidCreateException.class, () -> emaDao.createMetaAlert(createRequest));
  }
}
