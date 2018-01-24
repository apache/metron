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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.dao.ElasticsearchDao;
import org.apache.metron.elasticsearch.integration.components.ElasticSearchComponent;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.SearchIntegrationTest;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.integration.InMemoryComponent;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
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
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("duplicate_name_field"));
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
      Assert.assertEquals(FieldType.TEXT, fieldTypes.get("duplicate_name_field"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("alert"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(14, fieldTypes.size());
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("duplicate_name_field"));
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
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("duplicate_name_field"));
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
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("duplicate_name_field"));
    Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("threat:triage:score"));
    Assert.assertEquals(FieldType.OTHER, fieldTypes.get("alert"));
  }

  @Override
  public void group_by_ip_query() throws Exception {
    GroupRequest request = JSONUtils.INSTANCE.load(groupByIpQuery, GroupRequest.class);
    GroupResponse response = dao.group(request);

    // expect only 1 group for 'ip_src_addr'
    Assert.assertEquals("ip_src_addr", response.getGroupedBy());

    // there are 8 different 'ip_src_addr' values
    List<GroupResult> groups = response.getGroupResults();
    Assert.assertEquals(8, groups.size());

    // expect dotted-decimal notation in descending order
    Assert.assertEquals("192.168.1.8", groups.get(0).getKey());
    Assert.assertEquals("192.168.1.7", groups.get(1).getKey());
    Assert.assertEquals("192.168.1.6", groups.get(2).getKey());
    Assert.assertEquals("192.168.1.5", groups.get(3).getKey());
    Assert.assertEquals("192.168.1.4", groups.get(4).getKey());
    Assert.assertEquals("192.168.1.3", groups.get(5).getKey());
    Assert.assertEquals("192.168.1.2", groups.get(6).getKey());
    Assert.assertEquals("192.168.1.1", groups.get(7).getKey());
  }

  @Override
  public void group_by_returns_results_in_groups() throws Exception {
    // Group by test case, default order is count descending
    GroupRequest request = JSONUtils.INSTANCE.load(groupByQuery, GroupRequest.class);
    GroupResponse response = dao.group(request);
    Assert.assertEquals("is_alert", response.getGroupedBy());
    List<GroupResult> isAlertGroups = response.getGroupResults();
    Assert.assertEquals(2, isAlertGroups.size());

    // isAlert == true group
    GroupResult trueGroup = isAlertGroups.get(0);
    Assert.assertEquals("true", trueGroup.getKey());
    Assert.assertEquals(6, trueGroup.getTotal());
    Assert.assertEquals("latitude", trueGroup.getGroupedBy());
    Assert.assertEquals(198.0, trueGroup.getScore(), 0.00001);
    List<GroupResult> trueLatitudeGroups = trueGroup.getGroupResults();
    Assert.assertEquals(2, trueLatitudeGroups.size());


    // isAlert == true && latitude == 48.5839 group
    GroupResult trueLatitudeGroup2 = trueLatitudeGroups.get(0);
    Assert.assertEquals(48.5839, Double.parseDouble(trueLatitudeGroup2.getKey()), 0.00001);
    Assert.assertEquals(5, trueLatitudeGroup2.getTotal());
    Assert.assertEquals(148.0, trueLatitudeGroup2.getScore(), 0.00001);

    // isAlert == true && latitude == 48.0001 group
    GroupResult trueLatitudeGroup1 = trueLatitudeGroups.get(1);
    Assert.assertEquals(48.0001, Double.parseDouble(trueLatitudeGroup1.getKey()), 0.00001);
    Assert.assertEquals(1, trueLatitudeGroup1.getTotal());
    Assert.assertEquals(50.0, trueLatitudeGroup1.getScore(), 0.00001);

    // isAlert == false group
    GroupResult falseGroup = isAlertGroups.get(1);
    Assert.assertEquals("false", falseGroup.getKey());
    Assert.assertEquals("latitude", falseGroup.getGroupedBy());
    Assert.assertEquals(130.0, falseGroup.getScore(), 0.00001);
    List<GroupResult> falseLatitudeGroups = falseGroup.getGroupResults();
    Assert.assertEquals(2, falseLatitudeGroups.size());

    // isAlert == false && latitude == 48.5839 group
    GroupResult falseLatitudeGroup2 = falseLatitudeGroups.get(0);
    Assert.assertEquals(48.5839, Double.parseDouble(falseLatitudeGroup2.getKey()), 0.00001);
    Assert.assertEquals(3, falseLatitudeGroup2.getTotal());
    Assert.assertEquals(80.0, falseLatitudeGroup2.getScore(), 0.00001);

    // isAlert == false && latitude == 48.0001 group
    GroupResult falseLatitudeGroup1 = falseLatitudeGroups.get(1);
    Assert.assertEquals(48.0001, Double.parseDouble(falseLatitudeGroup1.getKey()), 0.00001);
    Assert.assertEquals(1, falseLatitudeGroup1.getTotal());
    Assert.assertEquals(50.0, falseLatitudeGroup1.getScore(), 0.00001);
  }

  @Override
  public void group_by_returns_results_in_sorted_groups() throws Exception {
    // Group by with sorting test case where is_alert is sorted by count ascending and ip_src_addr is sorted by term descending
    GroupRequest request = JSONUtils.INSTANCE.load(sortedGroupByQuery, GroupRequest.class);
    GroupResponse response = dao.group(request);
    Assert.assertEquals("is_alert", response.getGroupedBy());
    List<GroupResult> isAlertGroups = response.getGroupResults();
    Assert.assertEquals(2, isAlertGroups.size());

    // isAlert == false group
    GroupResult falseGroup = isAlertGroups.get(0);
    Assert.assertEquals(4, falseGroup.getTotal());
    Assert.assertEquals("ip_src_addr", falseGroup.getGroupedBy());
    List<GroupResult> falseIpSrcAddrGroups = falseGroup.getGroupResults();
    Assert.assertEquals(4, falseIpSrcAddrGroups.size());

    // isAlert == false && ip_src_addr == 192.168.1.8 group
    GroupResult falseIpSrcAddrGroup1 = falseIpSrcAddrGroups.get(0);
    Assert.assertEquals("192.168.1.8", falseIpSrcAddrGroup1.getKey());
    Assert.assertEquals(1, falseIpSrcAddrGroup1.getTotal());
    Assert.assertNull(falseIpSrcAddrGroup1.getGroupedBy());
    Assert.assertNull(falseIpSrcAddrGroup1.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.7 group
    GroupResult falseIpSrcAddrGroup2 = falseIpSrcAddrGroups.get(1);
    Assert.assertEquals("192.168.1.7", falseIpSrcAddrGroup2.getKey());
    Assert.assertEquals(1, falseIpSrcAddrGroup2.getTotal());
    Assert.assertNull(falseIpSrcAddrGroup2.getGroupedBy());
    Assert.assertNull(falseIpSrcAddrGroup2.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.6 group
    GroupResult falseIpSrcAddrGroup3 = falseIpSrcAddrGroups.get(2);
    Assert.assertEquals("192.168.1.6", falseIpSrcAddrGroup3.getKey());
    Assert.assertEquals(1, falseIpSrcAddrGroup3.getTotal());
    Assert.assertNull(falseIpSrcAddrGroup3.getGroupedBy());
    Assert.assertNull(falseIpSrcAddrGroup3.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.2 group
    GroupResult falseIpSrcAddrGroup4 = falseIpSrcAddrGroups.get(3);
    Assert.assertEquals("192.168.1.2", falseIpSrcAddrGroup4.getKey());
    Assert.assertEquals(1, falseIpSrcAddrGroup4.getTotal());
    Assert.assertNull(falseIpSrcAddrGroup4.getGroupedBy());
    Assert.assertNull(falseIpSrcAddrGroup4.getGroupResults());

    // isAlert == false group
    GroupResult trueGroup = isAlertGroups.get(1);
    Assert.assertEquals(6, trueGroup.getTotal());
    Assert.assertEquals("ip_src_addr", trueGroup.getGroupedBy());
    List<GroupResult> trueIpSrcAddrGroups = trueGroup.getGroupResults();
    Assert.assertEquals(4, trueIpSrcAddrGroups.size());

    // isAlert == false && ip_src_addr == 192.168.1.5 group
    GroupResult trueIpSrcAddrGroup1 = trueIpSrcAddrGroups.get(0);
    Assert.assertEquals("192.168.1.5", trueIpSrcAddrGroup1.getKey());
    Assert.assertEquals(1, trueIpSrcAddrGroup1.getTotal());
    Assert.assertNull(trueIpSrcAddrGroup1.getGroupedBy());
    Assert.assertNull(trueIpSrcAddrGroup1.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.4 group
    GroupResult trueIpSrcAddrGroup2 = trueIpSrcAddrGroups.get(1);
    Assert.assertEquals("192.168.1.4", trueIpSrcAddrGroup2.getKey());
    Assert.assertEquals(1, trueIpSrcAddrGroup2.getTotal());
    Assert.assertNull(trueIpSrcAddrGroup2.getGroupedBy());
    Assert.assertNull(trueIpSrcAddrGroup2.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.3 group
    GroupResult trueIpSrcAddrGroup3 = trueIpSrcAddrGroups.get(2);
    Assert.assertEquals("192.168.1.3", trueIpSrcAddrGroup3.getKey());
    Assert.assertEquals(1, trueIpSrcAddrGroup3.getTotal());
    Assert.assertNull(trueIpSrcAddrGroup3.getGroupedBy());
    Assert.assertNull(trueIpSrcAddrGroup3.getGroupResults());

    // isAlert == false && ip_src_addr == 192.168.1.1 group
    GroupResult trueIpSrcAddrGroup4 = trueIpSrcAddrGroups.get(3);
    Assert.assertEquals("192.168.1.1", trueIpSrcAddrGroup4.getKey());
    Assert.assertEquals(3, trueIpSrcAddrGroup4.getTotal());
    Assert.assertNull(trueIpSrcAddrGroup4.getGroupedBy());
    Assert.assertNull(trueIpSrcAddrGroup4.getGroupResults());
  }

  @Test
  public void throws_exception_on_aggregation_queries_on_non_string_non_numeric_fields()
      throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Failed to execute search");
    GroupRequest request = JSONUtils.INSTANCE.load(badGroupQuery, GroupRequest.class);
    dao.group(request);
  }
}
