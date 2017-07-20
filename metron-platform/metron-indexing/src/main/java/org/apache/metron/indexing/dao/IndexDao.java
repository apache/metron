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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonPatch;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface IndexDao {
  SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException;
  void init(AccessConfig config);
  Document getLatest(String uuid, String sensorType) throws IOException;

  default Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    Document ret = getLatest(request.getUuid(), request.getSensorType());
    if(ret == null) {
      return Optional.empty();
    }
    else {
      return Optional.ofNullable(ret.getDocument());
    }
  }

  void update(Document update) throws IOException;


  default void patch( PatchRequest request
                    , Optional<Long> timestamp
                    ) throws OriginalNotFoundException, IOException {
    Map<String, Object> latest = request.getSource();
    if(latest == null) {
      Document latestDoc = getLatest(request.getUuid(), request.getSensorType());
      if(latestDoc.getDocument() != null) {
        latest = latestDoc.getDocument();
      }
      else {
        throw new OriginalNotFoundException("Unable to patch an document that doesn't exist and isn't specified.");
      }
    }
    JsonNode originalNode = JSONUtils.INSTANCE.convert(latest, JsonNode.class);
    JsonNode patched = JsonPatch.apply(request.getPatch(), originalNode);
    Map<String, Object> updated = JSONUtils.INSTANCE.getMapper()
                                           .convertValue(patched, new TypeReference<Map<String, Object>>() {});
    Document d = new Document(updated
                             , request.getUuid()
                             , request.getSensorType()
                             , timestamp.orElse(System.currentTimeMillis())
                             );
    update(d);
  }

  default void replace( ReplaceRequest request
                      , Optional<Long> timestamp
                      ) throws IOException {
    Document d = new Document(request.getReplacement()
                             , request.getUuid()
                             , request.getSensorType()
                             , timestamp.orElse(System.currentTimeMillis())
                             );
    update(d);
  }

}
