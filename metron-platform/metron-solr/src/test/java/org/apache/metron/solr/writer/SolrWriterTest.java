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

import backtype.storm.tuple.Tuple;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.writer.EnrichmentWriterConfiguration;
import org.apache.metron.integration.utils.SampleUtil;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.Description;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class SolrWriterTest {

  class CollectionRequestMatcher extends ArgumentMatcher<QueryRequest> {

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

  class SolrInputDocumentMatcher extends ArgumentMatcher<SolrInputDocument> {

    private int expectedId;
    private String expectedSourceType;
    private int expectedInt;
    private double expectedDouble;

    public SolrInputDocumentMatcher(int expectedId, String expectedSourceType, int expectedInt, double expectedDouble) {
      this.expectedId = expectedId;
      this.expectedSourceType = expectedSourceType;
      this.expectedInt = expectedInt;
      this.expectedDouble = expectedDouble;
    }

    @Override
    public boolean matches(Object o) {
      SolrInputDocument solrInputDocument = (SolrInputDocument) o;
      int actualId = (Integer) solrInputDocument.get("id").getValue();
      String actualName = (String) solrInputDocument.get("sensorType").getValue();
      int actualInt = (Integer) solrInputDocument.get("intField_i").getValue();
      double actualDouble = (Double) solrInputDocument.get("doubleField_d").getValue();
      return expectedId == actualId && expectedSourceType.equals(actualName) && expectedInt == actualInt && expectedDouble == actualDouble;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format("fields: [id=%d, doubleField_d=%f, name=%s, intField_i=%d]", expectedId, expectedDouble, expectedSourceType, expectedInt));
    }

  }

  @Test
  public void testWriter() throws Exception {
    EnrichmentConfigurations configurations = SampleUtil.getSampleEnrichmentConfigs();
    JSONObject message1 = new JSONObject();
    message1.put("intField", 100);
    message1.put("doubleField", 100.0);
    JSONObject message2 = new JSONObject();
    message2.put("intField", 200);
    message2.put("doubleField", 200.0);
    List<JSONObject> messages = new ArrayList<>();
    messages.add(message1);
    messages.add(message2);

    String collection = "metron";
    MetronSolrClient solr = Mockito.mock(MetronSolrClient.class);
    SolrWriter writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null, new EnrichmentWriterConfiguration(configurations));
    verify(solr, times(1)).createCollection(collection, 1, 1);
    verify(solr, times(1)).setDefaultCollection(collection);

    collection = "metron2";
    int numShards = 4;
    int replicationFactor = 2;
    Map<String, Object> globalConfig = configurations.getGlobalConfig();
    globalConfig.put("solr.collection", collection);
    globalConfig.put("solr.numShards", numShards);
    globalConfig.put("solr.replicationFactor", replicationFactor);
    configurations.updateGlobalConfig(globalConfig);
    writer = new SolrWriter().withMetronSolrClient(solr);
    writer.init(null, new EnrichmentWriterConfiguration(configurations));
    verify(solr, times(1)).createCollection(collection, numShards, replicationFactor);
    verify(solr, times(1)).setDefaultCollection(collection);

    writer.write("test", new EnrichmentWriterConfiguration(configurations), new ArrayList<>(), messages);
    verify(solr, times(1)).add(argThat(new SolrInputDocumentMatcher(message1.toJSONString().hashCode(), "test", 100, 100.0)));
    verify(solr, times(1)).add(argThat(new SolrInputDocumentMatcher(message2.toJSONString().hashCode(), "test", 200, 200.0)));
    verify(solr, times(0)).commit(collection);

    writer = new SolrWriter().withMetronSolrClient(solr).withShouldCommit(true);
    writer.init(null, new EnrichmentWriterConfiguration(configurations));
    writer.write("test", new EnrichmentWriterConfiguration(configurations), new ArrayList<>(), messages);
    verify(solr, times(2)).add(argThat(new SolrInputDocumentMatcher(message1.toJSONString().hashCode(), "test", 100, 100.0)));
    verify(solr, times(2)).add(argThat(new SolrInputDocumentMatcher(message2.toJSONString().hashCode(), "test", 200, 200.0)));
    verify(solr, times(1)).commit(collection);

  }
}
