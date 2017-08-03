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

package org.apache.metron.rest.service.impl;

import java.io.IOException;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.search.InvalidCreateException;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.MetaAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class MetaAlertServiceImpl implements MetaAlertService {
//  private MetaAlertDao metaDao;
  private MetaAlertDao dao;
  private Environment environment;

  @Autowired
  public MetaAlertServiceImpl(IndexDao indexDao, Environment environment) {
//    this.metaDao = metaDao;
    // By construction this is always a meta alert dao
    this.dao = (MetaAlertDao) indexDao;
    this.environment = environment;
  }


  @Override
  public MetaAlertCreateResponse create(MetaAlertCreateRequest createRequest) throws RestException {
    try {
      return dao.createMetaAlert(createRequest);
    } catch (InvalidCreateException | IOException e) {
      throw new RestException(e.getMessage(), e);
    }
  }

  @Override
  public SearchResponse getAllMetaAlertsForAlert(String guid) throws RestException {
    try {
      return dao.getAllMetaAlertsForAlert(guid);
    } catch (InvalidSearchException ise) {
      throw new RestException(ise.getMessage(), ise);
    }
  }
}
