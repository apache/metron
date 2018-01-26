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
package org.apache.metron.solr.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.solr.dao.SolrDao;
import org.apache.metron.solr.integration.components.SolrComponent;

public class SolrUpdateIntegrationTest extends UpdateIntegrationTest {

  SolrComponent solrComponent;

  @Override
  protected String getIndexName() {
    return SENSOR_NAME;
  }

  @Override
  protected Map<String, Object> createGlobalConfig() throws Exception {
    return new HashMap<String, Object>() {{
      put("solr.zookeeper", solrComponent.getZookeeperUrl());
    }};
  }

  @Override
  protected IndexDao createDao() throws Exception {
    return new SolrDao();
  }

  @Override
  protected InMemoryComponent startIndex() throws Exception {
    solrComponent = new SolrComponent.Builder().addCollection(SENSOR_NAME, "../metron-solr/src/test/resources/config/test/conf").build();
    solrComponent.start();
    return solrComponent;
  }

  @Override
  protected void loadTestData() throws Exception {

  }

  @Override
  protected void addTestData(String indexName, String sensorType,
      List<Map<String, Object>> docs) throws Exception {
    solrComponent.addDocs(indexName, docs);
  }

  @Override
  protected List<Map<String, Object>> getIndexedTestData(String indexName, String sensorType)
      throws Exception {
    return solrComponent.getAllIndexedDocs(indexName);
  }
}
