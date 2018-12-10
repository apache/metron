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
package org.apache.metron.elasticsearch.client;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.elasticsearch.utils.FieldMapping;
import org.apache.metron.elasticsearch.utils.FieldProperties;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Wrapper around the Elasticsearch REST clients. Exposes capabilities of the low and high-level clients.
 * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/java-rest-overview.html. Most, if not
 * all of use in Metron would be focused through the high-level client. It handles marshaling/unmarshaling.
 */
public class ElasticsearchClient implements AutoCloseable{
  private RestClient lowLevelClient;
  private RestHighLevelClient highLevelClient;

  /**
   * Instantiate with ElasticsearchClientFactory.
   *
   * @param lowLevelClient
   * @param highLevelClient
   */
  public ElasticsearchClient(RestClient lowLevelClient, RestHighLevelClient highLevelClient) {
    this.lowLevelClient = lowLevelClient;
    this.highLevelClient = highLevelClient;
  }

  /**
   * Exposes an Elasticsearch low-level client. Prefer the high level client.
   */
  public RestClient getLowLevelClient() {
    return lowLevelClient;
  }

  /**
   * <p>
   * Exposes an Elasticsearch high-level client. Prefer to use this client over the low-level client where possible. This client wraps the low-level
   * client and exposes some additional sugar on top of the low level methods including marshaling/unmarshaling.
   * </p>
   * <p>
   *   Note, as of 5.6 it does NOT support index or cluster management operations.
   *   https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_changing_the_application_8217_s_code.html
   *   <br>
   * &nbsp;&nbsp;&nbsp;&nbsp;<i>Does not provide indices or cluster management APIs. Management operations can be executed by external scripts or using the low-level client.</i>
   * </p>
   * <p>
   * Current supported ES API's seen here - https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/java-rest-high-supported-apis.html
   * </p>
   *
   * <ul>
   *   <li>Single document APIs
   *     <ul>
   *       <li>Index API</li>
   *       <li>Get API</li>
   *       <li>Delete API</li>
   *       <li>Update API</li>
   *     </ul>
   *   </li>
   *   <li>Multi document APIs
   *     <ul>
   *       <li>Bulk API</li>
   *     </ul>
   *   </li>
   *   <li>Search APIs
   *     <ul>
   *       <li>Search API</li>
   *       <li>Search Scroll API</li>
   *       <li>Clear Scroll API</li>
   *     </ul>
   *   </li>
   *   <li>Miscellaneous APIs
   *     <ul>
   *       <li>Info API</li>
   *     </ul>
   *   </li>
   * </ul>
   */
  public RestHighLevelClient getHighLevelClient() {
    return highLevelClient;
  }

  /**
   * Included as part of AutoCloseable because Elasticsearch recommends closing the client when not
   * being used.
   * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/5.6/_changing_the_client_8217_s_initialization_code.html
   * @throws IOException
   */
  @Override
  public void close() throws IOException {
    if (lowLevelClient != null) {
      lowLevelClient.close();
    }
  }

  /**
   * https://www.elastic.co/guide/en/elasticsearch/reference/5.6/indices-put-mapping.html
   * @param index
   * @param mappingType https://www.elastic.co/guide/en/elasticsearch/reference/5.6/mapping.html#mapping-type
   * @param mapping
   * @throws IOException
   */
  public void putMapping(String index, String mappingType, String mapping) throws IOException {
    HttpEntity entity = new StringEntity(mapping);
    Response response = lowLevelClient.performRequest("PUT"
            , "/" + index + "/_mapping/" + mappingType
            , Collections.emptyMap()
            , entity
    );

    if(response.getStatusLine().getStatusCode() != 200) {
      String responseStr = IOUtils.toString(response.getEntity().getContent());
      throw new IllegalStateException("Got a " + response.getStatusLine().getStatusCode() + " due to " + responseStr);
    }
  }

  /**
   * Gets ALL Elasticsearch indices, or null if status code returned is not OK 200.
   */
  public String[] getIndices() throws IOException {
    Response response = lowLevelClient.performRequest("GET", "/_cat/indices");
    if(response.getStatusLine().getStatusCode() == 200) {
      String responseStr = IOUtils.toString(response.getEntity().getContent());
      List<String> indices = new ArrayList<>();
      for(String line : Splitter.on("\n").split(responseStr)) {
        Iterable<String> splits = Splitter.on(" ").split(line.replaceAll("\\s+", " ").trim());
        if(Iterables.size(splits) > 3) {
          String index = Iterables.get(splits, 2, "");
          if(!StringUtils.isEmpty(index)) {
            indices.add(index.trim());
          }
        }
      }
      String[] ret = new String[indices.size()];
      ret=indices.toArray(ret);
      return ret;
    }
    return null;
  }

  /**
   * Gets FieldMapping detail for a list of indices.
   *
   * @param indices get field mapppings for the provided indices
   * @return mapping of index name to FieldMapping
   */
  public Map<String, FieldMapping> getMappingByIndex(String[] indices) throws IOException {
    Map<String, FieldMapping> ret = new HashMap<>();
    String indicesCsv = Joiner.on(",").join(indices);
    Response response = lowLevelClient.performRequest("GET", "/" + indicesCsv + "/_mapping");
    if(response.getStatusLine().getStatusCode() == 200) {
      String responseStr = IOUtils.toString(response.getEntity().getContent());
      Map<String, Object> indexToMapping = JSONUtils.INSTANCE.load(responseStr, JSONUtils.MAP_SUPPLIER);
      for(Map.Entry<String, Object> index2Mapping : indexToMapping.entrySet()) {
        String index = index2Mapping.getKey();
        Map<String, Object> mappings = getInnerMap((Map<String, Object>)index2Mapping.getValue(), "mappings");
        if(mappings.size() > 0) {
          Map.Entry<String, Object> docMap = Iterables.getFirst(mappings.entrySet(), null);
          if(docMap != null) {
            Map<String, Object> fieldPropertiesMap = getInnerMap((Map<String, Object>)docMap.getValue(), "properties");
            if(fieldPropertiesMap != null) {
              FieldMapping mapping = new FieldMapping();
              for (Map.Entry<String, Object> field2PropsKV : fieldPropertiesMap.entrySet()) {
                if(field2PropsKV.getValue() != null) {
                  FieldProperties props = new FieldProperties((Map<String, Object>) field2PropsKV.getValue());
                  mapping.put(field2PropsKV.getKey(), props);
                }
              }
              ret.put(index, mapping);
            }
          }
        }
      }
    }
    return ret;
  }

  /**
   * Traverses the outer map to retrieve a leaf map by iteratively calling get(key) using the provided keys in order. e.g.
   * for an outer map provided as follows:
   * <pre>
   * {
   *   "foo" : {
   *     "bar" : {
   *       "baz" : {
   *         "hello" : "world"
   *       }
   *     }
   *   }
   * }
   * </pre>
   * calling getInnerMap(outerMap, new String[] { "foo", "bar", "baz" }) would return the following:
   * <pre>
   * {hello=world}
   * </pre>
   * @param outerMap Complex map of nested keys/values
   * @param keys ordered list of keys to iterate over to grab a leaf mapping.
   * @return leaf node, or innermost matching node from outerMap if no leaf exists
   */
  private Map<String, Object> getInnerMap(Map<String, Object> outerMap, String... keys) {
    Map<String, Object> ret = outerMap;
    if(keys.length == 0) {
      return outerMap;
    }
    for(String key : keys) {
      ret = (Map<String, Object>)ret.get(key);
      if(ret == null) {
        return ret;
      }
    }
    return ret;
  }

}
