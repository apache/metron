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

package org.apache.metron.rest.service;

import java.io.IOException;
import java.util.Collection;
import org.apache.metron.indexing.dao.metaalert.MetaAlertAddRemoveRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.RestException;

public interface MetaAlertService {

  MetaAlertCreateResponse create(MetaAlertCreateRequest createRequest) throws RestException;

  SearchResponse getAllMetaAlertsForAlert(String guid) throws RestException;

  boolean addAlertsToMetaAlert(MetaAlertAddRemoveRequest metaAlertAddRemoveRequest) throws RestException;

  boolean removeAlertsFromMetaAlert(MetaAlertAddRemoveRequest metaAlertAddRemoveRequest) throws RestException;

  boolean updateMetaAlertStatus(String metaAlertGuid, MetaAlertStatus status) throws RestException;
}
