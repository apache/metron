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
package org.apache.metron.rest.service.impl;

import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.indexing.dao.update.ReplaceRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

@Service
public class UpdateServiceImpl implements UpdateService {
  private IndexDao dao;
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Autowired
  public UpdateServiceImpl(IndexDao dao) {
    this.dao = dao;
  }


  @Override
  public void patch(PatchRequest request) throws RestException, OriginalNotFoundException {
    try {
      dao.patch(dao, request, Optional.of(System.currentTimeMillis()));
    } catch (Exception e) {

      throw new RestException(e.getMessage(), e);
    }
  }

  @Override
  public void replace(ReplaceRequest request) throws RestException {
    try {
      dao.replace(request, Optional.of(System.currentTimeMillis()));
    } catch (Exception e) {
      throw new RestException(e.getMessage(), e);
    }
  }
}
