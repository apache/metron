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

import java.util.List;
import java.util.Optional;
import java.io.IOException;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchResponse;

/**
 * The MetaAlertDao exposes methods for interacting with meta alerts.  Meta alerts are objects that contain
 * alerts and summary statistics based on the scores of these alerts.  Meta alerts are returned in searches
 * just as alerts are and match based on the field values of child alerts.  If a child alert matches a search
 * the meta alert will be returned while the original child alert will not.  A meta alert also contains a
 * status field that controls it's inclusion in search results and a groups field that can be used to track
 * the groups a meta alert was created from.
 *
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
 * A child alert that has been added to a meta alert will store the meta alert GUID in a "metaalerts" field.
 * This field is an array of meta alert GUIDs, meaning a child alert can be contained in multiple meta alerts.
 * Any update to a child alert will trigger an update to the meta alert so that the alert inside a meta alert
 * and the original alert will be kept in sync.
 *
 * Other fields can be added to a meta alert through the patch method on the IndexDao interface.  However, attempts
 * to directly change the "alerts" or "status" field will result in an exception.
 */
public interface MetaAlertDao extends IndexDao {

  String METAALERTS_INDEX = "metaalert_index";
  String METAALERT_TYPE = "metaalert";
  String METAALERT_FIELD = "metaalerts";
  String METAALERT_DOC = METAALERT_TYPE + "_doc";
  String THREAT_FIELD_DEFAULT = "threat:triage:score";
  String THREAT_SORT_DEFAULT = "sum";
  String ALERT_FIELD = "metron_alert";
  String STATUS_FIELD = "status";
  String GROUPS_FIELD = "groups";

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
   * Adds a list of alerts to an existing meta alert.  This will add each alert object to the "alerts" array in the meta alert
   * and also add the meta alert GUID to each child alert's "metaalerts" array.  After alerts have been added the
   * meta alert scores are recalculated.  Any alerts already in the meta alert are skipped and no updates are
   * performed if all of the alerts are already in the meta alert.  The most recent version of each child alert is
   * retrieved using the DAO abstractions.  Alerts cannot be added to an 'inactive' meta alert.
   *
   * @param metaAlertGuid The meta alert GUID
   * @param getRequests Get requests for alerts to be added
   * @return True or false depending on if any alerts were added
   * @throws IOException
   */
  boolean addAlertsToMetaAlert(String metaAlertGuid, List<GetRequest> getRequests) throws IOException;

  /**
   * Removes a list of alerts from an existing meta alert.  This will remove each alert object from the "alerts" array in the meta alert
   * and also remove the meta alert GUID from each child alert's "metaalerts" array.  After alerts have been removed the
   * meta alert scores are recalculated.  Any alerts not contained in the meta alert are skipped and no updates are
   * performed if no alerts can be found in the meta alert.  Alerts cannot be removed from an 'inactive' meta alert.
   *
   * @param metaAlertGuid The meta alert GUID
   * @param getRequests Get requests for alerts to be removed
   * @return True or false depending on if any alerts were removed
   * @throws IOException
   */
  boolean removeAlertsFromMetaAlert(String metaAlertGuid, List<GetRequest> getRequests) throws IOException;

  /**
   * The meta alert status field can be set to either 'active' or 'inactive' and will control whether or not meta alerts
   * (and child alerts) appear in search results.  An 'active' status will cause meta alerts to appear in search
   * results instead of it's child alerts and an 'inactive' status will suppress the meta alert from search results
   * with child alerts appearing in search results as normal.  A change to 'inactive' will cause the meta alert GUID to
   * be removed from all it's child alert's "metaalerts" field.  A change back to 'active' will have the opposite effect.
   *
   * @param metaAlertGuid The GUID of the meta alert
   * @param status A status value of 'active' or 'inactive'
   * @return True or false depending on if the status was changed
   * @throws IOException
   */
  boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status) throws IOException;

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
}
