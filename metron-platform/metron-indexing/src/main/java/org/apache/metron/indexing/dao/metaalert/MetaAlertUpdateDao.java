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

package org.apache.metron.indexing.dao.metaalert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.UpdateDao;

public interface MetaAlertUpdateDao extends UpdateDao {

  String STATUS_PATH = "/" + MetaAlertConstants.STATUS_FIELD;
  String ALERT_PATH = "/" + MetaAlertConstants.ALERT_FIELD;

  /**
   * Determines if a given patch request is allowed or not. By default patching the 'alert' or
   * 'status' fields are not allowed, because they should be updated via the specific methods.
   * @param request The patch request to examine
   * @return True if patch can be performed, false otherwise
   */
  default boolean isPatchAllowed(PatchRequest request) {
    if (request.getPatch() != null && !request.getPatch().isEmpty()) {
      for (Map<String, Object> patch : request.getPatch()) {
        Object pathObj = patch.get("path");
        if (pathObj != null && pathObj instanceof String) {
          String path = (String) pathObj;
          if (STATUS_PATH.equals(path) || ALERT_PATH.equals(path)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Creates a meta alert from a list of child alerts.  The most recent version of each child alert is
   * retrieved using the DAO abstractions.
   *
   * @param request A request object containing get requests for alerts to be added and a list of groups
   * @return The complete document of the created metaalert.
   * @throws InvalidCreateException If a malformed create request is provided
   * @throws IOException If a problem occurs during communication
   */
  Document createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException;

  /**
   * Adds alerts to a metaalert, based on a list of GetRequests provided for retrieval.
   * @param metaAlertGuid The GUID of the metaalert to be given new children.
   * @param alertRequests GetRequests for the appropriate alerts to add.
   * @return The complete metaalert document with the alerts added.
   */
  Document addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException, IllegalStateException;

  /**
   * Removes alerts from a metaalert
   * @param metaAlertGuid The metaalert guid to be affected.
   * @param alertRequests A list of GetReqests that will provide the alerts to remove
   * @return The complete metaalert document with the alerts removed.
   * @throws IOException If an error is thrown during retrieal.
   */
  Document removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> alertRequests)
      throws IOException, IllegalStateException;

  /**
   * Removes a metaalert link from a given alert. An nonexistent link performs no change.
   * @param metaAlertGuid The metaalert GUID to link.
   * @param alert The alert to be linked to.
   * @return True if the alert changed, false otherwise.
   */
  default boolean removeMetaAlertFromAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertConstants.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }
    boolean metaAlertRemoved = metaAlertField.remove(metaAlertGuid);
    if (metaAlertRemoved) {
      alert.getDocument().put(MetaAlertConstants.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertRemoved;
  }

  /**
   * The meta alert status field can be set to either 'active' or 'inactive' and will control whether or not meta alerts
   * (and child alerts) appear in search results.  An 'active' status will cause meta alerts to appear in search
   * results instead of it's child alerts and an 'inactive' status will suppress the meta alert from search results
   * with child alerts appearing in search results as normal.  A change to 'inactive' will cause the meta alert GUID to
   * be removed from all it's child alert's "metaalerts" field.  A change back to 'active' will have the opposite effect.
   *
   * @param metaAlertGuid The GUID of the meta alert
   * @param status A status value of 'active' or 'inactive'
   * @return The complete metaalert document with the updated status.
   * @throws IOException if an error occurs during the update.
   */
  Document updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status)
      throws IOException;

  /**
   * Adds a metaalert link to a provided alert Document.  Adding an existing link does no change.
   * @param metaAlertGuid The GUID to be added.
   * @param alert The alert we're adding the link to.
   * @return True if the alert is modified, false if not.
   */
  default boolean addMetaAlertToAlert(String metaAlertGuid, Document alert) {
    List<String> metaAlertField = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<String> alertField = (List<String>) alert.getDocument()
        .get(MetaAlertConstants.METAALERT_FIELD);
    if (alertField != null) {
      metaAlertField.addAll(alertField);
    }

    boolean metaAlertAdded = !metaAlertField.contains(metaAlertGuid);
    if (metaAlertAdded) {
      metaAlertField.add(metaAlertGuid);
      alert.getDocument().put(MetaAlertConstants.METAALERT_FIELD, metaAlertField);
    }
    return metaAlertAdded;
  }
}
