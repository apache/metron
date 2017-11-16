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
import java.util.Iterator;
import java.util.Optional;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.indexing.dao.search.GroupResult;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.integration.InMemoryComponent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
   * {"source:type": "snort", "ip_src_addr":"192.168.1.6", "ip_src_port": 8005, "long_field": 10000, "timestamp":6, "latitude": 48.5839, "score": 50.0, "is_alert":false, "location_point": "50.0,7.7455", "snort_field": 10, "duplicate_name_field": 1, "guid":"snort_1"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8004, "long_field": 10000, "timestamp":7, "latitude": 48.5839, "score": 10.0, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 20, "duplicate_name_field": 2, "guid":"snort_2"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.7", "ip_src_port": 8003, "long_field": 10000, "timestamp":8, "latitude": 48.5839, "score": 20.0, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 30, "duplicate_name_field": 3, "guid":"snort_3"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8002, "long_field": 20000, "timestamp":9, "latitude": 48.0001, "score": 50.0, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 40, "duplicate_name_field": 4, "guid":"snort_4"},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.8", "ip_src_port": 8001, "long_field": 10000, "timestamp":10, "latitude": 48.5839, "score": 10.0, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 50, "duplicate_name_field": 5, "guid":"snort_5"}
   * ]
   */
  @Multiline
  public static String snortData;

  /**
   * [
   *{"guid":"meta_1","alert":[{"guid":"bro_1"}],"average":"5.0","min":"5.0","median":"5.0","max":"5.0","count":"1.0","sum":"5.0"},
   *{"guid":"meta_2","alert":[{"guid":"bro_1"},{"guid":"bro_2"},{"guid":"snort_1"}],"average":"5.0","min":"0.0","median":"5.0","max":"10.0","count":"3.0","sum":"15.0"}
   * ]
   */
  @Multiline
  public static String metaAlertData;

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
  public static String allQuery;

  /**
   * {
   * "guid": "bro-3",
   * "sensorType": "bro"
   * }
   */
  @Multiline
  public static String findOneGuidQuery;

  /**
   * [
   * {
   * "guid": "bro-1",
   * "sensorType": "bro"
   * },
   * {
   * "guid": "bro-2",
   * "sensorType": "bro"
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
   * "indices": ["bro", "snort", "metaalert"],
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
   * "indices": ["bro", "snort", "metaalert"],
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
   * "indices": ["bro", "snort", "metaalert"],
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

  @Test
  public void test() throws Exception {
    //All Query Testcase
    {
      SearchRequest request = JSONUtils.INSTANCE.load(allQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      for(int i = 0;i < 5;++i) {
        Assert.assertEquals("snort", results.get(i).getSource().get("source:type"));
        Assert.assertEquals(10-i, results.get(i).getSource().get("timestamp"));
      }
      for(int i = 5;i < 10;++i) {
        Assert.assertEquals("bro", results.get(i).getSource().get("source:type"));
        Assert.assertEquals(10-i, results.get(i).getSource().get("timestamp"));
      }
    }
    //Find One Guid Testcase
    {
      GetRequest request = JSONUtils.INSTANCE.load(findOneGuidQuery, GetRequest.class);
      Optional<Map<String, Object>> response = dao.getLatestResult(request);
      Assert.assertTrue(response.isPresent());
      Map<String, Object> doc = response.get();
      Assert.assertEquals("bro", doc.get("source:type"));
      Assert.assertEquals(3, doc.get("timestamp"));
    }
    //Get All Latest Guid Testcase
    {
      List<GetRequest> request = JSONUtils.INSTANCE.load(getAllLatestQuery, new TypeReference<List<GetRequest>>() {
      });
      Iterator<Document> response = dao.getAllLatest(request).iterator();
      Document bro2 = response.next();
      Assert.assertEquals("bro_1", bro2.getDocument().get("guid"));
      Assert.assertEquals("bro", bro2.getDocument().get("source:type"));
      Document snort2 = response.next();
      Assert.assertEquals("bro_2", snort2.getDocument().get("guid"));
      Assert.assertEquals("bro", snort2.getDocument().get("source:type"));
      Assert.assertFalse(response.hasNext());
    }
    //Filter test case
    {
      SearchRequest request = JSONUtils.INSTANCE.load(filterQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(3, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
      Assert.assertEquals(9, results.get(0).getSource().get("timestamp"));
      Assert.assertEquals("snort", results.get(1).getSource().get("source:type"));
      Assert.assertEquals(7, results.get(1).getSource().get("timestamp"));
      Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
      Assert.assertEquals(1, results.get(2).getSource().get("timestamp"));
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
    //pagination test case
    {
      SearchRequest request = JSONUtils.INSTANCE.load(paginationQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      Assert.assertEquals(3, results.size());
      Assert.assertEquals("snort", results.get(0).getSource().get("source:type"));
      Assert.assertEquals(6, results.get(0).getSource().get("timestamp"));
      Assert.assertEquals("bro", results.get(1).getSource().get("source:type"));
      Assert.assertEquals(5, results.get(1).getSource().get("timestamp"));
      Assert.assertEquals("bro", results.get(2).getSource().get("source:type"));
      Assert.assertEquals(4, results.get(2).getSource().get("timestamp"));
    }
    //Index query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(indexQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(5, response.getTotal());
      List<SearchResult> results = response.getResults();
      for(int i = 5,j=0;i > 0;i--,j++) {
        Assert.assertEquals("bro", results.get(j).getSource().get("source:type"));
        Assert.assertEquals(i, results.get(j).getSource().get("timestamp"));
      }
    }
    //Facet query including all field types
    {
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
    //Bad facet query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(badFacetQuery, SearchRequest.class);
      try {
        dao.search(request);
        Assert.fail("Exception expected, but did not come.");
      }
      catch(InvalidSearchException ise) {
        Assert.assertEquals("Could not execute search", ise.getMessage());
      }
    }
    //Disabled facet query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(disabledFacetQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertNull(response.getFacetCounts());
    }
    //Exceeded maximum results query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(exceededMaxResultsQuery, SearchRequest.class);
      try {
        dao.search(request);
        Assert.fail("Exception expected, but did not come.");
      }
      catch(InvalidSearchException ise) {
        Assert.assertEquals("Search result size must be less than 100", ise.getMessage());
      }
    }
    // getColumnMetadata with multiple indices
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
      Assert.assertEquals(13, fieldTypes.size());
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("guid"));
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("score"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("duplicate_name_field"));
    }
    // getColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(12, fieldTypes.size());
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("bro_field"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(12, fieldTypes.size());
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
    }
    // getColumnMetadata with an index that doesn't exist
    {
      Map<String, FieldType> fieldTypes = dao.getColumnMetadata(Collections.singletonList("someindex"));
      Assert.assertEquals(0, fieldTypes.size());
    }
    //Fields query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(fieldsQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(10, response.getTotal());
      List<SearchResult> results = response.getResults();
      for(int i = 0;i < 5;++i) {
        Map<String, Object> source = results.get(i).getSource();
        Assert.assertEquals(1, source.size());
        Assert.assertNotNull(source.get("ip_src_addr"));
      }
      for(int i = 5;i < 10;++i) {
        Map<String, Object> source = results.get(i).getSource();
        Assert.assertEquals(1, source.size());
        Assert.assertNotNull(source.get("ip_src_addr"));
      }
    }
    //Meta Alerts Fields query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(metaAlertsFieldQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(2, response.getTotal());
      List<SearchResult> results = response.getResults();
      for (int i = 0;i < 2;++i) {
        Map<String, Object> source = results.get(i).getSource();
        Assert.assertEquals(1, source.size());
        Assert.assertEquals(source.get("guid"), "meta_" + (i + 1));
      }
    }
    //No results fields query
    {
      SearchRequest request = JSONUtils.INSTANCE.load(noResultsFieldsQuery, SearchRequest.class);
      SearchResponse response = dao.search(request);
      Assert.assertEquals(0, response.getTotal());
    }
    // Group by test case, default order is count descending
    {
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
    // Group by with sorting test case where is_alert is sorted by count ascending and ip_src_addr is sorted by term descending
    {
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
    //Bad group query
    {
      GroupRequest request = JSONUtils.INSTANCE.load(badGroupQuery, GroupRequest.class);
      try {
        dao.group(request);
        Assert.fail("Exception expected, but did not come.");
      }
      catch(InvalidSearchException ise) {
        Assert.assertEquals("Could not execute search", ise.getMessage());
      }
    }
    //Group by IP query
    {
      {
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
