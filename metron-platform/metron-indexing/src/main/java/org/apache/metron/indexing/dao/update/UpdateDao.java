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
package org.apache.metron.indexing.dao.update;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.RetrieveLatestDao;

public interface UpdateDao {

  /**
   * Update a given Document and optionally the index where the document exists.  This is a full
   * update, meaning the current document will be replaced if it exists or a new document will be
   * created if it does not exist.  Partial updates are not supported in this method.
   *
   * @param update The document to replace from the index.
   * @param index The index where the document lives.
   * @return The updated document
   * @throws IOException If an error occurs during the update.
   */
  Document update(Document update, Optional<String> index) throws IOException;

  /**
   * Similar to the update method but accepts multiple documents and performs updates in batch.
   *
   * @param updates A map of the documents to update to the index where they live.
   * @return The updated documents.
   * @throws IOException If an error occurs during the updates.
   */
  Map<Document, Optional<String>> batchUpdate(Map<Document, Optional<String>> updates) throws IOException;

  Document addCommentToAlert(CommentAddRemoveRequest request) throws IOException;

  Document removeCommentFromAlert(CommentAddRemoveRequest request) throws IOException;

  Document addCommentToAlert(CommentAddRemoveRequest request, Document latest) throws IOException;

  Document removeCommentFromAlert(CommentAddRemoveRequest request, Document latest) throws IOException;


  /**
   * Update a document in an index given a JSON Patch (see RFC 6902 at
   * https://tools.ietf.org/html/rfc6902)
   * @param request The patch request
   * @param timestamp Optionally a timestamp to set. If not specified then current time is used.
   * @return The patched document.
   * @throws OriginalNotFoundException If the original is not found, then it cannot be patched.
   * @throws IOException If an error occurs while patching.
   */
  default Document patch(RetrieveLatestDao retrieveLatestDao, PatchRequest request
      , Optional<Long> timestamp
  ) throws OriginalNotFoundException, IOException {
    Document d = getPatchedDocument(retrieveLatestDao, request, timestamp);
    return update(d, Optional.ofNullable(request.getIndex()));
  }

  default Document getPatchedDocument(RetrieveLatestDao retrieveLatestDao, PatchRequest request,
      Optional<Long> timestamp
  ) throws OriginalNotFoundException, IOException {
    Map<String, Object> latest = request.getSource();
    if (latest == null) {
      Document latestDoc = retrieveLatestDao.getLatest(request.getGuid(), request.getSensorType());
      if (latestDoc != null && latestDoc.getDocument() != null) {
        latest = latestDoc.getDocument();
      } else {
        throw new OriginalNotFoundException(
            "Unable to patch an document that doesn't exist and isn't specified.");
      }
    }

    Map<String, Object> updated = JSONUtils.INSTANCE.applyPatch(request.getPatch(), latest);
    return new Document(updated,
        request.getGuid(),
        request.getSensorType(),
        timestamp.orElse(System.currentTimeMillis()));
  }

  /**
   * Replace a document in an index.
   * @param request The replacement request.
   * @param timestamp The timestamp (optional) of the update.  If not specified, then current time will be used.
   * @return The replaced document.
   * @throws IOException If an error occurs during replacement.
   */
  default Document replace(ReplaceRequest request, Optional<Long> timestamp)
      throws IOException {
    Document d = new Document(request.getReplacement(),
        request.getGuid(),
        request.getSensorType(),
        timestamp.orElse(System.currentTimeMillis())
    );
    return update(d, Optional.ofNullable(request.getIndex()));
  }
}
