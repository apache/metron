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
import static org.apache.metron.common.Constants.SENSOR_TYPE_FIELD_PROPERTY;
import static org.apache.metron.indexing.dao.MetaAlertDao.METAALERT_TYPE;
import static org.apache.metron.rest.MetronRestConstants.INDEX_WRITER_NAME;
import static org.apache.metron.rest.MetronRestConstants.SEARCH_FACET_FIELDS_SPRING_PROPERTY;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.rest.service.AlertsUIService;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SearchService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private IndexDao dao;
  private Environment environment;
  private SensorIndexingConfigService sensorIndexingConfigService;
  private GlobalConfigService globalConfigService;
  private AlertsUIService alertsUIService;

  @Autowired
  public SearchServiceImpl(IndexDao dao,
      Environment environment,
      SensorIndexingConfigService sensorIndexingConfigService,
      GlobalConfigService globalConfigService,
      AlertsUIService alertsUIService) {
    this.dao = dao;
    this.environment = environment;
    this.sensorIndexingConfigService = sensorIndexingConfigService;
    this.globalConfigService = globalConfigService;
    this.alertsUIService = alertsUIService;
  }

  @Override
  public SearchResponse search(SearchRequest searchRequest) throws RestException {
    try {
      if (searchRequest.getIndices() == null || searchRequest.getIndices().isEmpty()) {
        List<String> indices = getDefaultIndices();
        // metaalerts should be included by default in search requests
        indices.add(METAALERT_TYPE);
        searchRequest.setIndices(indices);
      }
      if (searchRequest.getFacetFields() != null && searchRequest.getFacetFields().isEmpty()) {
        searchRequest.setFacetFields(getDefaultFacetFields());
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
      if (groupRequest.getIndices() == null || groupRequest.getIndices().isEmpty()) {
        groupRequest.setIndices(getDefaultIndices());
      }
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
  public Map<String, FieldType> getColumnMetadata(List<String> indices) throws RestException {
    try {
      if (indices == null || indices.isEmpty()) {
        indices = getDefaultIndices();
        // metaalerts should be included by default in column metadata requests
        indices.add(METAALERT_TYPE);
        LOG.debug(String.format("No indices provided for getColumnMetadata.  Using default indices: %s", String.join(",", indices)));
      }
      return dao.getColumnMetadata(indices);
    }
    catch(IOException ioe) {
      throw new RestException(ioe.getMessage(), ioe);
    }
  }

  private List<String> getDefaultIndices() throws RestException {
    // Pull the indices from the cache by default
    List<String> indices = Lists.newArrayList((sensorIndexingConfigService.getAllIndices(environment.getProperty(INDEX_WRITER_NAME))));
    // errors should not be included by default
    indices.remove(ERROR_TYPE);
    return indices;
  }

  @SuppressWarnings("unchecked")
  public List<String> getDefaultFacetFields() throws RestException {
    Optional<AlertsUIUserSettings> alertUserSettings = alertsUIService.getAlertsUIUserSettings();
    if (!alertUserSettings.isPresent() || alertUserSettings.get().getFacetFields() == null) {
      String facetFieldsProperty = environment
          .getProperty(SEARCH_FACET_FIELDS_SPRING_PROPERTY, String.class, "");
      String sourceTypeField = ConfigurationsUtils.getFieldName(globalConfigService.get(), SENSOR_TYPE_FIELD_PROPERTY,
              Constants.SENSOR_TYPE.replace('.', ':'));
      List<String> facetFields = new ArrayList<>();
      facetFields.add(sourceTypeField);
      if (facetFieldsProperty != null) {
        facetFields.addAll(Arrays.asList(facetFieldsProperty.split(",")));
      }
      return facetFields;
    } else {
      return alertUserSettings.get().getFacetFields();
    }
  }
}
