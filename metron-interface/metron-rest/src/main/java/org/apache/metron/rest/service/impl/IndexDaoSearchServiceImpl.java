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
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IndexDaoSearchServiceImpl implements SearchService {
  private IndexDao dao;
  private Environment environment;

  @Autowired
  public IndexDaoSearchServiceImpl(IndexDao dao, Environment environment) {
    this.dao = dao;
    this.environment = environment;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws RestException {
    try {
      return dao.search(searchRequest);
    }
    catch(InvalidSearchException ise) {
      throw new RestException(ise.getMessage(), ise);
    }
  }

  @Override
  public Map<String, Map<String, FieldType>> getColumnMetadata(List<String> indices) throws RestException {
    try {
      return dao.getColumnMetadata(indices);
    }
    catch(IOException ioe) {
      throw new RestException(ioe.getMessage(), ioe);
    }
  }

  @Override
  public Map<String, FieldType> getCommonColumnMetadata(List<String> indices) throws RestException {
    try {
      return dao.getCommonColumnMetadata(indices);
    }
    catch(IOException ioe) {
      throw new RestException(ioe.getMessage(), ioe);
    }
  }
}
