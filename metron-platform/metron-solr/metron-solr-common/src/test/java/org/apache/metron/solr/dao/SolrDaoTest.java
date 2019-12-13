/**
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

import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class SolrDaoTest {
  private SolrClient client;
  private SolrSearchDao solrSearchDao;
  private SolrUpdateDao solrUpdateDao;
  private SolrRetrieveLatestDao solrRetrieveLatestDao;
  private SolrColumnMetadataDao solrColumnMetadataDao;
  private SolrDao solrDao;

  @BeforeEach
  public void setUp() {
    client = mock(SolrClient.class);
    solrSearchDao = mock(SolrSearchDao.class);
    solrUpdateDao = mock(SolrUpdateDao.class);
    solrRetrieveLatestDao = mock(SolrRetrieveLatestDao.class);
    solrColumnMetadataDao = mock(SolrColumnMetadataDao.class);
  }

  @Test
  public void initShouldEnableKerberos() {
    AccessConfig accessConfig = new AccessConfig();

    solrDao = spy(new SolrDao(
        client,
        accessConfig,
        solrSearchDao,
        solrUpdateDao,
        solrRetrieveLatestDao,
        solrColumnMetadataDao));
    doNothing().when(solrDao).enableKerberos();

    solrDao.init(accessConfig);

    verify(solrDao, times(0)).enableKerberos();

    accessConfig.setKerberosEnabled(true);

    solrDao.init(accessConfig);
    verify(solrDao).enableKerberos();
  }

  @Test
  public void testInitShouldCreateDaos() {
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() ->
            new HashMap<String, Object>() {{
              put(SOLR_ZOOKEEPER, "zookeeper:2181");
            }}
    );
    solrDao = new SolrDao();
    solrDao.init(accessConfig);
    assertTrue(solrDao.daosSetup());
  }

  @Test
  public void testDaoDelegatesQueries() throws Exception {
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() ->
        new HashMap<String, Object>() {{
          put(SOLR_ZOOKEEPER, "zookeeper:2181");
        }}
    );

    solrDao = spy(new SolrDao(
            client,
            accessConfig,
            solrSearchDao,
            solrUpdateDao,
            solrRetrieveLatestDao,
            solrColumnMetadataDao));

    solrDao.init(accessConfig);

    SearchRequest searchRequest = mock(SearchRequest.class);
    solrDao.search(searchRequest);
    verify(solrSearchDao).search(searchRequest);

    GroupRequest groupRequest = mock(GroupRequest.class);
    solrDao.group(groupRequest);
    verify(solrSearchDao).group(groupRequest);

    solrDao.getLatest("guid", "collection");
    verify(solrRetrieveLatestDao).getLatest("guid", "collection");

    GetRequest getRequest1 = mock(GetRequest.class);
    GetRequest getRequest2 = mock(GetRequest.class);
    solrDao.getAllLatest(Arrays.asList(getRequest1, getRequest2));
    verify(solrRetrieveLatestDao).getAllLatest(Arrays.asList(getRequest1, getRequest2));

    Document document = mock(Document.class);
    solrDao.update(document, Optional.of("bro"));
    verify(solrUpdateDao).update(document, Optional.of("bro"));

    Map<Document, Optional<String>> updates = new HashMap<Document, Optional<String>>() {{
      put(document, Optional.of("bro"));
    }};
    solrDao.batchUpdate(updates);
    verify(solrUpdateDao).batchUpdate(updates);

    solrDao.getColumnMetadata(Arrays.asList("bro", "snort"));
    verify(solrColumnMetadataDao).getColumnMetadata(Arrays.asList("bro", "snort"));
  }
}
