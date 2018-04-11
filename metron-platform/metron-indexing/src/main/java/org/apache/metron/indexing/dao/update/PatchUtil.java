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

package org.apache.metron.indexing.dao.update;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.RetrieveLatestDao;

public class PatchUtil {

  public static Document getPatchedDocument(
      RetrieveLatestDao retrieveLatestDao,
      PatchRequest request
      , Optional<Long> timestamp
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
    return new Document(updated
        , request.getGuid()
        , request.getSensorType()
        , timestamp.orElse(System.currentTimeMillis()));
  }
}
