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
import org.apache.metron.elasticsearch.integration.utils.ElasticsearchTestUtils;
import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {
  /**
   * {
   * "searchintegrationtest_bro_doc": {
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
   *  "searchintegrationtest_snort_doc": {
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

  private static Client client;

  @BeforeClass
  public static void start() {
    client = ElasticsearchUtils.getClient(ElasticsearchTestUtils.getGlobalConfig(), null);
    ElasticsearchTestUtils.clearIndices(client, broIndex, snortIndex);
  }

  @AfterClass
  public static void stop() throws Exception {
    ElasticsearchTestUtils.clearIndices(client, broIndex, snortIndex);
  }

  @Override
  protected IndexDao createDao() throws Exception {
    AccessConfig config = new AccessConfig();
    config.setMaxSearchResults(100);
    config.setMaxSearchGroups(100);
    config.setGlobalConfigSupplier(ElasticsearchTestUtils::getGlobalConfig);

    IndexDao dao = new ElasticsearchDao();
    dao.init(config);
    return dao;
  }

  @Override
  protected void loadTestData()
      throws ParseException {
    client.admin().indices().prepareCreate(broIndex)
            .addMapping(broType, broTypeMappings).get();
    client.admin().indices().prepareCreate(snortIndex)
            .addMapping(snortType, snortTypeMappings).get();

    BulkRequestBuilder bulkRequest = client.prepareBulk().setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    addIndexRequest(bulkRequest, broData, broIndex, broType);
    addIndexRequest(bulkRequest, snortData, snortIndex, snortType);
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }
  }

  private void addIndexRequest(BulkRequestBuilder bulkRequest, String data, String index, String docType) throws ParseException {
    JSONArray dataArray = (JSONArray) new JSONParser().parse(data);
    for(Object o: dataArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, docType);
      indexRequestBuilder = indexRequestBuilder.setId((String) jsonObject.get("guid"));
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder.setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
  }


}
