/*
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

package org.apache.metron.solr.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolrClientFactoryTest {

  @Test
  public void testGetZkHostsSingle() {
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(SOLR_ZOOKEEPER, "   zookeeper:2181   ");
    }};

    List<String> actual = SolrClientFactory.getZkHosts(globalConfig);
    List<String> expected = new ArrayList<>();
    expected.add("zookeeper:2181");
    assertEquals(expected, actual);
  }

  @Test
  public void testGetZkHostsMultiple() {
    Map<String, Object> globalConfig = new HashMap<String, Object>() {{
      put(SOLR_ZOOKEEPER, "   zookeeper:2181    ,   zookeeper2:2181    ");
    }};

    List<String> actual = SolrClientFactory.getZkHosts(globalConfig);
    List<String> expected = new ArrayList<>();
    expected.add("zookeeper:2181");
    expected.add("zookeeper2:2181");
    assertEquals(expected, actual);
  }
}
