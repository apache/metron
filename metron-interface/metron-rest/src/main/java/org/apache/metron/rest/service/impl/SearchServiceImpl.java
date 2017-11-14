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

import static org.apache.metron.common.Constants.ERROR_TYPE;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_TYPE;
import static org.apache.metron.rest.MetronRestConstants.INDEX_WRITER_NAME;

import com.google.common.collect.Lists;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SearchService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
  private IndexDao dao;
  private Environment environment;
  private SensorIndexingConfigService sensorIndexingConfigService;

  @Autowired
  public SearchServiceImpl(IndexDao dao, Environment environment, SensorIndexingConfigService sensorIndexingConfigService) {
    this.dao = dao;
    this.environment = environment;
    this.sensorIndexingConfigService = sensorIndexingConfigService;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws RestException {
    try {
      // Pull the indices from the cache by default
      if (searchRequest.getIndices() == null || searchRequest.getIndices().isEmpty()) {
        List<String> indices = Lists.newArrayList((sensorIndexingConfigService.getAllIndices(environment.getProperty(INDEX_WRITER_NAME))));
        // metaalerts should be included by default
        indices.add(METAALERT_TYPE);
        // errors should not be included by default
        indices.remove(ERROR_TYPE);
        searchRequest.setIndices(indices);
      }
      return dao.search(searchRequest);
    }
    catch(InvalidSearchException ise) {
      throw new RestException(ise.getMessage(), ise);
    }
  }

  @Override
  public GroupResponse group(GroupRequest groupRequest) throws RestException {
    try {
      return dao.group(groupRequest);
    }
    catch(InvalidSearchException ise) {
      throw new RestException(ise.getMessage(), ise);
    }
  }

  @Override
  public Optional<Map<String, Object>> getLatest(GetRequest request) throws RestException {
    try {
      return dao.getLatestResult(request);
    } catch (IOException e) {
      throw new RestException(e.getMessage(), e);
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
