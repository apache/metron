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


import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.dao.ElasticsearchMetaAlertDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.util.HashMap;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {
  private static String indexDir = "target/elasticsearch_search";
  private static String dateFormat = "yyyy.MM.dd.HH";

  /**
   * {
   * "bro_doc": {
   *   "properties": {
   *     "source:type": { "type": "string" },
   *     "ip_src_addr": { "type": "ip" },
   *     "ip_src_port": { "type": "integer" },
   *     "long_field": { "type": "long" },
   *     "timestamp" : { "type": "date" },
   *     "latitude" : { "type": "float" },
   *     "score": { "type": "double" },
   *     "is_alert": { "type": "boolean" },
   *     "location_point": { "type": "geo_point" },
   *     "bro_field": { "type": "string" },
   *     "duplicate_name_field": { "type": "string" }
   *   }
   * }
   * }
   */
  @Multiline
  private static String broTypeMappings;

  /**
   * {
   * "snort_doc": {
   *   "properties": {
   *     "source:type": { "type": "string" },
   *     "ip_src_addr": { "type": "ip" },
   *     "ip_src_port": { "type": "integer" },
   *     "long_field": { "type": "long" },
   *     "timestamp" : { "type": "date" },
   *     "latitude" : { "type": "float" },
   *     "score": { "type": "double" },
   *     "is_alert": { "type": "boolean" },
   *     "location_point": { "type": "geo_point" },
   *     "snort_field": { "type": "integer" },
   *     "duplicate_name_field": { "type": "integer" }
   *   }
   * }
   * }
   */
  @Multiline
  private static String snortTypeMappings;


  @Override
  protected IndexDao createDao() throws Exception {
    IndexDao elasticsearchDao = new ElasticsearchDao();
    elasticsearchDao.init(
            new AccessConfig() {{
              setMaxSearchResults(100);
              setMaxSearchGroups(100);
              setGlobalConfigSupplier( () ->
                new HashMap<String, Object>() {{
                  put("es.clustername", "metron");
                  put("es.port", "9300");
                  put("es.ip", "localhost");
                  put("es.date.format", dateFormat);
                  }}
              );
            }}
    );
    MetaAlertDao ret = new ElasticsearchMetaAlertDao();
    ret.init(elasticsearchDao);
    return elasticsearchDao;
  }

  @Override
  protected InMemoryComponent startIndex() throws Exception {
    InMemoryComponent es = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .build();
    es.start();
    return es;
  }

  @Override
  protected void loadTestData() throws ParseException {
    ElasticSearchComponent es = (ElasticSearchComponent)indexComponent;
    es.getClient().admin().indices().prepareCreate("bro_index_2017.01.01.01")
            .addMapping("bro_doc", broTypeMappings).get();
    es.getClient().admin().indices().prepareCreate("snort_index_2017.01.01.02")
            .addMapping("snort_doc", snortTypeMappings).get();

    BulkRequestBuilder bulkRequest = es.getClient().prepareBulk().setRefresh(true);
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    for(Object o: broArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient().prepareIndex("bro_index_2017.01.01.01", "bro_doc");
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    for(Object o: snortArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient().prepareIndex("snort_index_2017.01.01.02", "snort_doc");
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray metaAlertArray = (JSONArray) new JSONParser().parse(metaAlertData);
    for(Object o: metaAlertArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient().prepareIndex("metaalerts", "metaalert_doc");
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
//      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }
  }
}
