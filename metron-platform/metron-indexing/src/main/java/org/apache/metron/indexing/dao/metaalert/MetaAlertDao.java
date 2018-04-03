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

import static org.apache.metron.common.Constants.GUID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertConstants;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertSearchDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertUpdateDao;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.update.Document;

/**
 * The MetaAlertDao exposes methods for interacting with meta alerts.  Meta alerts are objects that contain
 * alerts and summary statistics based on the scores of these alerts.  Meta alerts are returned in searches
 * just as alerts are and match based on the field values of child alerts.  If a child alert matches a search
 * the meta alert will be returned while the original child alert will not.  A meta alert also contains a
 * status field that controls it's inclusion in search results and a groups field that can be used to track
 * the groups a meta alert was created from.
 *
 * </p>
 * The structure of a meta alert is as follows:
 * {
 *   "guid": "meta alert guid",
 *   "timestamp": timestamp,
 *   "source:type": "metaalert",
 *   "alerts": [ array of child alerts ],
 *   "status": "active or inactive",
 *   "groups": [ array of group names ],
 *   "average": 10,
 *   "max": 10,
 *   "threat:triage:score": 30,
 *   "count": 3,
 *   "sum": 30,
 *   "min": 10,
 *   "median": 10
 * }
 *
 * </p>
 * A child alert that has been added to a meta alert will store the meta alert GUID in a "metaalerts" field.
 * This field is an array of meta alert GUIDs, meaning a child alert can be contained in multiple meta alerts.
 * Any update to a child alert will trigger an update to the meta alert so that the alert inside a meta alert
 * and the original alert will be kept in sync.
 *
 * </p>
 * Other fields can be added to a meta alert through the patch method on the IndexDao interface.  However, attempts
 * to directly change the "alerts" or "status" field will result in an exception.
 */
public interface MetaAlertDao extends IndexDao, MetaAlertUpdateDao, MetaAlertSearchDao {


  /**
   * Given an alert GUID, retrieve all associated meta alerts.
   * @param guid The alert GUID to be searched for
   * @return All meta alerts with a child alert having the GUID
   * @throws InvalidSearchException If a problem occurs with the search
   */
  SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException;

  /**
   * Creates a meta alert from a list of child alerts.  The most recent version of each child alert is
   * retrieved using the DAO abstractions.
   *
   * @param request A request object containing get requests for alerts to be added and a list of groups
   * @return A response indicating success or failure along with the GUID of the new meta alert
   * @throws InvalidCreateException If a malformed create request is provided
   * @throws IOException If a problem occurs during communication
   */
  MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException;

  /**
   * Initializes a Meta Alert DAO with default "sum" meta alert threat sorting.
   * @param indexDao The DAO to wrap for our queries.
   */
  default void init(IndexDao indexDao) {
    init(indexDao, Optional.empty());
  }

  /**
   * Initializes a Meta Alert DAO.
   * @param indexDao The DAO to wrap for our queries
   * @param threatSort The aggregation to use as the threat field. E.g. "sum", "median", etc.
   *     null is "sum"
   */
  void init(IndexDao indexDao, Optional<String> threatSort);

  /**
   * Build the Document representing a meta alert to be created.
   * @param alerts The Elasticsearch results for the meta alerts child documents
   * @param groups The groups used to create this meta alert
   * @return A Document representing the new meta alert
   */
  default Document buildCreateDocument(Iterable<Document> alerts, List<String> groups,
      String alertField) {
    // Need to create a Document from the multiget. Scores will be calculated later
    Map<String, Object> metaSource = new HashMap<>();
    List<Map<String, Object>> alertList = new ArrayList<>();
    for (Document alert : alerts) {
      alertList.add(alert.getDocument());
    }
    metaSource.put(alertField, alertList);

    // Add any meta fields
    String guid = UUID.randomUUID().toString();
    metaSource.put(GUID, guid);
    metaSource.put(Constants.Fields.TIMESTAMP.getName(), System.currentTimeMillis());
    metaSource.put(MetaAlertConstants.GROUPS_FIELD, groups);
    metaSource.put(MetaAlertConstants.STATUS_FIELD, MetaAlertStatus.ACTIVE.getStatusString());

    return new Document(metaSource, guid, MetaAlertConstants.METAALERT_TYPE,
        System.currentTimeMillis());
  }

  int getPageSize();

  void setPageSize(int pageSize);
}
