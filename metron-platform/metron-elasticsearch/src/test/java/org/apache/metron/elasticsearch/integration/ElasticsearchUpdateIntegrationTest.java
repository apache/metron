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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.HBaseDao;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MultiIndexDao;
import org.apache.metron.indexing.dao.UpdateIntegrationTest;
import org.apache.metron.integration.UnableToStartException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class ElasticsearchUpdateIntegrationTest extends UpdateIntegrationTest {
  private static final String SENSOR_NAME= "test";
  private static String indexDir = "target/elasticsearch_mutation";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String index = SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private static ElasticSearchComponent es;

  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";
  private static MockHTable table;
  private static IndexDao hbaseDao;

  @Override
  protected String getIndexName() {
    return SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  }

  @BeforeClass
  public static void setupBeforeClass() throws UnableToStartException {
    es = new ElasticSearchComponent.Builder()
        .withHttpPort(9211)
        .withIndexDir(new File(indexDir))
        .build();
    es.start();
  }

  @Before
  public void setup() throws IOException {
    Configuration config = HBaseConfiguration.create();
    MockHBaseTableProvider tableProvider = new MockHBaseTableProvider();
    MockHBaseTableProvider.addToCache(TABLE_NAME, CF);
    table = (MockHTable) tableProvider.getTable(config, TABLE_NAME);

    hbaseDao = new HBaseDao();
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setTableProvider(tableProvider);
    Map<String, Object> globalConfig = createGlobalConfig();
    globalConfig.put(HBaseDao.HBASE_TABLE, TABLE_NAME);
    globalConfig.put(HBaseDao.HBASE_CF, CF);
    accessConfig.setGlobalConfigSupplier(() -> globalConfig);

    dao = new MultiIndexDao(hbaseDao, createDao());
    dao.init(accessConfig);
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

  protected static Map<String, Object> createGlobalConfig() {
    return new HashMap<String, Object>() {{
      put("es.clustername", "metron");
      put("es.port", "9300");
      put("es.ip", "localhost");
      put("es.date.format", dateFormat);
    }};
  }

  protected static IndexDao createDao() {
    return new ElasticsearchDao();
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

  @Override
  protected MockHTable getMockHTable() {
    return table;
  }
}
