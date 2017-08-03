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
  String METAALERTS_INDEX = "metaalerts";
  String METAALERT_TYPE = "metaalert";
  String METAALERT_DOC = METAALERT_TYPE + "_doc";
  String THREAT_FIELD_DEFAULT = "threat:triage:level";
  String ALERT_FIELD = "alert";
  String STATUS_FIELD = "status";
  String GROUPS_FIELD = "groups";

  SearchResponse getAllMetaAlertsForAlert(String guid) throws InvalidSearchException;

  MetaAlertCreateResponse createMetaAlert(MetaAlertCreateRequest request)
      throws InvalidCreateException, IOException;

  void init(IndexDao indexDao);
}
