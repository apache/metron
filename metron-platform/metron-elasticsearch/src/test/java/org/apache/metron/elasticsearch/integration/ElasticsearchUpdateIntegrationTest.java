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
package org.apache.metron.elasticsearch.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.integration.InMemoryComponent;

public class ElasticsearchUpdateIntegrationTest extends UpdateIntegrationTest {
  private static final String SENSOR_NAME= "test";
  private static String indexDir = "target/elasticsearch_mutation";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String index = SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private static ElasticSearchComponent es;

  @Override
  protected String getIndexName() {
    return SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  }

  @Override
  protected Map<String, Object> createGlobalConfig() throws Exception {
    return new HashMap<String, Object>() {{
      put("es.clustername", "metron");
      put("es.port", "9300");
      put("es.ip", "localhost");
      put("es.date.format", dateFormat);
    }};
  }

  @Override
  protected IndexDao createDao() throws Exception {
    return new ElasticsearchDao();
  }

  @Override
  protected InMemoryComponent startIndex() throws Exception {
    es = new ElasticSearchComponent.Builder()
        .withHttpPort(9211)
        .withIndexDir(new File(indexDir))
        .build();
    es.start();
    return es;
  }

  @Override
  protected void loadTestData() throws Exception {

  }

  @Override
  protected void addTestData(String indexName, String sensorType,
      List<Map<String, Object>> docs) throws Exception {
    es.add(index, SENSOR_NAME
        , Iterables.transform(docs,
            m -> {
              try {
                return JSONUtils.INSTANCE.toJSON(m, true);
              } catch (JsonProcessingException e) {
                throw new IllegalStateException(e.getMessage(), e);
              }
            }
        )
    );
  }

  @Override
  protected List<Map<String, Object>> getIndexedTestData(String indexName, String sensorType) throws Exception {
    return es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
  }
}
