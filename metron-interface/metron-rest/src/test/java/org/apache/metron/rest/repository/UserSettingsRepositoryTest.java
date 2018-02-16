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
package org.apache.metron.rest.repository;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.rest.model.UserSettings;
import org.apache.metron.rest.service.impl.GlobalConfigServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.HashMap;

import static org.apache.metron.rest.repository.UserSettingsRepository.USER_SETTINGS_HBASE_CF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class UserSettingsRepositoryTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  /**
   {
     "facetFields": ["facetField1"],
     "savedSearches": [
       {
         "name": "savedSearch1",
         "searchRequest": {
           "facetFields": ["searchFacetField1"],
           "fields": ["searchField1"],
           "from": 0,
           "indices": ["index1"],
           "query": "query1",
           "size": 5
         }
       }
     ],
     "tableColumns": ["tableColumn1"]
   }
   */
  @Multiline
  private static String userSettings1String;

  /**
   {
     "facetFields": ["facetField2"],
     "savedSearches": [
       {
         "name": "savedSearch2",
         "searchRequest": {
           "facetFields": ["searchFacetField2"],
           "fields": ["searchField2"],
           "from": 1,
           "indices": ["index2"],
           "query": "query2",
           "size": 10
         }
       }
     ],
     "tableColumns": ["tableColumn2"]
   }
   */
  @Multiline
  private static String userSettings2String;
  private UserSettings userSettings1;
  private UserSettings userSettings2;

  private Environment environment;
  private HTableInterface userSettingsTable;
  private GlobalConfigServiceImpl globalConfigService;
  private UserSettingsRepository userSettingsRepository;
  private static byte[] cf = Bytes.toBytes("cf");
  private static byte[] qualifer = Bytes.toBytes("value");

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    userSettingsTable = mock(HTableInterface.class);
    globalConfigService = mock(GlobalConfigServiceImpl.class);
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>() {{
      put(USER_SETTINGS_HBASE_CF, "cf");
    }});
    userSettings1 = _mapper.get().readValue(userSettings1String, UserSettings.class);
    userSettings2 = _mapper.get().readValue(userSettings2String, UserSettings.class);
  }

  @Test
  public void shouldFindOne() throws Exception {
    Result result = mock(Result.class);
    when(result.getValue(cf, qualifer)).thenReturn(userSettings1String.getBytes());
    Get get = new Get("user1".getBytes());
    get.addFamily(cf);
    when(userSettingsTable.get(get)).thenReturn(result);

    UserSettingsRepository userSettingsRepository = new UserSettingsRepository(environment, userSettingsTable, globalConfigService);
    assertEquals(userSettings1, userSettingsRepository.findOne("user1"));
    assertNull(userSettingsRepository.findOne("missingUser"));
  }

  @Test
  public void shouldFindAll() throws Exception {
    ResultScanner resultScanner = mock(ResultScanner.class);
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(result1.getRow()).thenReturn("user1".getBytes());
    when(result2.getRow()).thenReturn("user2".getBytes());
    when(result1.getValue(cf, qualifer)).thenReturn(userSettings1String.getBytes());
    when(result2.getValue(cf, qualifer)).thenReturn(userSettings2String.getBytes());
    when(resultScanner.iterator()).thenReturn(Arrays.asList(result1, result2).iterator());
    when(userSettingsTable.getScanner(any(Scan.class))).thenReturn(resultScanner);

    UserSettingsRepository userSettingsRepository = new UserSettingsRepository(environment, userSettingsTable, globalConfigService);
    assertEquals(new HashMap<String, UserSettings>() {{
      put("user1", userSettings1);
      put("user2", userSettings2);
    }}, userSettingsRepository.findAll());
  }

}
