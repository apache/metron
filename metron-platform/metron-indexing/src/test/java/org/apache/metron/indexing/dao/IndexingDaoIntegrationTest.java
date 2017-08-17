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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.integration.InMemoryComponent;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class IndexingDaoIntegrationTest {
  /**
   * [
   * {"source:type": "bro", "ip_src_addr":"192.168.1.1", "ip_src_port": 8010, "long_field": 10000, "timestamp":1, "latitude": 48.5839, "double_field": 1.00001, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 1", "duplicate_name_field": "data 1"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.2", "ip_src_port": 8009, "long_field": 20000, "timestamp":2, "latitude": 48.0001, "double_field": 1.00002, "is_alert":false, "location_point": "48.5839,7.7455", "bro_field": "bro data 2", "duplicate_name_field": "data 2"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.3", "ip_src_port": 8008, "long_field": 10000, "timestamp":3, "latitude": 48.5839, "double_field": 1.00002, "is_alert":true, "location_point": "50.0,7.7455", "bro_field": "bro data 3", "duplicate_name_field": "data 3"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.4", "ip_src_port": 8007, "long_field": 10000, "timestamp":4, "latitude": 48.5839, "double_field": 1.00002, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 4", "duplicate_name_field": "data 4"},
   * {"source:type": "bro", "ip_src_addr":"192.168.1.5", "ip_src_port": 8006, "long_field": 10000, "timestamp":5, "latitude": 48.5839, "double_field": 1.00001, "is_alert":true, "location_point": "48.5839,7.7455", "bro_field": "bro data 5", "duplicate_name_field": "data 5"}
   * ]
   */
  @Multiline
  public static String broData;

  /**
   * [
   * {"source:type": "snort", "ip_src_addr":"192.168.1.6", "ip_src_port": 8005, "long_field": 10000, "timestamp":6, "latitude": 48.5839, "double_field": 1.00001, "is_alert":false, "location_point": "50.0,7.7455", "snort_field": 10, "duplicate_name_field": 1},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8004, "long_field": 10000, "timestamp":7, "latitude": 48.5839, "double_field": 1.00002, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 20, "duplicate_name_field": 2},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.7", "ip_src_port": 8003, "long_field": 10000, "timestamp":8, "latitude": 48.5839, "double_field": 1.00001, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 30, "duplicate_name_field": 3},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.1", "ip_src_port": 8002, "long_field": 20000, "timestamp":9, "latitude": 48.0001, "double_field": 1.00002, "is_alert":true, "location_point": "48.5839,7.7455", "snort_field": 40, "duplicate_name_field": 4},
   * {"source:type": "snort", "ip_src_addr":"192.168.1.8", "ip_src_port": 8001, "long_field": 10000, "timestamp":10, "latitude": 48.5839, "double_field": 1.00001, "is_alert":false, "location_point": "48.5839,7.7455", "snort_field": 50, "duplicate_name_field": 5}
   * ]
   */
  @Multiline
  public static String snortData;

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
   * "facetFields": ["source:type", "ip_src_addr", "ip_src_port", "long_field", "timestamp", "latitude", "double_field", "is_alert"],
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

  protected IndexDao dao;
  protected InMemoryComponent indexComponent;

  @Before
  public void setup() throws Exception {
    indexComponent = startIndex();
    loadTestData();
    dao = createDao();
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
      Map<String, Long> doubleFieldCounts = facetCounts.get("double_field");
      Assert.assertEquals(2, doubleFieldCounts.size());
      List<String> doubleFieldKeys = new ArrayList<>(doubleFieldCounts.keySet());
      Collections.sort(doubleFieldKeys);
      Assert.assertEquals(1.00001, Double.parseDouble(doubleFieldKeys.get(0)), 0.00001);
      Assert.assertEquals(1.00002, Double.parseDouble(doubleFieldKeys.get(1)), 0.00001);
      Assert.assertEquals(new Long(5), doubleFieldCounts.get(doubleFieldKeys.get(0)));
      Assert.assertEquals(new Long(5), doubleFieldCounts.get(doubleFieldKeys.get(1)));
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
      Map<String, Map<String, FieldType>> fieldTypes = dao.getColumnMetadata(Arrays.asList("bro", "snort"));
      Assert.assertEquals(2, fieldTypes.size());
      Map<String, FieldType> broTypes = fieldTypes.get("bro");
      Assert.assertEquals(11, broTypes.size());
      Assert.assertEquals(FieldType.STRING, broTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, broTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, broTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, broTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, broTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, broTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, broTypes.get("double_field"));
      Assert.assertEquals(FieldType.BOOLEAN, broTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, broTypes.get("location_point"));
      Assert.assertEquals(FieldType.STRING, broTypes.get("bro_field"));
      Assert.assertEquals(FieldType.STRING, broTypes.get("duplicate_name_field"));
      Map<String, FieldType> snortTypes = fieldTypes.get("snort");
      Assert.assertEquals(11, snortTypes.size());
      Assert.assertEquals(FieldType.STRING, snortTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, snortTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, snortTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, snortTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, snortTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, snortTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, snortTypes.get("double_field"));
      Assert.assertEquals(FieldType.BOOLEAN, snortTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, snortTypes.get("location_point"));
      Assert.assertEquals(FieldType.INTEGER, snortTypes.get("snort_field"));
      Assert.assertEquals(FieldType.INTEGER, snortTypes.get("duplicate_name_field"));
    }
    // getColumnMetadata with only bro
    {
      Map<String, Map<String, FieldType>> fieldTypes = dao.getColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(1, fieldTypes.size());
      Map<String, FieldType> broTypes = fieldTypes.get("bro");
      Assert.assertEquals(11, broTypes.size());
      Assert.assertEquals(FieldType.STRING, broTypes.get("bro_field"));
    }
    // getColumnMetadata with only snort
    {
      Map<String, Map<String, FieldType>> fieldTypes = dao.getColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(1, fieldTypes.size());
      Map<String, FieldType> snortTypes = fieldTypes.get("snort");
      Assert.assertEquals(11, snortTypes.size());
      Assert.assertEquals(FieldType.INTEGER, snortTypes.get("snort_field"));
    }
    // getCommonColumnMetadata with multiple Indices
    {
      Map<String, FieldType> fieldTypes = dao.getCommonColumnMetadata(Arrays.asList("bro", "snort"));
      // Should only return fields in both
      Assert.assertEquals(9, fieldTypes.size());
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("source:type"));
      Assert.assertEquals(FieldType.IP, fieldTypes.get("ip_src_addr"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("ip_src_port"));
      Assert.assertEquals(FieldType.LONG, fieldTypes.get("long_field"));
      Assert.assertEquals(FieldType.DATE, fieldTypes.get("timestamp"));
      Assert.assertEquals(FieldType.FLOAT, fieldTypes.get("latitude"));
      Assert.assertEquals(FieldType.DOUBLE, fieldTypes.get("double_field"));
      Assert.assertEquals(FieldType.BOOLEAN, fieldTypes.get("is_alert"));
      Assert.assertEquals(FieldType.OTHER, fieldTypes.get("location_point"));
    }
    // getCommonColumnMetadata with only bro
    {
      Map<String, FieldType> fieldTypes = dao.getCommonColumnMetadata(Collections.singletonList("bro"));
      Assert.assertEquals(11, fieldTypes.size());
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("bro_field"));
      Assert.assertEquals(FieldType.STRING, fieldTypes.get("duplicate_name_field"));
    }
    // getCommonColumnMetadata with only snort
    {
      Map<String, FieldType> fieldTypes = dao.getCommonColumnMetadata(Collections.singletonList("snort"));
      Assert.assertEquals(11, fieldTypes.size());
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("snort_field"));
      Assert.assertEquals(FieldType.INTEGER, fieldTypes.get("duplicate_name_field"));
    }
  }

  @After
  public void stop() throws Exception {
    indexComponent.stop();
  }

  protected abstract IndexDao createDao() throws Exception;
  protected abstract InMemoryComponent startIndex() throws Exception;
  protected abstract void loadTestData() throws Exception;
}
