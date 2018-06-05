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

package org.apache.metron.elasticsearch.dao;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.Test;

public class ElasticsearchMetaAlertDaoTest {


  @Test(expected = IllegalArgumentException.class)
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
      public void update(Document update, Optional<String> index) {
      }

      @Override
      public void batchUpdate(Map<Document, Optional<String>> updates) {
      }

      @Override
      public Map<String, FieldType> getColumnMetadata(List<String> indices) {
        return null;
      }

      @Override
      public void addCommentToAlert(CommentAddRemoveRequest request) {
      }

      @Override
      public void removeCommentFromAlert(CommentAddRemoveRequest request) {
      }

      @Override
      public void addCommentToAlert(CommentAddRemoveRequest request, Document latest) {
      }

      @Override
      public void removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) {
      }
    };
    ElasticsearchMetaAlertDao metaAlertDao = new ElasticsearchMetaAlertDao();
    metaAlertDao.init(dao);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitInvalidDao() {
    HBaseDao dao = new HBaseDao();
    ElasticsearchMetaAlertDao esDao = new ElasticsearchMetaAlertDao();
    esDao.init(dao, Optional.empty());
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGuids() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(esDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    emaDao.createMetaAlert(createRequest);
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGroups() throws InvalidCreateException, IOException {
    ElasticsearchDao esDao = new ElasticsearchDao();
    MultiIndexDao miDao = new MultiIndexDao(esDao);
    ElasticsearchMetaAlertDao emaDao = new ElasticsearchMetaAlertDao();
    emaDao.init(miDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    createRequest.setAlerts(Collections.singletonList(new GetRequest("don't", "care")));
    emaDao.createMetaAlert(createRequest);
  }
}
