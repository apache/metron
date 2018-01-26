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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.indexing.dao.*;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class ElasticsearchUpdateIntegrationTest extends UpdateIntegrationTest {
  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;
  private static final String SENSOR_NAME= "test";
  private static final String TABLE_NAME = "modifications";
  private static final String CF = "p";
  private static String indexDir = "target/elasticsearch_mutation";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static String index = SENSOR_NAME + "_index_" + new SimpleDateFormat(dateFormat).format(new Date());
  private static MockHTable table;
  private static IndexDao esDao;
  private static IndexDao hbaseDao;
  private static MultiIndexDao dao;
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

      Assert.assertEquals(1, table.size());
      Document doc = dao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = table.get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(1, columns.size());
        Assert.assertEquals(message0
                , JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                        , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t, Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
          cnt = docs
                  .stream()
                  .filter(d -> message0.get("new-field").equals(d.get("new-field")))
                  .count();
        }
        Assert.assertNotEquals("Elasticsearch is not updated!", cnt, 0);
      }
    }
    //modify the same message and modify the new field
    {
      Map<String, Object> message0 = new HashMap<String, Object>(inputData.get(0)) {{
        put("new-field", "metron2");
      }};
      String guid = "" + message0.get(Constants.GUID);
      dao.replace(new ReplaceRequest(){{
        setReplacement(message0);
        setGuid(guid);
        setSensorType(SENSOR_NAME);
      }}, Optional.empty());
      Assert.assertEquals(1, table.size());
      Document doc = dao.getLatest(guid, SENSOR_NAME);
      Assert.assertEquals(message0, doc.getDocument());
      {
        //ensure hbase is up to date
        Get g = new Get(HBaseDao.Key.toBytes(new HBaseDao.Key(guid, SENSOR_NAME)));
        Result r = table.get(g);
        NavigableMap<byte[], byte[]> columns = r.getFamilyMap(CF.getBytes());
        Assert.assertEquals(2, columns.size());
        Assert.assertEquals(message0, JSONUtils.INSTANCE.load(new String(columns.lastEntry().getValue())
                        , JSONUtils.MAP_SUPPLIER)
        );
        Assert.assertNotEquals(message0, JSONUtils.INSTANCE.load(new String(columns.firstEntry().getValue())
                        , JSONUtils.MAP_SUPPLIER)
        );
      }
      {
        //ensure ES is up-to-date
        long cnt = 0;
        for (int t = 0; t < MAX_RETRIES && cnt == 0; ++t,Thread.sleep(SLEEP_MS)) {
          docs = es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
          cnt = docs
                  .stream()
                  .filter(d -> message0.get("new-field").equals(d.get("new-field")))
                  .count();
        }

        Assert.assertNotEquals("Elasticsearch is not updated!", cnt, 0);
      }
    }
  }


  @Override
  protected List<Map<String, Object>> getIndexedTestData(String indexName, String sensorType) throws Exception {
    return es.getAllIndexedDocs(index, SENSOR_NAME + "_doc");
  }

}
