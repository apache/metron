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
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchResponse;

public interface MetaAlertDao extends IndexDao {

  String METAALERTS_INDEX = "metaalert_index";
  String METAALERT_TYPE = "metaalert";
  String METAALERT_FIELD = "metaalerts";
  String METAALERT_DOC = METAALERT_TYPE + "_doc";
  String THREAT_FIELD_DEFAULT = "threat:triage:score";
  String THREAT_SORT_DEFAULT = "sum";
  String ALERT_FIELD = "alert";
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
   * Create a meta alert.
   * @param request The parameters for creating the new meta alert
   * @return A response indicating success or failure
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
    init(indexDao, null);
  }

  /**
   * Initializes a Meta Alert DAO.
   * @param indexDao The DAO to wrap for our queries
   * @param threatSort The aggregation to use as the threat field. E.g. "sum", "median", etc.
   *     null is "sum"
   */
  void init(IndexDao indexDao, String threatSort);
}
