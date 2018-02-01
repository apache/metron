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

import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {
  private static String host = "localhost";
  private static String port = "9310";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;

  /**
   * {
   * "searchintegrationtest_bro_doc": {
   *   "properties": {
   *     "source:type": {
   *        "type": "string",
   *        "index": "not_analyzed"
   *     },
   *     "ip_src_addr": {
   *        "type": "ip"
   *     },
   *     "ip_src_port": {
   *        "type": "integer"
   *     },
   *     "long_field": {
   *        "type": "long"
   *     },
   *     "timestamp": {
   *        "type": "date",
   *        "format": "epoch_millis"
   *      },
   *     "latitude" : {
   *        "type": "float"
   *      },
   *     "score": {
   *        "type": "double"
   *     },
   *     "is_alert": {
   *        "type": "boolean"
   *     },
   *     "location_point": {
   *        "type": "geo_point"
   *     },
   *     "bro_field": {
   *        "type": "string"
   *     },
   *     "duplicate_name_field": {
   *        "type": "string"
   *     },
   *     "alert": {
   *         "type": "nested"
   *     }
   *   }
   *  }
   * }
   */
  @Multiline
  private static String broTypeMappings;

  /**
   * {
   *  "searchintegrationtest_snort_doc": {
   *     "properties": {
   *        "source:type": {
   *          "type": "string",
   *          "index": "not_analyzed"
   *        },
   *        "ip_src_addr": {
   *          "type": "ip"
   *        },
   *        "ip_src_port": {
   *          "type": "integer"
   *        },
   *        "long_field": {
   *          "type": "long"
   *        },
   *        "timestamp": {
   *          "type": "date",
   *          "format": "epoch_millis"
   *        },
   *        "latitude" : {
   *          "type": "float"
   *        },
   *        "score": {
   *          "type": "double"
   *        },
   *        "is_alert": {
   *          "type": "boolean"
   *        },
   *        "location_point": {
   *          "type": "geo_point"
   *        },
   *        "snort_field": {
   *          "type": "integer"
   *        },
   *        "duplicate_name_field": {
   *          "type": "integer"
   *        },
   *        "alert": {
   *           "type": "nested"
   *        },
   *        "threat:triage:score": {
   *           "type": "float"
   *        }
   *      }
   *    }
   * }
   */
  @Multiline
  private static String snortTypeMappings;

  /**
   * {
   * "searchintegrationtest_metaalert_doc": {
   *   "properties": {
   *     "source:type": { "type": "string" },
   *     "alert": { "type": "nested"}
   *   }
   * }
   * }
   */
  @Multiline
  private static String metaAlertTypeMappings;

  private static Map<String, Object> globalConfig;
  private static Client client;

  @BeforeClass
  public static void start() {
    globalConfig = new HashMap<String, Object>() {{
      put("es.clustername", "elasticsearch");
      put("es.port", port);
      put("es.ip", host);
      put("es.date.format", dateFormat);
    }};
    client = ElasticsearchUtils.getClient(globalConfig, null);
    clearIndices();
  }

  @AfterClass
  public static void stop() throws Exception {
    clearIndices();
  }

  private static void clearIndices() {
    try {
      client.admin().indices().prepareDelete(broIndex, snortIndex, metaAlertIndex).get();
    } catch (IndexNotFoundException infe) {}
  }

  @Override
  protected IndexDao createDao() throws Exception {
    AccessConfig config = new AccessConfig();
    config.setMaxSearchResults(100);
    config.setMaxSearchGroups(100);
    config.setGlobalConfigSupplier( () -> globalConfig);

    IndexDao dao = new ElasticsearchDao();
    dao.init(config);
    return dao;
  }

  @Override
  protected void loadTestData()
      throws ParseException, IOException, ExecutionException, InterruptedException {
    client.admin().indices().prepareCreate(broIndex)
            .addMapping(broType, broTypeMappings).get();
    client.admin().indices().prepareCreate(snortIndex)
            .addMapping(snortType, snortTypeMappings).get();
    client.admin().indices().prepareCreate(metaAlertIndex)
        .addMapping(metaAlertType, metaAlertTypeMappings).get();

    BulkRequestBuilder bulkRequest = client.prepareBulk().setRefresh(true);
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    for(Object o: broArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(broIndex, broType);
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    for(Object o: snortArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(snortIndex, snortType);
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray metaAlertArray = (JSONArray) new JSONParser().parse(metaAlertData);
    for(Object o: metaAlertArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(metaAlertIndex, metaAlertType);
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }

    SearchResponse broDocs = client
        .prepareSearch(broIndex)
        .setTypes(broType)
        .setQuery(QueryBuilders.matchAllQuery())
        .get();
    // We're changing the _id field, we need to create a copy and delete the original.
    for (SearchHit hit : broDocs.getHits()) {
      // Bro GUIDs to collide while using the standard analyzer
      // Use timestamp as part of guid because query may not return in order each time
      IndexRequest indexRequest = new IndexRequest()
          .index(broIndex)
          .type(broType)
          .id("bro-" + hit.getSource().get("timestamp"))
          .source(hit.getSource());
      client.index(indexRequest).get();

      // Delete the original
      client.prepareDelete(broIndex, broType, hit.getId())
          .get();
    }

    // Wait until everything is updated
    // Assume true until proven otherwise.
    boolean allUpdated = true;
    for (int t = 0; t < MAX_RETRIES; ++t, Thread.sleep(SLEEP_MS)) {
      allUpdated = true;
      SearchResponse response = client
          .prepareSearch(broIndex)
          .setTypes(broType)
          .setQuery(QueryBuilders.matchAllQuery())
          .get();
      if (response.getHits().getTotalHits() == 0) {
        throw new IllegalStateException("Bro index is empty. No docs to validate were updated");
      }
      for (SearchHit hit : response.getHits()) {
        if (!hit.getId().startsWith("bro-")) {
          allUpdated = false;
        }
      }
      if (allUpdated) {
        break;
      }
    }
    if (!allUpdated) {
      throw new IllegalStateException("Unable to update Elasticsearch ids properly");
    }
  }
}
