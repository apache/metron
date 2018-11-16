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

import static org.apache.metron.elasticsearch.utils.ElasticsearchUtils.INDEX_NAME_DELIMITER;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.metron.elasticsearch.client.ElasticsearchClient;
import org.apache.metron.elasticsearch.utils.FieldMapping;
import org.apache.metron.elasticsearch.utils.FieldProperties;
import org.apache.metron.indexing.dao.ColumnMetadataDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for retrieving column-level metadata for Elasticsearch search indices.
 */
public class ElasticsearchColumnMetadataDao implements ColumnMetadataDao {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Map<String, FieldType> elasticsearchTypeMap;
  static {
    Map<String, FieldType> fieldTypeMap = new HashMap<>();
    fieldTypeMap.put("text", FieldType.TEXT);
    fieldTypeMap.put("keyword", FieldType.KEYWORD);
    fieldTypeMap.put("ip", FieldType.IP);
    fieldTypeMap.put("integer", FieldType.INTEGER);
    fieldTypeMap.put("long", FieldType.LONG);
    fieldTypeMap.put("date", FieldType.DATE);
    fieldTypeMap.put("float", FieldType.FLOAT);
    fieldTypeMap.put("double", FieldType.DOUBLE);
    fieldTypeMap.put("boolean", FieldType.BOOLEAN);
    elasticsearchTypeMap = Collections.unmodifiableMap(fieldTypeMap);
  }

  private transient ElasticsearchClient esClient;

  /**
   * @param esClient The Elasticsearch client.
   */
  public ElasticsearchColumnMetadataDao(ElasticsearchClient esClient) {
    this.esClient = esClient;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws IOException {
    Map<String, FieldType> indexColumnMetadata = new HashMap<>();
    Map<String, String> previousIndices = new HashMap<>();
    Set<String> fieldBlackList = new HashSet<>();

    String[] latestIndices = getLatestIndices(indices);
    if (latestIndices.length > 0) {

     Map<String, FieldMapping>  mappings = esClient.getMappingByIndex(latestIndices);

      // for each index
      for (Map.Entry<String, FieldMapping> kv : mappings.entrySet()) {
        String indexName = kv.getKey();
        FieldMapping mapping = kv.getValue();

        // for each mapping in the index
        for(Map.Entry<String, FieldProperties> fieldToProperties : mapping.entrySet()) {
          String field = fieldToProperties.getKey();
          FieldProperties properties = fieldToProperties.getValue();
          if (!fieldBlackList.contains(field)) {
            FieldType type = toFieldType((String) properties.get("type"));

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
    } else {
      LOG.info(String.format("Unable to find any latest indices; indices=%s", indices));
    }

    return indexColumnMetadata;

  }

  /**
   * Finds the latest version of a set of base indices.  This can be used to find
   * the latest 'bro' index, for example.
   *
   * Assuming the following indices exist...
   *
   *    [
   *      'bro_index_2017.10.03.19'
   *      'bro_index_2017.10.03.20',
   *      'bro_index_2017.10.03.21',
   *      'snort_index_2017.10.03.19',
   *      'snort_index_2017.10.03.20',
   *      'snort_index_2017.10.03.21'
   *    ]
   *
   *  And the include indices are given as...
   *
   *    ['bro', 'snort']
   *
   * Then the latest indices are...
   *
   *    ['bro_index_2017.10.03.21', 'snort_index_2017.10.03.21']
   *
   * @param includeIndices The base names of the indices to include
   * @return The latest version of a set of indices.
   */
  String[] getLatestIndices(List<String> includeIndices) throws IOException {
    LOG.debug("Getting latest indices; indices={}", includeIndices);
    Map<String, String> latestIndices = new HashMap<>();

    String[] indices = esClient.getIndices();

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
