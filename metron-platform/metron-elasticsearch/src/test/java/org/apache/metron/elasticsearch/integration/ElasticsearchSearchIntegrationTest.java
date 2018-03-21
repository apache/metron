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


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.WriteRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {

  private static String indexDir = "target/elasticsearch_search";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;

  /**
   * {
   * "bro_doc": {
   *   "properties": {
   *     "source:type": {
   *        "type": "text",
   *        "fielddata" : "true"
   *     },
   *     "guid" : {
   *        "type" : "keyword"
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
   *        "type": "text",
   *        "fielddata" : "true"
   *     },
   *     "duplicate_name_field": {
   *        "type": "text",
   *        "fielddata" : "true"
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
   *  "snort_doc": {
   *     "properties": {
   *        "source:type": {
   *          "type": "text",
   *          "fielddata" : "true"
   *        },
   *        "guid" : {
   *          "type" : "keyword"
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
   * "bro_doc_default": {
   *   "dynamic_templates": [{
   *     "strings": {
   *       "match_mapping_type": "string",
   *       "mapping": {
   *         "type": "text"
   *       }
   *     }
   *   }]
   *  }
   * }
   */
  @Multiline
  private static String broDefaultStringMappings;

  @Override
  protected IndexDao createDao() throws Exception {
    AccessConfig config = new AccessConfig();
    config.setMaxSearchResults(100);
    config.setMaxSearchGroups(100);
    config.setGlobalConfigSupplier( () ->
            new HashMap<String, Object>() {{
              put("es.clustername", "metron");
              put("es.port", "9300");
              put("es.ip", "localhost");
              put("es.date.format", dateFormat);
            }}
    );

    IndexDao dao = new ElasticsearchDao();
    dao.init(config);
    return dao;
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
  protected void loadTestData()
      throws ParseException, IOException, ExecutionException, InterruptedException {
    ElasticSearchComponent es = (ElasticSearchComponent)indexComponent;
    es.getClient().admin().indices().prepareCreate("bro_index_2017.01.01.01")
            .addMapping("bro_doc", broTypeMappings).addMapping("bro_doc_default", broDefaultStringMappings).get();
    es.getClient().admin().indices().prepareCreate("snort_index_2017.01.01.02")
            .addMapping("snort_doc", snortTypeMappings).get();

    BulkRequestBuilder bulkRequest = es.getClient().prepareBulk().setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    for(Object o: broArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient().prepareIndex("bro_index_2017.01.01.01", "bro_doc");
      indexRequestBuilder = indexRequestBuilder.setId((String) jsonObject.get("guid"));
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    for(Object o: snortArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient().prepareIndex("snort_index_2017.01.01.02", "snort_doc");
      indexRequestBuilder = indexRequestBuilder.setId((String) jsonObject.get("guid"));
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }
  }


}
