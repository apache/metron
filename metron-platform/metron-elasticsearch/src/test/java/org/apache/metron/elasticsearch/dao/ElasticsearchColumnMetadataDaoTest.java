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

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    return setup(indices, ImmutableOpenMap.of());
  }

  /**
   * @param indices The names of all indices that will exist.
   * @param mappings The index mappings.
   * @return An object to test.
   */
  public ElasticsearchColumnMetadataDao setup(
          String[] indices,
          ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings) {

    AdminClient adminClient = mock(AdminClient.class);
    IndicesAdminClient indicesAdminClient = mock(IndicesAdminClient.class);
    GetIndexRequestBuilder getIndexRequestBuilder = mock(GetIndexRequestBuilder.class);
    GetIndexResponse getIndexResponse = mock(GetIndexResponse.class);
    ActionFuture getMappingsActionFuture = mock(ActionFuture.class);
    GetMappingsResponse getMappingsResponse = mock(GetMappingsResponse.class);

    // setup the mocks so that a set of indices are available to the DAO
    when(adminClient.indices()).thenReturn(indicesAdminClient);
    when(indicesAdminClient.prepareGetIndex()).thenReturn(getIndexRequestBuilder);
    when(getIndexRequestBuilder.setFeatures()).thenReturn(getIndexRequestBuilder);
    when(getIndexRequestBuilder.get()).thenReturn(getIndexResponse);
    when(getIndexResponse.getIndices()).thenReturn(indices);

    // setup the mocks so that a set of mappings are available to the DAO
    when(indicesAdminClient.getMappings(any())).thenReturn(getMappingsActionFuture);
    when(getMappingsActionFuture.actionGet()).thenReturn(getMappingsResponse);
    when(getMappingsResponse.getMappings()).thenReturn(mappings);

    return new ElasticsearchColumnMetadataDao(adminClient);
  }

  @Test
  public void testGetOneLatestIndex() {

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
  public void testGetLatestIndices() {
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
  public void testLatestIndicesWhereNoneExist() {

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
