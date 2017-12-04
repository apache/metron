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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  /**
   * An Elasticsearch administrative client.
   */
  private transient AdminClient adminClient;

  /**
   * @param adminClient The Elasticsearch admin client.
   */
  public ElasticsearchColumnMetadataDao(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> indexColumnMetadata = new HashMap<>();
    Map<String, String> previousIndices = new HashMap<>();
    Set<String> fieldBlackList = new HashSet<>();

    String[] latestIndices = getLatestIndices(indices);
    if (latestIndices.length > 0) {
      ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = adminClient
              .indices()
              .getMappings(new GetMappingsRequest().indices(latestIndices))
              .actionGet()
              .getMappings();

      // for each index
      for (Object key : mappings.keys().toArray()) {
        String indexName = key.toString();
        ImmutableOpenMap<String, MappingMetaData> mapping = mappings.get(indexName);

        // for each mapping in the index
        Iterator<String> mappingIterator = mapping.keysIt();
        while (mappingIterator.hasNext()) {
          MappingMetaData mappingMetaData = mapping.get(mappingIterator.next());
          Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) mappingMetaData
                  .getSourceAsMap().get("properties");

          // for each field in the mapping
          for (String field : map.keySet()) {
            if (!fieldBlackList.contains(field)) {
              FieldType type = toFieldType(map.get(field).get("type"));

              if(!indexColumnMetadata.containsKey(field)) {
                indexColumnMetadata.put(field, type);

                // record the last index in which a field exists, to be able to print helpful error message on type mismatch
                previousIndices.put(field, indexName);

              } else {
                FieldType previousType = indexColumnMetadata.get(field);
                if (!type.equals(previousType)) {
                  String previousIndexName = previousIndices.get(field);
                  LOG.error(String.format(
                          "Field type mismatch: %s.%s has type %s while %s.%s has type %s.  Defaulting type to %s.",
                          indexName, field, type.getFieldType(),
                          previousIndexName, field, previousType.getFieldType(),
                          FieldType.OTHER.getFieldType()));
                  indexColumnMetadata.put(field, FieldType.OTHER);

                  // the field is defined in multiple indices with different types; ignore the field as type has been set to OTHER
                  fieldBlackList.add(field);
                }
              }
            }
          }
        }
      }
    } else {
      LOG.info(String.format("Unable to find any latest indices; indices=%s", indices));
    }

    return indexColumnMetadata;

  }

  /**
   * Retrieves the latest indices.
   * @param includeIndices
   * @return
   */
  @Override
  public String[] getLatestIndices(List<String> includeIndices) {
    LOG.debug("Getting latest indices; indices={}", includeIndices);
    Map<String, String> latestIndices = new HashMap<>();
    String[] indices = adminClient
            .indices()
            .prepareGetIndex()
            .setFeatures()
            .get()
            .getIndices();

    for (String index : indices) {
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
