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

import org.apache.metron.elasticsearch.utils.ElasticsearchUtils;
import org.apache.metron.indexing.dao.search.FieldType;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.INDEX_NAME_DELIMITER;

/**
 * Responsible for retrieving column-level metadata for Elasticsearch search indices.
 */
public class ElasticsearchColumnMetadataDao implements ColumnMetadataDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Map<String, FieldType> elasticsearchTypeMap;
  static {
    Map<String, FieldType> fieldTypeMap = new HashMap<>();
    fieldTypeMap.put("string", FieldType.STRING);
    fieldTypeMap.put("ip", FieldType.IP);
    fieldTypeMap.put("integer", FieldType.INTEGER);
    fieldTypeMap.put("long", FieldType.LONG);
    fieldTypeMap.put("date", FieldType.DATE);
    fieldTypeMap.put("float", FieldType.FLOAT);
    fieldTypeMap.put("double", FieldType.DOUBLE);
    fieldTypeMap.put("boolean", FieldType.BOOLEAN);
    elasticsearchTypeMap = Collections.unmodifiableMap(fieldTypeMap);
  }

  private transient AdminClient adminClient;
  private List<String> ignoredIndices;

  public ElasticsearchColumnMetadataDao(AdminClient adminClient, List<String> ignoredIndices) {
    this.adminClient = adminClient;
    this.ignoredIndices = ignoredIndices;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, Map<String, FieldType>> allColumnMetadata = new HashMap<>();
    String[] latestIndices = getLatestIndices(indices);
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = adminClient
            .indices()
            .getMappings(new GetMappingsRequest().indices(latestIndices))
            .actionGet()
            .getMappings();
    for(Object key: mappings.keys().toArray()) {
      String indexName = key.toString();

      Map<String, FieldType> indexColumnMetadata = new HashMap<>();
      ImmutableOpenMap<String, MappingMetaData> mapping = mappings.get(indexName);
      Iterator<String> mappingIterator = mapping.keysIt();
      while(mappingIterator.hasNext()) {
        MappingMetaData mappingMetaData = mapping.get(mappingIterator.next());
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) mappingMetaData.getSourceAsMap().get("properties");
        for(String field: map.keySet()) {
          indexColumnMetadata.put(field, toFieldType(map.get(field).get("type")));
        }
      }

      String baseIndexName = ElasticsearchUtils.getBaseIndexName(indexName);
      allColumnMetadata.put(baseIndexName, indexColumnMetadata);
    }
    return allColumnMetadata;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException {
    LOG.debug("Getting common metadata; indices={}", indices);
    Map<String, FieldType> commonColumnMetadata = null;

    // retrieve the mappings for only the latest version of each index
    String[] latestIndices = getLatestIndices(indices);
    ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings;
    mappings = adminClient
            .indices()
            .getMappings(new GetMappingsRequest().indices(latestIndices))
            .actionGet()
            .getMappings();

    // did we get all the mappings that we expect?
    if(mappings.size() < latestIndices.length) {
      String msg = String.format(
              "Failed to get required mappings; expected mappings for '%s', but got '%s'",
              latestIndices, mappings.keys().toArray());
      throw new IllegalStateException(msg);
    }

    // for each index...
    for(Object index: mappings.keys().toArray()) {
      ImmutableOpenMap<String, MappingMetaData> mapping = mappings.get(index.toString());
      Iterator<String> mappingIterator = mapping.keysIt();
      while(mappingIterator.hasNext()) {
        MappingMetaData metadata = mapping.get(mappingIterator.next());
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) metadata.getSourceAsMap().get("properties");
        Map<String, FieldType> mappingsWithTypes = map
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e-> toFieldType(e.getValue().get("type"))));

        // keep only the properties in common
        if (commonColumnMetadata == null) {
          commonColumnMetadata = mappingsWithTypes;
        } else {
          commonColumnMetadata
                  .entrySet()
                  .retainAll(mappingsWithTypes.entrySet());
        }
      }
    }

    return commonColumnMetadata;
  }

  /**
   * Retrieves the latest indices.
   * @param includeIndices
   * @return
   */
  protected String[] getLatestIndices(List<String> includeIndices) {
    LOG.debug("Getting latest indices; indices={}", includeIndices);
    Map<String, String> latestIndices = new HashMap<>();
    String[] indices = adminClient
            .indices()
            .prepareGetIndex()
            .setFeatures()
            .get()
            .getIndices();

    for (String index : indices) {
      if (!ignoredIndices.contains(index)) {
        int prefixEnd = index.indexOf(INDEX_NAME_DELIMITER);
        if (prefixEnd != -1) {
          String prefix = index.substring(0, prefixEnd);
          if (includeIndices.contains(prefix)) {
            String latestIndex = latestIndices.get(prefix);
            if (latestIndex == null || index.compareTo(latestIndex) > 0) {
              latestIndices.put(prefix, index);
            }
          }
        }
      }
    }

    return latestIndices.values().toArray(new String[latestIndices.size()]);
  }

  /**
   * Converts a string type to the corresponding FieldType.
   * @param type The type to convert.
   * @return The corresponding FieldType or FieldType.OTHER, if no match.
   */
  private FieldType toFieldType(String type) {
    return elasticsearchTypeMap.getOrDefault(type, FieldType.OTHER);
  }
}
