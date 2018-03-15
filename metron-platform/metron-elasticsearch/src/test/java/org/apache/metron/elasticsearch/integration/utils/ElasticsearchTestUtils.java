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
package org.apache.metron.elasticsearch.integration.utils;

import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.test.utils.DockerUtils;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchTestUtils {

  public static Map<String, Object> getGlobalConfig() {
    return new HashMap<String, Object>() {{
      put("es.clustername", "elasticsearch");
      put("es.port", "9310");
      put("es.ip", DockerUtils.getDockerIpAddress());
      put("es.date.format", "yyyy.MM.dd.HH");
    }};
  }

  public static void clearIndices(Client client, String... indices) {
    for (String index: indices) {
      try {
        client.admin().indices().prepareDelete(index).get();
      } catch (IndexNotFoundException infe) {
      }
    }
  }

  public static List<Map<String, Object>> getAllIndexedDocs(Client client, String index, String sourceType) throws IOException {
    client.admin().indices().refresh(new RefreshRequest());
    SearchResponse response = client.prepareSearch(index)
            .setTypes(sourceType)
            .setFrom(0)
            .setSize(1000)
            .execute().actionGet();
    List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
    for (SearchHit hit : response.getHits()) {
      Object o = hit.getSource();
      ret.add((Map<String, Object>) (o));
    }
    return ret;
  }

  public static BulkResponse add(Client client, String indexName, String docType, Iterable<String> docs)
          throws IOException {
    BulkRequestBuilder bulkRequest = client.prepareBulk();
    for (String doc : docs) {
      IndexRequestBuilder indexRequestBuilder = client.prepareIndex(indexName, docType);

      indexRequestBuilder = indexRequestBuilder.setSource(doc);
      Map<String, Object> esDoc = JSONUtils.INSTANCE
              .load(doc, JSONUtils.MAP_SUPPLIER);
      indexRequestBuilder.setId((String) esDoc.get(Constants.GUID));
      Object ts = esDoc.get("timestamp");
      if (ts != null) {
        indexRequestBuilder = indexRequestBuilder.setTimestamp(ts.toString());
      }
      bulkRequest.add(indexRequestBuilder);
    }

    BulkResponse response = bulkRequest.execute().actionGet();
    if (response.hasFailures()) {
      throw new IOException(response.buildFailureMessage());
    }
    return response;
  }
}
