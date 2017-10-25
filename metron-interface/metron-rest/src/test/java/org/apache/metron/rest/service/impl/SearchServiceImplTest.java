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

import static org.apache.metron.rest.MetronRestConstants.INDEX_WRITER_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.search.InvalidSearchException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SearchService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;

@SuppressWarnings("ALL")
public class SearchServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  IndexDao dao;
  Environment environment;
  SensorIndexingConfigService sensorIndexingConfigService;
  SearchService searchService;

  @Before
  public void setUp() throws Exception {
    dao = mock(IndexDao.class);
    environment = mock(Environment.class);
    sensorIndexingConfigService = mock(SensorIndexingConfigService.class);
    searchService = new SearchServiceImpl(dao, environment, sensorIndexingConfigService);
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
  public void searchShouldProperlySearch() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro"));
    searchService.search(searchRequest);

    SearchRequest expectedSearchRequest = new SearchRequest();
    expectedSearchRequest.setIndices(Arrays.asList("bro"));
    verify(dao).search(eq(expectedSearchRequest));

    verifyNoMoreInteractions(dao);
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    when(dao.search(any(SearchRequest.class))).thenThrow(InvalidSearchException.class);

    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setIndices(Arrays.asList("bro"));
    searchService.search(searchRequest);
  }
}
