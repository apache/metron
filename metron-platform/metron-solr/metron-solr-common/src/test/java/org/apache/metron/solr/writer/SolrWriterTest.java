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
package org.apache.metron.solr.writer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.enrichment.integration.utils.SampleUtil;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


public class SolrWriterTest {

  static class CollectionRequestMatcher implements ArgumentMatcher<QueryRequest> {

    private String name;

    public CollectionRequestMatcher(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(QueryRequest queryRequest) {
      return name.equals(queryRequest.getParams().get("action"));
    }

    @Override
    public String toString() {
        return name;
    }
  }

  static class SolrInputDocumentMatcher implements ArgumentMatcher<List<SolrInputDocument>> {

    List<Map<String, Object>> expectedDocs;

    public SolrInputDocumentMatcher(List<Map<String, Object>> expectedDocs) {
      this.expectedDocs = expectedDocs;
    }

    @Override
    public boolean matches(List<SolrInputDocument> docs) {
      int size = docs.size();
      if(size != expectedDocs.size()) {
        return false;
      }
      for(int i = 0; i < size;++i) {
        SolrInputDocument doc = docs.get(i);
        Map<String, Object> expectedDoc = expectedDocs.get(i);
        for(Map.Entry<String, Object> expectedKv : expectedDoc.entrySet()) {
          if(!expectedKv.getValue().equals(doc.get(expectedKv.getKey()).getValue())) {
            return false;
          }
        }
      }
      return true;
    }

    @Override
    public String toString() {
        return expectedDocs.toString();
    }

  }

  @Test
  public void testWriter() throws Exception {
    IndexingConfigurations configurations = SampleUtil.getSampleIndexingConfigs();
    JSONObject message1 = new JSONObject();
    message1.put(Constants.GUID, "guid-1");
    message1.put(Constants.SENSOR_TYPE, "test");
    message1.put("intField", 100);
    message1.put("doubleField", 100.0);
    JSONObject message2 = new JSONObject();
    message2.put(Constants.GUID, "guid-2");
    message2.put(Constants.SENSOR_TYPE, "test");
    message2.put("intField", 200);
    message2.put("doubleField", 200.0);
    List<BulkMessage<JSONObject>> messages = new ArrayList<>();
    messages.add(new BulkMessage<>("message1", message1));
    messages.add(new BulkMessage<>("message2", message2));

    String collection = "metron";
    MetronSolrClient solr = Mockito.mock(MetronSolrClient.class);
    SolrWriter writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null,new IndexingWriterConfiguration("solr", configurations));
    verify(solr, times(1)).setDefaultCollection(collection);

    collection = "metron2";
    Map<String, Object> globalConfig = configurations.getGlobalConfig();
    globalConfig.put("solr.collection", collection);
    configurations.updateGlobalConfig(globalConfig);
    writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null, new IndexingWriterConfiguration("solr", configurations));
    verify(solr, times(1)).setDefaultCollection(collection);

    writer.write("test", new IndexingWriterConfiguration("solr", configurations), messages);
    verify(solr, times(1)).add(eq("yaf"), argThat(new SolrInputDocumentMatcher(ImmutableList.of(message1, message2))));
    verify(solr, times(1)).commit("yaf"
                                 , (boolean)SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.defaultValue.get()
                                 , (boolean)SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.defaultValue.get()
                                 , (boolean)SolrWriter.SolrProperties.COMMIT_SOFT.defaultValue.get()
                                 );

  }

  @Test
  public void configTest_zookeeperQuorumSpecified() {
    String expected = "test";
    assertEquals(expected,
            SolrWriter.SolrProperties.ZOOKEEPER_QUORUM.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.ZOOKEEPER_QUORUM.name, expected)
                    , String.class));
  }

  @Test
  public void configTest_zookeeperQuorumUnpecified() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            SolrWriter.SolrProperties.ZOOKEEPER_QUORUM.coerceOrDefaultOrExcept(
                new HashMap<>(), String.class));
  }


  @Test
  public void configTest_commitPerBatchSpecified() {
    Object expected = false;
    assertEquals(expected,
            SolrWriter.SolrProperties.COMMIT_PER_BATCH.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_PER_BATCH.name, false)
                    , Boolean.class));
  }

  @Test
  public void configTest_commitPerBatchUnpecified() {
    assertEquals(SolrWriter.SolrProperties.COMMIT_PER_BATCH.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_PER_BATCH.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , Boolean.class));
    assertEquals(SolrWriter.SolrProperties.COMMIT_PER_BATCH.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_PER_BATCH.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_PER_BATCH.name, new DummyClass())
                    , Boolean.class));
  }

  @Test
  public void configTest_commitSoftSpecified() {
    Object expected = true;
    assertEquals(expected,
            SolrWriter.SolrProperties.COMMIT_SOFT.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_SOFT.name, expected)
                    , Boolean.class));
  }

  @Test
  public void configTest_commitSoftUnpecified() {
    assertEquals(SolrWriter.SolrProperties.COMMIT_SOFT.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_SOFT.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , Boolean.class));
    assertEquals(SolrWriter.SolrProperties.COMMIT_SOFT.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_SOFT.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_SOFT.name, new DummyClass())
                    , Boolean.class));
  }

  @Test
  public void configTest_commitWaitFlushSpecified() {
    Object expected = false;
    assertEquals(expected,
            SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.name, expected)
                    , Boolean.class));
  }

  @Test
  public void configTest_commitWaitFlushUnspecified() {
    assertEquals(SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , Boolean.class));
    assertEquals(SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_WAIT_FLUSH.name, new DummyClass())
                    , Boolean.class));
  }

  @Test
  public void configTest_commitWaitSearcherSpecified() {
    Object expected = false;
    assertEquals(expected,
            SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.name, expected)
                    , Boolean.class));
  }

  @Test
  public void configTest_commitWaitSearcherUnspecified() {
    assertEquals(SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , Boolean.class));
    assertEquals(SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.defaultValue.get(),
    SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.COMMIT_WAIT_SEARCHER.name, new DummyClass())
                    , Boolean.class));
  }

  @Test
  public void configTest_defaultCollectionSpecified() {
    Object expected = "mycollection";
    assertEquals(expected,
            SolrWriter.SolrProperties.DEFAULT_COLLECTION.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.DEFAULT_COLLECTION.name, expected)
                    , String.class));
  }

  @Test
  public void configTest_defaultCollectionUnspecified() {
    assertEquals(SolrWriter.SolrProperties.DEFAULT_COLLECTION.defaultValue.get(),
    SolrWriter.SolrProperties.DEFAULT_COLLECTION.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , String.class));
  }

  @Test
  public void configTest_httpConfigSpecified() {
    Object expected = new HashMap<String, Object>() {{
      put("name", "metron");
    }};
    assertEquals(expected,
            SolrWriter.SolrProperties.HTTP_CONFIG.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.HTTP_CONFIG.name, expected)
                    , Map.class));
  }

  @Test
  public void configTest_httpConfigUnspecified() {
    assertEquals(SolrWriter.SolrProperties.HTTP_CONFIG.defaultValue.get(),
    SolrWriter.SolrProperties.HTTP_CONFIG.coerceOrDefaultOrExcept(
                    new HashMap<>()
                    , Map.class));
    assertEquals(SolrWriter.SolrProperties.HTTP_CONFIG.defaultValue.get(),
    SolrWriter.SolrProperties.HTTP_CONFIG.coerceOrDefaultOrExcept(
                    ImmutableMap.of( SolrWriter.SolrProperties.HTTP_CONFIG.name, new DummyClass())
                    , Map.class));
  }

  public static class DummyClass {}
}
