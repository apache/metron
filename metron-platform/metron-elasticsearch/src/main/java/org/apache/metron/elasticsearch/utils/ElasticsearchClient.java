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
package org.apache.metron.elasticsearch.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.metron.common.utils.JSONUtils;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchClient implements AutoCloseable{
  private RestClient lowLevelClient;
  private RestHighLevelClient highLevelClient;

  public ElasticsearchClient(RestClient lowLevelClient, RestHighLevelClient highLevelClient) {
    this.lowLevelClient = lowLevelClient;
    this.highLevelClient = highLevelClient;
  }

  public RestClient getLowLevelClient() {
    return lowLevelClient;
  }

  public RestHighLevelClient getHighLevelClient() {
    return highLevelClient;
  }

  @Override
  public void close() throws IOException {
    if(lowLevelClient != null) {
      lowLevelClient.close();
    }
  }

  public void putMapping(String index, String type, String source) throws IOException {
    HttpEntity entity = new StringEntity(source);
    Response response = lowLevelClient.performRequest("PUT"
            , "/" + index + "/_mapping/" + type
            , Collections.emptyMap()
            , entity
    );

    if(response.getStatusLine().getStatusCode() != 200) {
      String responseStr = IOUtils.toString(response.getEntity().getContent());
      throw new IllegalStateException("Got a " + response.getStatusLine().getStatusCode() + " due to " + responseStr);
    }
    /**
     * ((ElasticsearchDao) esDao).getClient().admin().indices().preparePutMapping(INDEX)
            .setType("test_doc")
            .setSource(nestedAlertMapping)
            .get();
     */
  }

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

  public Map<String, FieldMapping> getMappings(String[] indices) throws IOException {
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

}
