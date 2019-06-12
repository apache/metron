/*
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.update.Document;

/**
 * An base interface for other DAOs to extend.  All DAOs are expected to be able to retrieve
 * Documents they've stored.
 */
public interface RetrieveLatestDao {

  /**
   * Return the latest version of a document given the GUID and the sensor type.
   *
   * @param guid The GUID for the document
   * @param sensorType The sensor type of the document
   * @return The Document matching or null if not available.
   * @throws IOException If an error occurs retrieving the latest document.
   */
  Document getLatest(String guid, String sensorType) throws IOException;

  /**
   * Return a list of the latest versions of documents given a list of GUIDs and sensor types.
   *
   * @param getRequests A list of get requests for documents
   * @return A list of documents matching or an empty list in not available.
   * @throws IOException If an error occurs retrieving the latest documents.
   */
  Iterable<Document> getAllLatest(List<GetRequest> getRequests) throws IOException;

  /**
   * Return the latest version of a document given a GetRequest.
   * @param request The GetRequest which indicates the GUID and sensor type.
   * @return Optionally the document (dependent upon existence in the index).
   * @throws IOException If an error occurs while retrieving the document.
   */
  default Optional<Map<String, Object>> getLatestResult(GetRequest request) throws IOException {
    Document ret = getLatest(request.getGuid(), request.getSensorType());
    if (ret == null) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(ret.getDocument());
    }
  }
}
