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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.WriteRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticsearchSearchIntegrationTest extends SearchIntegrationTest {

  private static String indexDir = "target/elasticsearch_search";
  private static String dateFormat = "yyyy.MM.dd.HH";
  private static final int MAX_RETRIES = 10;
  private static final int SLEEP_MS = 500;
  protected static IndexDao dao;

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
   *     "ttl": {
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
   *        "ttl": {
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

  @BeforeClass
  public static void setup() throws Exception {
    indexComponent = startIndex();
    dao = createDao();
    // The data is all static for searches, so we can set it up beforehand, and it's faster
    loadTestData();
  }

  protected static IndexDao createDao() {
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

  protected static InMemoryComponent startIndex() throws Exception {
    InMemoryComponent es = new ElasticSearchComponent.Builder()
            .withHttpPort(9211)
            .withIndexDir(new File(indexDir))
            .build();
    es.start();
    return es;
  }

  protected static void loadTestData() throws ParseException {
    ElasticSearchComponent es = (ElasticSearchComponent) indexComponent;
    es.getClient().admin().indices().prepareCreate("bro_index_2017.01.01.01")
        .addMapping("bro_doc", broTypeMappings)
        .addMapping("bro_doc_default", broDefaultStringMappings).get();
    es.getClient().admin().indices().prepareCreate("snort_index_2017.01.01.02")
        .addMapping("snort_doc", snortTypeMappings).get();

    BulkRequestBuilder bulkRequest = es.getClient().prepareBulk()
        .setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
    JSONArray broArray = (JSONArray) new JSONParser().parse(broData);
    for (Object o : broArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient()
          .prepareIndex("bro_index_2017.01.01.01", "bro_doc");
      indexRequestBuilder = indexRequestBuilder.setId((String) jsonObject.get("guid"));
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder
          .setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    JSONArray snortArray = (JSONArray) new JSONParser().parse(snortData);
    for (Object o : snortArray) {
      JSONObject jsonObject = (JSONObject) o;
      IndexRequestBuilder indexRequestBuilder = es.getClient()
          .prepareIndex("snort_index_2017.01.01.02", "snort_doc");
      indexRequestBuilder = indexRequestBuilder.setId((String) jsonObject.get("guid"));
      indexRequestBuilder = indexRequestBuilder.setSource(jsonObject.toJSONString());
      indexRequestBuilder = indexRequestBuilder
          .setTimestamp(jsonObject.get("timestamp").toString());
      bulkRequest.add(indexRequestBuilder);
    }
    BulkResponse bulkResponse = bulkRequest.execute().actionGet();
    if (bulkResponse.hasFailures()) {
      throw new RuntimeException("Failed to index test data");
    }
  }

  @Test
  public void bad_facet_query_throws_exception() throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Failed to execute search");
    SearchRequest request = JSONUtils.INSTANCE.load(badFacetQuery, SearchRequest.class);
    dao.search(request);
  }



  @Override
  public void returns_column_metadata_for_specified_indices() throws Exception {
    // getColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(13, fieldTypes.size());
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("alert"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(14, fieldTypes.size());
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ttl"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("alert"));
    }
  }

  @Override
  public void returns_column_data_for_multiple_indices() throws Exception {
    Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
    Assert.assertEquals(15, fieldTypes.size());
    Assert.assertEquals(FieldType.KEYWORD, fieldTypes.get("guid"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("source:type"));
    Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
    Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
    Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
    Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
    Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
    Assert.assertEquals(FieldType.TEXT, fieldTypes.get("bro_field"));
    Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
    //NOTE: This is because the field is in both bro and snort and they have different types.
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("ttl"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("threat:triage:score"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("alert"));
  }

  @Test
  public void throws_exception_on_aggregation_queries_on_non_string_non_numeric_fields()
      throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Failed to execute search");
    GroupRequest request = JSONUtils.INSTANCE.load(badGroupQuery, GroupRequest.class);
    dao.group(request);
  }

  @Test
  public void different_type_filter_query() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(differentTypeFilterQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(1, response.getTotal());
    List<SearchResult> results = response.getResults();
    Assert.assertEquals("bro", results.get(0).getSource().get("source:type"));
    Assert.assertEquals("data 1", results.get(0).getSource().get("ttl"));
  }

  @Override
  protected String getSourceTypeField() {
    return Constants.SENSOR_TYPE.replace('.', ':');
  }

  @Override
  protected IndexDao getIndexDao() {
    return dao;
  }
}
