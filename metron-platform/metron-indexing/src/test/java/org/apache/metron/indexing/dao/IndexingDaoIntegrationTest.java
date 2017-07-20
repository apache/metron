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
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.SearchResult;
import org.apache.metron.integration.InMemoryComponent;
import org.json.simple.parser.ParseException;
import org.junit.*;

import java.util.List;

public abstract class IndexingDaoIntegrationTest {
  /**
   * [
   * {"source:type": "bro", "ip_src_addr":"192.168.1.1", "ip_src_port": 8010, "timestamp":1, "rejected":true},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.2", "ip_src_port": 8009, "timestamp":2, "rejected":false},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.3", "ip_src_port": 8008, "timestamp":3, "rejected":true},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.4", "ip_src_port": 8007, "timestamp":4, "rejected":false},
   * {"source:type": "bro" "ip_src_addr":"192.168.1.5", "ip_src_port": 8006, "timestamp":5, "rejected":true}
   * ]
   */
  @Multiline
  public static String broData;

  /**
   * [
   * {"source:type": "snort" "ip_src_addr":"192.168.1.6", "ip_src_port": 8005, "timestamp":6, "is_alert":false},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.1", "ip_src_port": 8004, "timestamp":7, "is_alert":true},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.7", "ip_src_port": 8003, "timestamp":8, "is_alert":false},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.1", "ip_src_port": 8002, "timestamp":9, "is_alert":true},
   * {"source:type": "snort" "ip_src_addr":"192.168.1.8", "ip_src_port": 8001, "timestamp":10, "is_alert":false}
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
  }

  @AfterClass
  public static void stop() throws Exception {
    indexComponent.stop();
  }

  protected abstract IndexDao createDao() throws Exception;
  protected abstract InMemoryComponent startIndex() throws Exception;
  protected abstract void loadTestData() throws Exception;
}
