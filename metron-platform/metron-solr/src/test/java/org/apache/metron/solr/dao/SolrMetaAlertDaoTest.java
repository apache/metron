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

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.RetrieveLatestDao;
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
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.solr.client.SolrClientFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SolrMetaAlertDao.class, SolrClientFactory.class})
public class SolrMetaAlertDaoTest {
  private static AccessConfig accessConfig = new AccessConfig();
  private SolrClient client;

  @BeforeClass
  public static void setupBefore() {
    accessConfig.setGlobalConfigSupplier(() ->
        new HashMap<String, Object>() {{
          put(SOLR_ZOOKEEPER, "zookeeper:2181");
        }}
    );
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    client = mock(SolrClient.class);
    mockStatic(SolrClientFactory.class);
    when(SolrClientFactory.create(accessConfig.getGlobalConfigSupplier().get())).thenReturn(client);
  }

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
    metaAlertDao.init(dao);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitInvalidDao() {
    HBaseDao dao = new HBaseDao();
    SolrMetaAlertDao solrDao = new SolrMetaAlertDao();
    solrDao.init(dao, Optional.empty());
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGuids() throws InvalidCreateException, IOException {
    SolrDao solrDao = new SolrDao();
    solrDao.init(accessConfig);
    SolrMetaAlertDao emaDao = new SolrMetaAlertDao();
    emaDao.init(solrDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    emaDao.createMetaAlert(createRequest);
  }

  @Test(expected = InvalidCreateException.class)
  public void testCreateMetaAlertEmptyGroups() throws InvalidCreateException, IOException {
    SolrDao solrDao = new SolrDao();
    solrDao.init(accessConfig);
    MultiIndexDao miDao = new MultiIndexDao(solrDao);
    SolrMetaAlertDao emaDao = new SolrMetaAlertDao();
    emaDao.init(miDao);

    MetaAlertCreateRequest createRequest = new MetaAlertCreateRequest();
    createRequest.setAlerts(Collections.singletonList(new GetRequest("don't", "care")));
    emaDao.createMetaAlert(createRequest);
  }
}
