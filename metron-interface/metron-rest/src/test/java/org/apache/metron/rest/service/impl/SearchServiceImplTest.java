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

import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.rest.service.AlertsUIService;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.*;

import static org.apache.metron.common.Constants.SENSOR_TYPE_FIELD_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.INDEX_WRITER_NAME;
import static org.apache.metron.rest.MetronRestConstants.SEARCH_FACET_FIELDS_SPRING_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("ALL")
public class SearchServiceImplTest {
  IndexDao dao;
  Environment environment;
  SensorIndexingConfigService sensorIndexingConfigService;
  GlobalConfigService globalConfigService;
  AlertsUIService alertsUIService;
  SearchServiceImpl searchService;

  @BeforeEach
  public void setUp() throws Exception {
    dao = mock(IndexDao.class);
    environment = mock(Environment.class);
    sensorIndexingConfigService = mock(SensorIndexingConfigService.class);
    globalConfigService = mock(GlobalConfigService.class);
    alertsUIService = mock(AlertsUIService.class);
    searchService = new SearchServiceImpl(
        dao,
        environment,
        sensorIndexingConfigService,
        globalConfigService,
        alertsUIService
    );
  }


  @Test
  public void searchShouldProperlySearchDefaultIndices() throws Exception {
    when(environment.getProperty(INDEX_WRITER_NAME)).thenReturn("elasticsearch");
    when(sensorIndexingConfigService.getAllIndices("elasticsearch")).thenReturn(Arrays.asList("bro", "snort", "error"));

    SearchRequest searchRequest = new SearchRequest();
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    verify(dao).search(eq(expectedSearchRequest));

    verifyNoMoreInteractions(dao);
  }

  @Test
  public void searchShouldProperlySearchWithEmptyDefaultFacetFields() throws Exception {
    when(environment.getProperty(SEARCH_FACET_FIELDS_SPRING_PROPERTY, String.class, ""))
        .thenReturn("");

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    verify(dao).search(eq(expectedSearchRequest));
  }

  @Test
  public void searchShouldProperlySearchDefaultFacetFields() throws Exception {
    when(environment.getProperty(SEARCH_FACET_FIELDS_SPRING_PROPERTY, String.class, ""))
        .thenReturn("ip_src_addr,ip_dst_addr");
    when(alertsUIService.getAlertsUIUserSettings()).thenReturn(Optional.empty());

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    searchRequest.setFacetFields(new ArrayList<>());
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    expectedSearchRequest.setFacetFields(Arrays.asList("source:type", "ip_src_addr", "ip_dst_addr"));
    verify(dao).search(eq(expectedSearchRequest));
  }

  @Test
  public void searchShouldProperlySearchWithUserSettingsFacetFields() throws Exception {
    AlertsUIUserSettings alertsUIUserSettings = new AlertsUIUserSettings();
    alertsUIUserSettings.setFacetFields(Arrays.asList("ip_src_addr", "ip_dst_addr"));
    when(alertsUIService.getAlertsUIUserSettings()).thenReturn(Optional.of(alertsUIUserSettings));

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    searchRequest.setFacetFields(new ArrayList<>());
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro", "snort", "metaalert"));
    expectedSearchRequest.setFacetFields(Arrays.asList("ip_src_addr", "ip_dst_addr"));
    verify(dao).search(eq(expectedSearchRequest));
  }

  @Test
  public void searchShouldProperlySearch() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro"));
    searchRequest.setFacetFields(Arrays.asList("ip_src_addr"));
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro"));
    expectedSearchRequest.setFacetFields(Arrays.asList("ip_src_addr"));
    verify(dao).search(eq(expectedSearchRequest));

    verifyNoMoreInteractions(dao);
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    when(dao.search(any(SearchRequest.class))).thenThrow(InvalidSearchException.class);

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro"));
    searchRequest.setFacetFields(Arrays.asList("ip_src_addr"));
    assertThrows(RestException.class, () -> searchService.search(searchRequest));
  }

  @Test
  public void getColumnMetadataShouldProperlyGetDefaultIndices() throws Exception {
    when(environment.getProperty(INDEX_WRITER_NAME)).thenReturn("elasticsearch");
    when(sensorIndexingConfigService.getAllIndices("elasticsearch")).thenReturn(Arrays.asList("bro", "snort", "error"));

    searchService.getColumnMetadata(new ArrayList<>());

    verify(dao).getColumnMetadata(eq(Arrays.asList("bro", "snort", "metaalert")));

    verifyNoMoreInteractions(dao);
  }

  @Test
  public void testGetDefaultFacetFieldsGlobalConfig() throws RestException {
    when(environment.getProperty(SEARCH_FACET_FIELDS_SPRING_PROPERTY, String.class, ""))
        .thenReturn("ip_src_addr");
    Map<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(SENSOR_TYPE_FIELD_PROPERTY, "source.type");
    when(globalConfigService.get()).thenReturn(globalConfig);
    when(alertsUIService.getAlertsUIUserSettings()).thenReturn(Optional.empty());
    List<String> defaultFields = searchService.getDefaultFacetFields();

    List<String> expectedFields = new ArrayList<>();
    expectedFields.add("source.type");
    expectedFields.add("ip_src_addr");

    assertEquals(expectedFields, defaultFields);
  }

  @Test
  public void testGetDefaultFacetFieldsEmptyGlobalConfig() throws RestException {
    when(environment.getProperty(SEARCH_FACET_FIELDS_SPRING_PROPERTY, String.class, ""))
        .thenReturn("ip_src_addr");
    Map<String, Object> globalConfig = new HashMap<>();
    when(globalConfigService.get()).thenReturn(globalConfig);
    when(alertsUIService.getAlertsUIUserSettings()).thenReturn(Optional.empty());
    List<String> defaultFields = searchService.getDefaultFacetFields();

    List<String> expectedFields = new ArrayList<>();
    expectedFields.add("source:type");
    expectedFields.add("ip_src_addr");

    assertEquals(expectedFields, defaultFields);
  }
}
