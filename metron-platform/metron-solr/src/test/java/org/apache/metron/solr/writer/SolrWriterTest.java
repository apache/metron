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
import com.google.common.collect.Iterables;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.configuration.writer.IndexingWriterConfiguration;
import org.apache.metron.enrichment.integration.utils.SampleUtil;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class SolrWriterTest {

  static class CollectionRequestMatcher extends ArgumentMatcher<QueryRequest> {

    private String name;

    public CollectionRequestMatcher(String name) {
      this.name = name;
    }

    @Override
    public boolean matches(Object o) {
      QueryRequest queryRequest = (QueryRequest) o;
      return name.equals(queryRequest.getParams().get("action"));
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(name);
    }
  }

  static class SolrInputDocumentMatcher extends ArgumentMatcher<Collection<SolrInputDocument>> {


    List<Map<String, Object>> expectedDocs;

    public SolrInputDocumentMatcher(List<Map<String, Object>> expectedDocs) {
      this.expectedDocs = expectedDocs;
    }

    @Override
    public boolean matches(Object o) {
      List<SolrInputDocument> docs = (List<SolrInputDocument>)o;
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
    public void describeTo(Description description) {
      description.appendText(expectedDocs.toString());
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
    List<JSONObject> messages = new ArrayList<>();
    messages.add(message1);
    messages.add(message2);

    String collection = "metron";
    MetronSolrClient solr = Mockito.mock(MetronSolrClient.class);
    SolrWriter writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null, null,new IndexingWriterConfiguration("solr", configurations));
    verify(solr, times(1)).setDefaultCollection(collection);

    collection = "metron2";
    Map<String, Object> globalConfig = configurations.getGlobalConfig();
    globalConfig.put("solr.collection", collection);
    configurations.updateGlobalConfig(globalConfig);
    writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null, null, new IndexingWriterConfiguration("solr", configurations));
    verify(solr, times(1)).setDefaultCollection(collection);

    writer.write("test", new IndexingWriterConfiguration("solr", configurations), new ArrayList<>(), messages);
    verify(solr, times(1)).add(eq("yaf"), argThat(new SolrInputDocumentMatcher(ImmutableList.of(message1, message2))));
    verify(solr, times(1)).commit("yaf");

  }
}
