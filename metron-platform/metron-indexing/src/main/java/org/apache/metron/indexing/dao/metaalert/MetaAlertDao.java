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

import java.util.Optional;
import org.apache.metron.indexing.dao.IndexDao;

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
public interface MetaAlertDao extends MetaAlertSearchDao, MetaAlertUpdateDao, IndexDao {

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
