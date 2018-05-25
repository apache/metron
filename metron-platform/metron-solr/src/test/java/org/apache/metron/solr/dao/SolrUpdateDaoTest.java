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

import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.util.IndexingCacheUtil;
import org.apache.metron.solr.matcher.SolrInputDocumentListMatcher;
import org.apache.metron.solr.matcher.SolrInputDocumentMatcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CollectionAdminRequest.class})
public class SolrUpdateDaoTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private SolrClient client;
  private SolrUpdateDao solrUpdateDao;

  private static AccessConfig accessConfig = new AccessConfig();

  @BeforeClass
  public static void setupBefore() {
    accessConfig.setGlobalConfigSupplier(() ->
        new HashMap<String, Object>() {{
          put("solr.zookeeper", "zookeeper:2181");
        }}
    );
    IndexingConfigurations indexingConfigs = mock(IndexingConfigurations.class);
    ConfigurationsCache cache = mock(ConfigurationsCache.class);

    Map<String, Object> broIndexingConfig = new HashMap<String, Object>() {{
      put("solr", new HashMap<String, Object>() {{
      }});
    }};
    when(indexingConfigs.getSensorIndexingConfig("bro")).thenReturn(broIndexingConfig);
    when(cache.get(IndexingConfigurations.class)).thenReturn(indexingConfigs);

    accessConfig.setIndexSupplier(IndexingCacheUtil.getIndexLookupFunction(cache, "solr"));
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    client = mock(SolrClient.class);
    solrUpdateDao = new SolrUpdateDao(client, accessConfig);
  }

  @Test
  public void updateShouldProperlyUpdateDocumentImplicitIndex() throws Exception {
    Document document = new Document(new HashMap<String, Object>(){{
      put("field", "value");
    }}, "guid", "bro", 0L);

    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("field", "value");

    solrUpdateDao.update(document, Optional.empty());

    verify(client).add(eq("bro"), argThat(new SolrInputDocumentMatcher(solrInputDocument)));

  }

  @Test
  public void updateShouldProperlyUpdateDocumentExplicitIndex() throws Exception {
    Document document = new Document(new HashMap<String, Object>(){{
      put("field", "value");
    }}, "guid", "bro", 0L);

    SolrInputDocument solrInputDocument = new SolrInputDocument();
    solrInputDocument.addField("field", "value");

    solrUpdateDao.update(document, Optional.of("bro"));

    verify(client).add(eq("bro"), argThat(new SolrInputDocumentMatcher(solrInputDocument)));
  }

  @Test
  public void batchUpdateShouldProperlyUpdateDocuments() throws Exception {
    Document broDocument1 = new Document(new HashMap<String, Object>(){{
      put("broField1", "value");
      put("guid", "broGuid1");
    }}, "broGuid1", "bro", 0L);
    Document broDocument2 = new Document(new HashMap<String, Object>(){{
      put("broField2", "value");
      put("guid", "broGuid2");
    }}, "broGuid2", "bro", 0L);

    Map<Document, Optional<String>> updates = new HashMap<Document, Optional<String>>(){{
      put(broDocument1, Optional.of("bro"));
      put(broDocument2, Optional.of("bro"));
    }};

    SolrInputDocument broSolrInputDocument1 = new SolrInputDocument();
    broSolrInputDocument1.addField("broField1", "value");
    broSolrInputDocument1.addField("guid", "broGuid1");
    SolrInputDocument broSolrInputDocument2 = new SolrInputDocument();
    broSolrInputDocument2.addField("broField2", "value");
    broSolrInputDocument2.addField("guid", "broGuid2");

    solrUpdateDao.batchUpdate(updates);

    verify(client).add(eq("bro"), argThat(new SolrInputDocumentListMatcher(Arrays.asList(broSolrInputDocument1, broSolrInputDocument2))));
  }

  @Test
  public void batchUpdateShouldProperlyUpdateDocumentsWithoutIndex() throws Exception {
    Document snortDocument1 = new Document(new HashMap<String, Object>(){{
      put("snortField1", "value");
      put("guid", "snortGuid1");
    }}, "snortGuid1", "snort", 0L);
    Document snortDocument2 = new Document(new HashMap<String, Object>(){{
      put("snortField2", "value");
      put("guid", "snortGuid2");
    }}, "snortGuid2", "snort", 0L);

    Map<Document, Optional<String>> updates = new HashMap<Document, Optional<String>>(){{
      put(snortDocument1, Optional.empty());
      put(snortDocument2, Optional.empty());
    }};

    SolrInputDocument snortSolrInputDocument1 = new SolrInputDocument();
    snortSolrInputDocument1.addField("snortField1", "value");
    snortSolrInputDocument1.addField("guid", "snortGuid1");
    SolrInputDocument snortSolrInputDocument2 = new SolrInputDocument();
    snortSolrInputDocument2.addField("snortField2", "value");
    snortSolrInputDocument2.addField("guid", "snortGuid2");

    solrUpdateDao.batchUpdate(updates);

    verify(client).add(eq("snort"), argThat(new SolrInputDocumentListMatcher(Arrays.asList(snortSolrInputDocument1, snortSolrInputDocument2))));
  }

}
