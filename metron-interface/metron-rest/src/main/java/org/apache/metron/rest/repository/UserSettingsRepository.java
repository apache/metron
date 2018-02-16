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
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.UserSettings;
import org.apache.metron.rest.service.GlobalConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Repository
public class UserSettingsRepository {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static String USER_SETTINGS_HBASE_TABLE = "user.settings.hbase.table";
  public static String USER_SETTINGS_HBASE_CF = "user.settings.hbase.cf";

  public static ThreadLocal<ObjectMapper> _mapper = ThreadLocal.withInitial(() ->
          new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL));

  @Autowired
  @Qualifier("userSettingsTable")
  private HTableInterface userSettingsTable;
  private byte[] cf;
  private byte[] qualifer = Bytes.toBytes("value");


  @Autowired
  public UserSettingsRepository(Environment environment, HTableInterface userSettingsTable, GlobalConfigService globalConfigService) throws RestException {
    this.userSettingsTable = userSettingsTable;
    String cf = (String) globalConfigService.get().get(USER_SETTINGS_HBASE_CF);
    if (cf == null) {
      cf = environment.getProperty(MetronRestConstants.USER_SETTINGS_HBASE_CF_SPRING_PROPERTY);
    }
    this.cf = Bytes.toBytes(cf);
  }

  public UserSettings findOne(String user) throws IOException {
    byte[] rowKey = Bytes.toBytes(user);
    Get get = new Get(rowKey);
    get.addFamily(cf);
    Result result = userSettingsTable.get(get);
    if (result == null) {
      return null;
    } else {
      return getAlertProfileFromResult(result);
    }
  }

  public Map<String, UserSettings> findAll() throws IOException {
    Scan scan = new Scan();
    ResultScanner results = userSettingsTable.getScanner(scan);
    Map<String, UserSettings> allUserSettings = new HashMap<>();
    for (Result result : results) {
      UserSettings userSettings = getAlertProfileFromResult(result);
      if (userSettings != null) {
        allUserSettings.put(new String(result.getRow()), getAlertProfileFromResult(result));
      }
    }
    return allUserSettings;
  }

  public UserSettings save(UserSettings userSettings) throws IOException {
    byte[] rowKey = Bytes.toBytes(userSettings.getUser());
    Put put = new Put(rowKey);
    String value = _mapper.get().writeValueAsString(userSettings);
    put.addColumn(cf, qualifer, Bytes.toBytes(value));
    userSettingsTable.put(put);
    return userSettings;
  }

  public void delete(String user) throws IOException {
    Delete delete = new Delete(Bytes.toBytes(user));
    userSettingsTable.delete(delete);
  }

  private UserSettings getAlertProfileFromResult(Result result) throws IOException {
    byte[] value = result.getValue(cf, qualifer);
    if (value == null) {
      return null;
    } else {
      return _mapper.get().readValue(new String(value, StandardCharsets.UTF_8), UserSettings.class);
    }
  }
}
