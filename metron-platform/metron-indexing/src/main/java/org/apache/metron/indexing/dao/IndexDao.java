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
import org.apache.metron.indexing.dao.search.FieldType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IndexDao {

  /**
   * Return search response based on the search request
   *
   * @param searchRequest
   * @return
   * @throws InvalidSearchException
   */
  SearchResponse search(SearchRequest searchRequest) throws InvalidSearchException;

  /**
   * Initialize the DAO with the AccessConfig object.
   * @param config
   */
  void init(AccessConfig config);

  /**
   * Return the latest version of a document given the GUID and the sensor type.
   *
   * @param guid The GUID for the document
   * @param sensorType The sensor type of the document
   * @return The Document matching or null if not available.
   * @throws IOException
   */
  Document getLatest(String guid, String sensorType) throws IOException;

  /**
   * Return the latest version of a document given a GetRequest.
   * @param request The GetRequest which indicates the GUID and sensor type.
   * @return Optionally the document (dependent upon existence in the index).
   * @throws IOException
   */
  default Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    Document ret = getLatest(request.getGuid(), request.getSensorType());
    if(ret == null) {
      return Optional.empty();
    }
    else {
      return Optional.ofNullable(ret.getDocument());
    }
  }

  /**
   * Update given a Document and optionally the index where the document exists.
   *
   * @param update The document to replace from the index.
   * @param index The index where the document lives.
   * @throws IOException
   */
  void update(Document update, Optional<String> index) throws IOException;


  /**
   * Update a document in an index given a JSON Patch (see RFC 6902 at https://tools.ietf.org/html/rfc6902)
   * @param request The patch request
   * @param timestamp Optionally a timestamp to set. If not specified then current time is used.
   * @throws OriginalNotFoundException If the original is not found, then it cannot be patched.
   * @throws IOException
   */
  default void patch( PatchRequest request
                    , Optional<Long> timestamp
                    ) throws OriginalNotFoundException, IOException {
    Map<String, Object> latest = request.getSource();
    if(latest == null) {
      Document latestDoc = getLatest(request.getGuid(), request.getSensorType());
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
    Document d = new Document( updated
                             , request.getGuid()
                             , request.getSensorType()
                             , timestamp.orElse(System.currentTimeMillis())
                             );
    update(d, Optional.ofNullable(request.getIndex()));
  }

  /**
   * Replace a document in an index.
   * @param request The replacement request.
   * @param timestamp The timestamp (optional) of the update.  If not specified, then current time will be used.
   * @throws IOException
   */
  default void replace( ReplaceRequest request
                      , Optional<Long> timestamp
                      ) throws IOException {
    Document d = new Document(request.getReplacement()
                             , request.getGuid()
                             , request.getSensorType()
                             , timestamp.orElse(System.currentTimeMillis())
                             );
    update(d, Optional.ofNullable(request.getIndex()));
  }

  Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws IOException;
  Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws IOException;
}
