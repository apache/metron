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

import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;
import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.UpdateDaoTest;
import org.apache.metron.indexing.dao.search.AlertComment;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;
import org.apache.metron.indexing.util.IndexingCacheUtil;
import org.apache.metron.solr.matcher.SolrInputDocumentListMatcher;
import org.apache.metron.solr.matcher.SolrInputDocumentMatcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * This class contains tests specific to the SolrUpdateDao implementation.  It also returns the SolrUpdateDao
 * implementation to be used in UpdateDaoTest.  UpdateDaoTest contains a common set of tests that all Dao
 * implementations must pass.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CollectionAdminRequest.class})
public class SolrUpdateDaoTest extends UpdateDaoTest {

  private SolrClient client;
  private SolrRetrieveLatestDao solrRetrieveLatestDao;
  private SolrUpdateDao solrUpdateDao;

  private static AccessConfig accessConfig = new AccessConfig();

  @BeforeClass
  public static void setupBefore() {
    accessConfig.setGlobalConfigSupplier(() ->
        new HashMap<String, Object>() {{
          put(SOLR_ZOOKEEPER, "zookeeper:2181");
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
    solrRetrieveLatestDao = new SolrRetrieveLatestDao(client, accessConfig);
    solrUpdateDao = new SolrUpdateDao(client, solrRetrieveLatestDao, accessConfig);
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

  @Test
  public void testConvertCommentsToRaw() {
    List<Map<String, Object>> commentList = new ArrayList<>();
    Map<String, Object> comments = new HashMap<>();
    comments.put("comment", "test comment");
    comments.put("username", "test username");
    comments.put("timestamp", 1526424323279L);
    commentList.add(comments);

    Map<String, Object> document = new HashMap<>();
    document.put("testField", "testValue");
    document.put(COMMENTS_FIELD, commentList);
    solrUpdateDao.convertCommentsToRaw(document);

    @SuppressWarnings("unchecked")
    List<String> actualComments = (List<String>) document.get(COMMENTS_FIELD);
    String expectedComment = "{\"comment\":\"test comment\",\"username\":\"test username\",\"timestamp\":1526424323279}";
    assertEquals(expectedComment, actualComments.get(0));
    assertEquals(1, actualComments.size());
    assertEquals("testValue", document.get("testField"));
  }

  @Test
  public void getPatchedDocument() throws IOException, OriginalNotFoundException {
    // Create the document to be patched. Including comments
    Map<String, Object> latestDoc = new HashMap<>();
    latestDoc.put(Constants.GUID, "guid");
    List<Map<String, Object>> comments = new ArrayList<>();
    comments.add(new AlertComment("comment", "user", 0L).asMap());
    comments.add(new AlertComment("comment_2", "user_2", 0L).asMap());
    latestDoc.put(COMMENTS_FIELD, comments);
    Document latest = new Document(latestDoc, "guid", "bro", 0L);

    SolrRetrieveLatestDao retrieveLatestDao = spy(new SolrRetrieveLatestDao(null, accessConfig));
    doReturn(latest).when(retrieveLatestDao).getLatest("guid", "bro");

    // Create the patch
    PatchRequest request = new PatchRequest();
    request.setIndex("bro");
    request.setSensorType("bro");
    request.setGuid("guid");
    List<Map<String, Object>> patchList = new ArrayList<>();
    Map<String, Object> patch = new HashMap<>();
    patch.put("op", "add");
    patch.put("path", "/project");
    patch.put("value", "metron");
    patchList.add(patch);
    request.setPatch(patchList);
    Document actual = solrUpdateDao.getPatchedDocument(retrieveLatestDao, request, Optional.of(0L));

    // Add the patch to our original document
    latest.getDocument().put("project", "metron");
    assertEquals(actual, latest);
  }

  @Override
  public UpdateDao getUpdateDao() {
    return solrUpdateDao;
  }
}
