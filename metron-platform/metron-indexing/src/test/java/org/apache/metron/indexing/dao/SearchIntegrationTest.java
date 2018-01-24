
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
package org.apache.metron.indexing.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.SortField;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.integration.InMemoryComponent;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class SearchIntegrationTest {
  /**
   * [
   * {"source:type": "bro", "ip_src_addr":"192.168.1.1", "ip_src_port": 8010, "long_field": 10000, "timestamp":1, "latitude": 48.5839, "score": 10.0, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 1", "duplicate_name_field": "data 1", "guid":"bro_1"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.2", "ip_src_port": 8009, "long_field": 20000, "timestamp":2, "latitude": 48.0001, "score": 50.0, "is_alert":false, "location_point": "48.5839,7.7455", "bro_field": "bro data 2", "duplicate_name_field": "data 2", "guid":"bro_2"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.3", "ip_src_port": 8008, "long_field": 10000, "timestamp":3, "latitude": 48.5839, "score": 20.0, "is_alert":true, "location_point": "50.0,7.7455", "bro_field": "bro data 3", "duplicate_name_field": "data 3", "guid":"bro_3"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.4", "ip_src_port": 8007, "long_field": 10000, "timestamp":4, "latitude": 48.5839, "score": 10.0, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 4", "duplicate_name_field": "data 4", "guid":"bro_4"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.5", "ip_src_port": 8006, "long_field": 10000, "timestamp":5, "latitude": 48.5839, "score": 98.0, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 5", "duplicate_name_field": "data 5", "guid":"bro_5"}
   * ]
   */
  @Multiline
  public static String broData;

  /**
   * [
   * {"source:type": "snort", "ip_src_addr":"192.168.1.6", "ip_src_port": 8005, "long_field": 10000, "timestamp":6, "latitude": 48.5839, "score": 50.0, "is_alert":false, "location_point": "50.0,7.7455", "snort_field": 10, "duplicate_name_field": 1, "guid":"snort_1", "threat:triage:score":10.0},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8004, "long_field": 10000, "timestamp":7, "latitude": 48.5839, "score": 10.0, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 20, "duplicate_name_field": 2, "guid":"snort_2", "threat:triage:score":20.0},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.7", "ip_src_port": 8003, "long_field": 10000, "timestamp":8, "latitude": 48.5839, "score": 20.0, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 30, "duplicate_name_field": 3, "guid":"snort_3"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8002, "long_field": 20000, "timestamp":9, "latitude": 48.0001, "score": 50.0, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 40, "duplicate_name_field": 4, "guid":"snort_4"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.8", "ip_src_port": 8001, "long_field": 10000, "timestamp":10, "latitude": 48.5839, "score": 10.0, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 50, "duplicate_name_field": 5, "guid":"snort_5"}
   * ]
   */
  @Multiline
  public static String snortData;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*:*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String allQuery;

  /**
   * {
   * "guid": "bro_3",
   * "sensorType": "bro"
   * }
   */
  @Multiline
  public static String findOneGuidQuery;

  /**
   * [
   * {
   * "guid": "bro_1",
   * "sensorType": "bro"
   * },
   * {
   * "guid": "snort_2",
   * "sensorType": "snort"
   * }
   * ]
   */
  @Multiline
  public static String getAllLatestQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "ip_src_addr:192.168.1.1",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String filterQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "ip_src_port",
   *     "sortOrder": "asc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String sortQuery;

  /**
   * {
   *  "indices": [
   *    "snort",
   *    "bro"
   *  ],
   * "query": "*",
   * "from": 0,
   * "size": 25,
   * "sort": [
   *    {
   *      "field": "threat:triage:score",
   *      "sortOrder": "asc"
   *    }
   *  ]
   * }
   */
  @Multiline
  public static String sortAscendingWithMissingFields;

  /**
   * {
   *  "indices": [
   *    "snort",
   *    "bro"
   *  ],
   * "query": "*",
   * "from": 0,
   * "size": 25,
   * "sort": [
   *    {
   *      "field": "threat:triage:score",
   *      "sortOrder": "desc"
   *    }
   *  ]
   * }
   */
  @Multiline
  public static String sortDescendingWithMissingFields;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 4,
   * "size": 3,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String paginationQuery;

  /**
   * {
   * "indices": ["bro"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String indexQuery;

  /**
   * {
   * "facetFields": ["source:type", "ip_src_addr", "ip_src_port", "long_field", "timestamp", "latitude", "score", "is_alert"],
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String facetQuery;

  /**
   * {
   * "facetFields": ["location_point"],
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String badFacetQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String disabledFacetQuery;

  /**
   * {
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 101,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String exceededMaxResultsQuery;

  /**
   * {
   * "fields": ["ip_src_addr"],
   * "indices": ["bro", "snort"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String fieldsQuery;

  /**
   * {
   * "fields": ["ip_src_addr"],
   * "indices": ["bro", "snort"],
   * "query": "ip_src_addr:192.168.1.9",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "timestamp",
   *     "sortOrder": "desc"
   *   }
   * ]
   * }
   */
  @Multiline
  public static String noResultsFieldsQuery;

  /**
   * {
   * "fields": ["guid"],
   * "indices": ["metaalert"],
   * "query": "*",
   * "from": 0,
   * "size": 10,
   * "sort": [
   *   {
   *     "field": "guid",
   *     "sortOrder": "asc"
   *   }
   * ]
   * }
   * }
   */
  @Multiline
  public static String metaAlertsFieldQuery;

  /**
   * {
   * "groups": [
   *   {
   *     "field":"is_alert"
   *   },
   *   {
   *     "field":"latitude"
   *   }
   * ],
   * "scoreField":"score",
   * "indices": ["bro", "snort"],
   * "query": "*"
   * }
   */
  @Multiline
  public static String groupByQuery;

  /**
   * {
   * "groups": [
   *   {
   *     "field":"is_alert",
   *     "order": {
   *       "groupOrderType": "count",
   *       "sortOrder": "ASC"
   *     }
   *   },
   *   {
   *     "field":"ip_src_addr",
   *     "order": {
   *       "groupOrderType": "term",
   *       "sortOrder": "DESC"
   *     }
   *   }
   * ],
   * "indices": ["bro", "snort"],
   * "query": "*"
   * }
   */
  @Multiline
  public static String sortedGroupByQuery;

  /**
   * {
   * "groups": [
   *   {
   *     "field":"location_point"
   *   }
   * ],
   * "indices": ["bro", "snort"],
   * "query": "*"
   * }
   */
  @Multiline
  public static String badGroupQuery;

  /**
   * {
   * "groups": [
   *   {
   *     "field":"ip_src_addr",
   *     "order": {
   *       "groupOrderType": "term",
   *       "sortOrder": "DESC"
   *     }
   *   }
   * ],
   * "indices": ["bro", "snort"],
   * "query": "*"
   * }
   */
  @Multiline
  public static String groupByIpQuery;

  protected static IndexDao dao;
  protected static InMemoryComponent indexComponent;

  @Before
  public synchronized void setup() throws Exception {
    if(dao == null && indexComponent == null) {
      indexComponent = startIndex();
      loadTestData();
      dao = createDao();
    }
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void find_one_guid() throws Exception {
    GetRequest request = JSONUtils.INSTANCE.load(findOneGuidQuery, GetRequest.class);
    Optional<Map<String, Object>> response = dao.getLatestResult(request);
    Assert.assertTrue(response.isPresent());
    Map<String, Object> doc = response.get();
    Assert.assertEquals("bro", doc.get("source:type"));
    Assert.assertEquals("3", doc.get("timestamp").toString());
  }

  @Test
  public void all_query_returns_all_results() throws Exception {
    //All Query Testcase
    {
      SearchRequest request = JSONUtils.INSTANCE.load(allQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals(10, results.size());
      for(int i = 0;i < 5;++i) {
        Assert.assertEquals("snort", results.get(i).getSource().get("source:type"));
        Assert.assertEquals((10 - i) + "", results.get(i).getSource().get("timestamp").toString());
      }
      for (int i = 5; i < 10; ++i) {
        Assert.assertEquals("bro", results.get(i).getSource().get("source:type"));
        Assert.assertEquals((10 - i) + "", results.get(i).getSource().get("timestamp").toString());
      }
    }
    //Get All Latest Guid Testcase
    {
      List<GetRequest> request = JSONUtils.INSTANCE.load(getAllLatestQuery, new TypeReference<List<GetRequest>>() {
      });
      Map<String, Document> docs = new HashMap<>();

      for(Document doc : dao.getAllLatest(request)) {
        docs.put(doc.getGuid(), doc);
      }
      Assert.assertEquals(2, docs.size());
      Assert.assertTrue(docs.keySet().contains("bro_1"));
      Assert.assertTrue(docs.keySet().contains("snort_2"));
      Assert.assertEquals("bro", docs.get("bro_1").getDocument().get("source:type"));
      Assert.assertEquals("snort", docs.get("snort_2").getDocument().get("source:type"));
//      for(Map.Entry<String, Document> kv : docs.entrySet()) {
//        Document d = kv.getValue();
//        Assert.assertEquals("bro", d.getDocument().get("source:type"));
//      }
    }
    //Filter test case
    {
      SearchRequest request = JSONUtils.INSTANCE.load(filterQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(3, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
      Assert.assertEquals("9", results.get(0).getSource().get("timestamp").toString());
      Assert.assertEquals("snort", results.get(1).getSource().get("source:type"));
      Assert.assertEquals("7", results.get(1).getSource().get("timestamp").toString());
      Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
      Assert.assertEquals("1", results.get(2).getSource().get("timestamp").toString());
    }
    //Sort test case
    {
      SearchRequest request = JSONUtils.INSTANCE.load(sortQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      for(int i = 8001;i < 8011;++i) {
        Assert.assertEquals(i, results.get(i-8001).getSource().get("ip_src_port"));
      }
    }
    //Sort descending with missing fields
    {
      SearchRequest request = JSONUtils.INSTANCE.load(sortDescendingWithMissingFields, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals(10, results.size());

      // validate sorted order - there are only 2 with a 'threat:triage:score'
      Assert.assertEquals("20.0", results.get(0).getSource().get("threat:triage:score").toString());
      Assert.assertEquals("10.0", results.get(1).getSource().get("threat:triage:score").toString());

      // the remaining are missing the 'threat:triage:score' and should be sorted last
      Assert.assertFalse(results.get(2).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(3).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(4).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(5).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(6).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(7).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(8).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(9).getSource().containsKey("threat:triage:score"));
    }
    //Sort ascending with missing fields
    {
      SearchRequest request = JSONUtils.INSTANCE.load(sortAscendingWithMissingFields, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals(10, results.size());

      // the remaining are missing the 'threat:triage:score' and should be sorted last
      Assert.assertFalse(results.get(0).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(1).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(2).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(3).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(4).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(5).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(6).getSource().containsKey("threat:triage:score"));
      Assert.assertFalse(results.get(7).getSource().containsKey("threat:triage:score"));

      // validate sorted order - there are only 2 with a 'threat:triage:score'
      Assert.assertEquals("10.0", results.get(8).getSource().get("threat:triage:score").toString());
      Assert.assertEquals("20.0", results.get(9).getSource().get("threat:triage:score").toString());
    }
    //pagination test case
    {
      SearchRequest request = JSONUtils.INSTANCE.load(paginationQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals(3, results.size());
      Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
      Assert.assertEquals("6", results.get(0).getSource().get("timestamp").toString());
      Assert.assertEquals("bro", results.get(1).getSource().get("source:type"));
      Assert.assertEquals("5", results.get(1).getSource().get("timestamp").toString());
      Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
      Assert.assertEquals("4", results.get(2).getSource().get("timestamp").toString());
    }
    //Index query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(indexQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(5, response.getTotal());
      List<SearchResult> results = response.getResults();
      for(int i = 5,j=0;i > 0;i--,j++) {
        Assert.assertEquals("bro", results.get(j).getSource().get("source:type"));
        Assert.assertEquals(i + "", results.get(j).getSource().get("timestamp").toString());
      }
    }
    //Facet query including all field types
    {
      SearchRequest request = JSONUtils.INSTANCE.load(facetQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());

      Map<String, Map<String, Long>> facetCounts = response.getFacetCounts();
      Assert.assertEquals(8, facetCounts.size());

      // source:type
      Map<String, Long> sourceTypeCounts = facetCounts.get("source:type");
      Assert.assertEquals(2, sourceTypeCounts.size());
      Assert.assertEquals(new Long(5), sourceTypeCounts.get("bro"));
      Assert.assertEquals(new Long(5), sourceTypeCounts.get("snort"));

      // ip_src_addr
      Map<String, Long> ipSrcAddrCounts = facetCounts.get("ip_src_addr");
      Assert.assertEquals(8, ipSrcAddrCounts.size());
      Assert.assertEquals(new Long(3), ipSrcAddrCounts.get("192.168.1.1"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.2"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.3"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.4"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.5"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.6"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.7"));
      Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.8"));

      // ip_src_port
      Map<String, Long> ipSrcPortCounts = facetCounts.get("ip_src_port");
      Assert.assertEquals(10, ipSrcPortCounts.size());
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8001"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8002"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8003"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8004"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8005"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8006"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8007"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8008"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8009"));
      Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8010"));

      // long_field
      Map<String, Long> longFieldCounts = facetCounts.get("long_field");
      Assert.assertEquals(2, longFieldCounts.size());
      Assert.assertEquals(new Long(8), longFieldCounts.get("10000"));
      Assert.assertEquals(new Long(2), longFieldCounts.get("20000"));

      // timestamp
      Map<String, Long> timestampCounts = facetCounts.get("timestamp");
      Assert.assertEquals(10, timestampCounts.size());
      Assert.assertEquals(new Long(1), timestampCounts.get("1"));
      Assert.assertEquals(new Long(1), timestampCounts.get("2"));
      Assert.assertEquals(new Long(1), timestampCounts.get("3"));
      Assert.assertEquals(new Long(1), timestampCounts.get("4"));
      Assert.assertEquals(new Long(1), timestampCounts.get("5"));
      Assert.assertEquals(new Long(1), timestampCounts.get("6"));
      Assert.assertEquals(new Long(1), timestampCounts.get("7"));
      Assert.assertEquals(new Long(1), timestampCounts.get("8"));
      Assert.assertEquals(new Long(1), timestampCounts.get("9"));
      Assert.assertEquals(new Long(1), timestampCounts.get("10"));

      // latitude
      Map<String, Long> latitudeCounts = facetCounts.get("latitude");
      Assert.assertEquals(2, latitudeCounts.size());
      List<String> latitudeKeys = new ArrayList<>(latitudeCounts.keySet());
      Collections.sort(latitudeKeys);
      Assert.assertEquals(48.0001, Double.parseDouble(latitudeKeys.get(0)), 0.00001);
      Assert.assertEquals(48.5839, Double.parseDouble(latitudeKeys.get(1)), 0.00001);
      Assert.assertEquals(new Long(2), latitudeCounts.get(latitudeKeys.get(0)));
      Assert.assertEquals(new Long(8), latitudeCounts.get(latitudeKeys.get(1)));

      // score
      Map<String, Long> scoreFieldCounts = facetCounts.get("score");
      Assert.assertEquals(4, scoreFieldCounts.size());
      List<String> scoreFieldKeys = new ArrayList<>(scoreFieldCounts.keySet());
      Collections.sort(scoreFieldKeys);
      Assert.assertEquals(10.0, Double.parseDouble(scoreFieldKeys.get(0)), 0.00001);
      Assert.assertEquals(20.0, Double.parseDouble(scoreFieldKeys.get(1)), 0.00001);
      Assert.assertEquals(50.0, Double.parseDouble(scoreFieldKeys.get(2)), 0.00001);
      Assert.assertEquals(98.0, Double.parseDouble(scoreFieldKeys.get(3)), 0.00001);
      Assert.assertEquals(new Long(4), scoreFieldCounts.get(scoreFieldKeys.get(0)));
      Assert.assertEquals(new Long(2), scoreFieldCounts.get(scoreFieldKeys.get(1)));
      Assert.assertEquals(new Long(3), scoreFieldCounts.get(scoreFieldKeys.get(2)));
      Assert.assertEquals(new Long(1), scoreFieldCounts.get(scoreFieldKeys.get(3)));

      // is_alert
      Map<String, Long> isAlertCounts = facetCounts.get("is_alert");
      Assert.assertEquals(2, isAlertCounts.size());
      Assert.assertEquals(new Long(6), isAlertCounts.get("true"));
      Assert.assertEquals(new Long(4), isAlertCounts.get("false"));
    }
    //Bad facet query
//    {
//      SearchRequest request = JSONUtils.INSTANCE.load(badFacetQuery, SearchRequest.class);
//      try {
//        dao.search(request);
//        Assert.fail("Exception expected, but did not come.");
//      }
//      catch(InvalidSearchException ise) {
//        // success
//      }
//    }
    //Disabled facet query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(disabledFacetQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertNull(response.getFacetCounts());
    }
  }

  @Test
  public void filter_query_filters_results() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(filterQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(3, response.getTotal());
    List<SearchResult> results = response.getResults();
    Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
    Assert.assertEquals("9", results.get(0).getSource().get("timestamp").toString());
    Assert.assertEquals("snort", results.get(1).getSource().get("source:type"));
    Assert.assertEquals("7", results.get(1).getSource().get("timestamp").toString());
    Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
    Assert.assertEquals("1", results.get(2).getSource().get("timestamp").toString());
  }

  @Test
  public void sort_query_sorts_results_ascending() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(sortQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(10, response.getTotal());
    List<SearchResult> results = response.getResults();
    for (int i = 8001; i < 8011; ++i) {
      Assert.assertEquals(i, results.get(i - 8001).getSource().get("ip_src_port"));
    }
  }

  @Test
  public void results_are_paginated() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(paginationQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(10, response.getTotal());
    List<SearchResult> results = response.getResults();
    Assert.assertEquals(3, results.size());
    Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
    Assert.assertEquals("6", results.get(0).getSource().get("timestamp").toString());
    Assert.assertEquals("bro", results.get(1).getSource().get("source:type"));
    Assert.assertEquals("5", results.get(1).getSource().get("timestamp").toString());
    Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
    Assert.assertEquals("4", results.get(2).getSource().get("timestamp").toString());
  }

  @Test
  public void returns_results_only_for_specified_indices() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(indexQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(5, response.getTotal());
    List<SearchResult> results = response.getResults();
    for (int i = 5, j = 0; i > 0; i--, j++) {
      Assert.assertEquals("bro", results.get(j).getSource().get("source:type"));
      Assert.assertEquals(i + "", results.get(j).getSource().get("timestamp").toString());
    }
  }

  @Test
  public void facet_query_yields_field_types() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(facetQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(10, response.getTotal());
    Map<String, Map<String, Long>> facetCounts = response.getFacetCounts();
    Assert.assertEquals(8, facetCounts.size());
    Map<String, Long> sourceTypeCounts = facetCounts.get("source:type");
    Assert.assertEquals(2, sourceTypeCounts.size());
    Assert.assertEquals(new Long(5), sourceTypeCounts.get("bro"));
    Assert.assertEquals(new Long(5), sourceTypeCounts.get("snort"));
    Map<String, Long> ipSrcAddrCounts = facetCounts.get("ip_src_addr");
    Assert.assertEquals(8, ipSrcAddrCounts.size());
    Assert.assertEquals(new Long(3), ipSrcAddrCounts.get("192.168.1.1"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.2"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.3"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.4"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.5"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.6"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.7"));
    Assert.assertEquals(new Long(1), ipSrcAddrCounts.get("192.168.1.8"));
    Map<String, Long> ipSrcPortCounts = facetCounts.get("ip_src_port");
    Assert.assertEquals(10, ipSrcPortCounts.size());
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8001"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8002"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8003"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8004"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8005"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8006"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8007"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8008"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8009"));
    Assert.assertEquals(new Long(1), ipSrcPortCounts.get("8010"));
    Map<String, Long> longFieldCounts = facetCounts.get("long_field");
    Assert.assertEquals(2, longFieldCounts.size());
    Assert.assertEquals(new Long(8), longFieldCounts.get("10000"));
    Assert.assertEquals(new Long(2), longFieldCounts.get("20000"));
    Map<String, Long> timestampCounts = facetCounts.get("timestamp");
    Assert.assertEquals(10, timestampCounts.size());
    Assert.assertEquals(new Long(1), timestampCounts.get("1"));
    Assert.assertEquals(new Long(1), timestampCounts.get("2"));
    Assert.assertEquals(new Long(1), timestampCounts.get("3"));
    Assert.assertEquals(new Long(1), timestampCounts.get("4"));
    Assert.assertEquals(new Long(1), timestampCounts.get("5"));
    Assert.assertEquals(new Long(1), timestampCounts.get("6"));
    Assert.assertEquals(new Long(1), timestampCounts.get("7"));
    Assert.assertEquals(new Long(1), timestampCounts.get("8"));
    Assert.assertEquals(new Long(1), timestampCounts.get("9"));
    Assert.assertEquals(new Long(1), timestampCounts.get("10"));
    Map<String, Long> latitudeCounts = facetCounts.get("latitude");
    Assert.assertEquals(2, latitudeCounts.size());
    List<String> latitudeKeys = new ArrayList<>(latitudeCounts.keySet());
    Collections.sort(latitudeKeys);
    Assert.assertEquals(48.0001, Double.parseDouble(latitudeKeys.get(0)), 0.00001);
    Assert.assertEquals(48.5839, Double.parseDouble(latitudeKeys.get(1)), 0.00001);
    Assert.assertEquals(new Long(2), latitudeCounts.get(latitudeKeys.get(0)));
    Assert.assertEquals(new Long(8), latitudeCounts.get(latitudeKeys.get(1)));
    Map<String, Long> scoreFieldCounts = facetCounts.get("score");
    Assert.assertEquals(4, scoreFieldCounts.size());
    List<String> scoreFieldKeys = new ArrayList<>(scoreFieldCounts.keySet());
    Collections.sort(scoreFieldKeys);
    Assert.assertEquals(10.0, Double.parseDouble(scoreFieldKeys.get(0)), 0.00001);
    Assert.assertEquals(20.0, Double.parseDouble(scoreFieldKeys.get(1)), 0.00001);
    Assert.assertEquals(50.0, Double.parseDouble(scoreFieldKeys.get(2)), 0.00001);
    Assert.assertEquals(98.0, Double.parseDouble(scoreFieldKeys.get(3)), 0.00001);
    Assert.assertEquals(new Long(4), scoreFieldCounts.get(scoreFieldKeys.get(0)));
    Assert.assertEquals(new Long(2), scoreFieldCounts.get(scoreFieldKeys.get(1)));
    Assert.assertEquals(new Long(3), scoreFieldCounts.get(scoreFieldKeys.get(2)));
    Assert.assertEquals(new Long(1), scoreFieldCounts.get(scoreFieldKeys.get(3)));
    Map<String, Long> isAlertCounts = facetCounts.get("is_alert");
    Assert.assertEquals(2, isAlertCounts.size());
    Assert.assertEquals(new Long(6), isAlertCounts.get("true"));
    Assert.assertEquals(new Long(4), isAlertCounts.get("false"));
  }

  @Test
  public void disabled_facet_query_returns_null_count() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(disabledFacetQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertNull(response.getFacetCounts());
  }

  @Test
  public void exceeding_max_resulsts_throws_exception() throws Exception {
    thrown.expect(InvalidSearchException.class);
    thrown.expectMessage("Search result size must be less than 100");
    SearchRequest request = JSONUtils.INSTANCE.load(exceededMaxResultsQuery, SearchRequest.class);
    dao.search(request);
  }

  @Test
  public abstract void returns_column_data_for_multiple_indices() throws Exception;

  @Test
  public abstract void returns_column_metadata_for_specified_indices() throws Exception;

  @Test
  public void column_metadata_for_missing_index() throws Exception {
    // getColumnMetadata with an index that doesn't exist
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("someindex"));
      Assert.assertEquals(0, fieldTypes.size());
    }
  }

  @Test
  public void no_results_returned_when_query_does_not_match() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(noResultsFieldsQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(0, response.getTotal());
  }

  @Test
  public abstract void group_by_ip_query() throws Exception;

  @Test
  public abstract void group_by_returns_results_in_groups() throws Exception;

  @Test
  public abstract void group_by_returns_results_in_sorted_groups() throws Exception;

  @Test
  public void queries_fields() throws Exception {
    SearchRequest request = JSONUtils.INSTANCE.load(fieldsQuery, SearchRequest.class);
    SearchResponse response = dao.search(request);
    Assert.assertEquals(10, response.getTotal());
    List<SearchResult> results = response.getResults();
    for (int i = 0; i < 5; ++i) {
      Map<String, Object> source = results.get(i).getSource();
      Assert.assertEquals(1, source.size());
      Assert.assertNotNull(source.get("ip_src_addr"));
    }
    for (int i = 5; i < 10; ++i) {
      Map<String, Object> source = results.get(i).getSource();
      Assert.assertEquals(1, source.size());
      Assert.assertNotNull(source.get("ip_src_addr"));
    }
  }

  @AfterClass
  public static void stop() throws Exception {
    indexComponent.stop();
  }

  protected abstract IndexDao createDao() throws Exception;
  protected abstract InMemoryComponent startIndex() throws Exception;
  protected abstract void loadTestData() throws Exception;
}