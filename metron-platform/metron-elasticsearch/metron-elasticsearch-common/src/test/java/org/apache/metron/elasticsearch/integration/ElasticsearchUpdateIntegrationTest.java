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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.client.ElasticsearchClientFactory;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.integration.UnableToStartException;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Response;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchUpdateIntegrationTest extends UpdateIntegrationTest {
  private static final String SENSOR_NAME= "test";
  private static String indexDir = "target/elasticsearch_mutation";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String index = SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private static ElasticSearchComponent es;

  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";
  private static MockHTable table;
  private static IndexDao elasticsearchDao;
  private static AccessConfig accessConfig;
  private static Map<String, Object> globalConfig;

  /**
   * {
   *   "template": "test*",
   *   "mappings": {
   *     "test_doc": {
   *       "properties": {
   *         "guid": {
   *           "type": "keyword"
   *         }
   *       }
   *     }
   *   }
   * }
   */
  @Multiline
  private static String indexTemplate;

  @Override
  protected String getIndexName() {
    return SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  }

  @BeforeClass
  public static void setupBeforeClass() throws UnableToStartException, IOException {
    Configuration config = HBaseConfiguration.create();
    MockHBaseTableProvider tableProvider = new MockHBaseTableProvider();
    MockHBaseTableProvider.addToCache(TABLE_NAME, CF);
    table = (MockHTable) tableProvider.getTable(config, TABLE_NAME);

    globalConfig = new HashMap<>();
    globalConfig.put("es.clustername", "metron");
    globalConfig.put("es.port", "9200");
    globalConfig.put("es.ip", "localhost");
    globalConfig.put("es.date.format", dateFormat);
    globalConfig.put(HBaseDao.HBASE_TABLE, TABLE_NAME);
    globalConfig.put(HBaseDao.HBASE_CF, CF);

    accessConfig = new AccessConfig();
    accessConfig.setTableProvider(tableProvider);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    es = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .withAccessConfig(accessConfig)
            .build();
    es.start();

    installIndexTemplate();
  }

  @Before
  public void setup() {
    elasticsearchDao = new ElasticsearchDao()
            .withRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    elasticsearchDao.init(accessConfig);
    setDao(elasticsearchDao);
  }

  @After
  public void reset() {
    es.reset();
    table.clear();
  }

  @AfterClass
  public static void teardown() {
    es.stop();
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

  /**
   * Install the index template to ensure that "guid" is of type "keyword". The
   * {@link org.apache.metron.elasticsearch.dao.ElasticsearchRetrieveLatestDao} cannot find
   * documents if the type is not "keyword".
   *
   * See https://www.elastic.co/guide/en/elasticsearch/reference/5.6/query-dsl-term-query.html
   */
  private static void installIndexTemplate() throws IOException {
    HttpEntity broEntity = new NStringEntity(indexTemplate, ContentType.APPLICATION_JSON);
    ElasticsearchClient client = ElasticsearchClientFactory.create(globalConfig);
    Response response = client
            .getLowLevelClient()
            .performRequest("PUT", "/_template/test_template", Collections.emptyMap(), broEntity);
    Assert.assertThat(response.getStatusLine().getStatusCode(), CoreMatchers.equalTo(200));
  }
}
