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

package org.apache.metron.elasticsearch.dao;

import org.apache.metron.elasticsearch.utils.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.FieldMapping;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ElasticsearchColumnMetadata class.
 */
public class ElasticsearchColumnMetadataDaoTest {

  /**
   * @param indices The names of all indices that will exist.
   * @return An object to test.
   */
  public ElasticsearchColumnMetadataDao setup(String[] indices) {
    return setup(indices, new HashMap<>());
  }

  /**
   * @param indices The names of all indices that will exist.
   * @param mappings The index mappings.
   * @return An object to test.
   */
  public ElasticsearchColumnMetadataDao setup(
          String[] indices,
          Map<String, FieldMapping> mappings) {
    ElasticsearchClient client = new ElasticsearchClient(mock(RestClient.class), mock(RestHighLevelClient.class)) {
      @Override
      public String[] getIndices() throws IOException {
        return indices;
      }

      @Override
      public Map<String, FieldMapping> getMappings(String[] indices) throws IOException {
        return mappings;
      }
    };
    return new ElasticsearchColumnMetadataDao(client);
  }

  @Test
  public void testGetOneLatestIndex() throws IOException {

    // setup
    String[] existingIndices = new String[] {
            "bro_index_2017.10.03.19",
            "bro_index_2017.10.03.20",
            "bro_index_2017.10.03.21",
            "snort_index_2017.10.03.19",
            "snort_index_2017.10.03.20",
            "snort_index_2017.10.03.21"
    };
    ElasticsearchColumnMetadataDao dao = setup(existingIndices);

    // get the latest indices
    List<String> args = Collections.singletonList("bro");
    String[] actual = dao.getLatestIndices(args);

    // validation
    String [] expected = new String[] { "bro_index_2017.10.03.21" };
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testGetLatestIndices() throws IOException {
    // setup
    String[] existingIndices = new String[] {
            "bro_index_2017.10.03.19",
            "bro_index_2017.10.03.20",
            "bro_index_2017.10.03.21",
            "snort_index_2017.10.03.19",
            "snort_index_2017.10.03.19",
            "snort_index_2017.10.03.21"
    };
    ElasticsearchColumnMetadataDao dao = setup(existingIndices);

    // get the latest indices
    List<String> args = Arrays.asList("bro", "snort");
    String[] actual = dao.getLatestIndices(args);

    // validation
    String [] expected = new String[] { "bro_index_2017.10.03.21", "snort_index_2017.10.03.21" };
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testLatestIndicesWhereNoneExist() throws IOException {

    // setup - there are no existing indices
    String[] existingIndices = new String[] {};
    ElasticsearchColumnMetadataDao dao = setup(existingIndices);

    // get the latest indices
    List<String> args = Arrays.asList("bro", "snort");
    String[] actual = dao.getLatestIndices(args);

    // validation
    String [] expected = new String[] {};
    assertArrayEquals(expected, actual);
  }
}
