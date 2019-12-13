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
package org.apache.metron.rest.user;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.apache.metron.rest.user.UserSettingsClient.USER_SETTINGS_HBASE_CF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserSettingsClientTest {
  private static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  private Table userSettingsTable;
  private Supplier<Map<String, Object>> globalConfigSupplier;
  private UserSettingsClient userSettingsClient;
  private static byte[] cf = Bytes.toBytes("cf");

  @BeforeEach
  public void setUp() {
    userSettingsTable = mock(Table.class);
    globalConfigSupplier = () -> new HashMap<String, Object>() {{
      put(USER_SETTINGS_HBASE_CF, "cf");
    }};
  }

  @Test
  public void shouldFindOne() throws Exception {
    Result result = mock(Result.class);
    when(result.getValue(cf, Bytes.toBytes("type"))).thenReturn("userSettings1String".getBytes(
        StandardCharsets.UTF_8));
    Get get = new Get("user1".getBytes(StandardCharsets.UTF_8));
    get.addFamily(cf);
    when(userSettingsTable.get(get)).thenReturn(result);

    UserSettingsClient userSettingsClient = new UserSettingsClient(userSettingsTable, cf);
    assertEquals("userSettings1String", userSettingsClient.findOne("user1", "type").get());
    assertFalse(userSettingsClient.findOne("missingUser", "type").isPresent());
  }

  @Test
  public void shouldFindAll() throws Exception {
    ResultScanner resultScanner = mock(ResultScanner.class);
    Result result1 = mock(Result.class);
    Result result2 = mock(Result.class);
    when(result1.getRow()).thenReturn("user1".getBytes(StandardCharsets.UTF_8));
    when(result2.getRow()).thenReturn("user2".getBytes(StandardCharsets.UTF_8));
    when(result1.getValue(cf, Bytes.toBytes("type"))).thenReturn("userSettings1String".getBytes(
        StandardCharsets.UTF_8));
    when(result2.getValue(cf, Bytes.toBytes("type"))).thenReturn("userSettings2String".getBytes(
        StandardCharsets.UTF_8));
    when(resultScanner.iterator()).thenReturn(Arrays.asList(result1, result2).iterator());
    when(userSettingsTable.getScanner(any(Scan.class))).thenReturn(resultScanner);

    UserSettingsClient userSettingsClient = new UserSettingsClient(userSettingsTable, cf);
    assertEquals(new HashMap<String, Optional<String>>() {{
      put("user1", Optional.of("userSettings1String"));
      put("user2", Optional.of("userSettings2String"));
    }}, userSettingsClient.findAll("type"));
  }

}
