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
package org.apache.metron.rest.user;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.HBaseProjectionCriteria;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link UserSettingsClient} that interacts with user settings persisted in HBase.
 */
public class HBaseUserSettingsClient implements UserSettingsClient {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String USER_SETTINGS_HBASE_TABLE = "user.settings.hbase.table";
  public static final String USER_SETTINGS_HBASE_CF = "user.settings.hbase.cf";
  public static final String USER_SETTINGS_MAX_SCAN = "user.settings.max.scan";
  private static final int DEFAULT_MAX_SCAN = 100_000;

  private String columnFamily;
  private Supplier<Map<String, Object>> globalConfigSupplier;
  private HBaseConnectionFactory hBaseConnectionFactory;
  private Configuration hBaseConfiguration;
  private HBaseClientFactory hBaseClientFactory;
  private HBaseClient hBaseClient;
  private int maxScanCount;

  public HBaseUserSettingsClient(Supplier<Map<String, Object>> globalConfigSupplier,
                                 HBaseClientFactory hBaseClientFactory,
                                 HBaseConnectionFactory hBaseConnectionFactory,
                                 Configuration hBaseConfiguration) {
    this.globalConfigSupplier = globalConfigSupplier;
    this.hBaseClientFactory = hBaseClientFactory;
    this.hBaseConnectionFactory = hBaseConnectionFactory;
    this.hBaseConfiguration = hBaseConfiguration;
  }

  @Override
  public synchronized void init() {
    if (hBaseClient == null) {
      Map<String, Object> globals = getGlobals();
      columnFamily = getColumnFamily(globals);
      maxScanCount = getMaxScanCount(globals);
      hBaseClient = hBaseClientFactory.create(hBaseConnectionFactory, hBaseConfiguration,  getTableName(globals));
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if(hBaseClient != null) {
      hBaseClient.close();
    }
  }

  @Override
  public Map<String, String> findOne(String user) throws IOException {
    Result result = getResult(user);
    return getAllUserSettings(result);
  }

  @Override
  public Optional<String> findOne(String user, String type) throws IOException {
    Result result = getResult(user);
    return getUserSettings(result, type);
  }

  @Override
  public Map<String, Map<String, String>> findAll() throws IOException {
    Map<String, Map<String, String>> settings = new HashMap<>();
    for (Result result : hBaseClient.scan(maxScanCount)) {
      String user = new String(result.getRow());
      settings.put(user, getAllUserSettings(result));
    }
    return settings;
  }

  @Override
  public Map<String, Optional<String>> findAll(String type) throws IOException {
    Map<String, Optional<String>> settings = new HashMap<>();
    for (Result result : hBaseClient.scan(maxScanCount)) {
      String user = new String(result.getRow());
      settings.put(user, getUserSettings(result, type));
    }
    return settings;
  }

  @Override
  public void save(String user, String type, String userSettings) throws IOException {
    byte[] rowKey = userToRowKey(user);
    ColumnList columns = new ColumnList().addColumn(columnFamily, type, userSettings);
    hBaseClient.addMutation(rowKey, columns);
    hBaseClient.mutate();
  }

  @Override
  public void delete(String user) {
    hBaseClient.delete(userToRowKey(user));
  }

  @Override
  public void delete(String user, String type)  {
    byte[] rowKey = userToRowKey(user);
    ColumnList columns = new ColumnList().addColumn(columnFamily, type);
    hBaseClient.delete(rowKey, columns);
  }

  private Result getResult(String user) {
    // for the given user's row, retrieve the column family
    byte[] rowKey = userToRowKey(user);
    HBaseProjectionCriteria criteria = new HBaseProjectionCriteria().addColumnFamily(columnFamily);
    hBaseClient.addGet(rowKey, criteria);
    Result[] results = hBaseClient.getAll();

    // expect 1 result, if the user settings exist
    Result result = null;
    if(results.length > 0) {
      result = results[0];
    } else {
      LOG.debug("No result found; user={}, columnFamily={}", user, columnFamily);
    }
    return result;
  }

  private byte[] userToRowKey(String user) {
    return Bytes.toBytes(user);
  }

  private Optional<String> getUserSettings(Result result, String type) throws IOException {
    Optional<String> userSettings = Optional.empty();
    if (result != null) {
      byte[] value = result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(type));
      if (value != null) {
        userSettings = Optional.of(new String(value, StandardCharsets.UTF_8));
      }
    }
    return userSettings;
  }

  public Map<String, String> getAllUserSettings(Result result) {
    if (result == null) {
      return new HashMap<>();
    }
    NavigableMap<byte[], byte[]> columns = result.getFamilyMap(Bytes.toBytes(columnFamily));
    if(columns == null || columns.size() == 0) {
      return new HashMap<>();
    }
    Map<String, String> userSettingsMap = new HashMap<>();
    for(Map.Entry<byte[], byte[]> column: columns.entrySet()) {
      userSettingsMap.put(
              new String(column.getKey(), StandardCharsets.UTF_8),
              new String(column.getValue(), StandardCharsets.UTF_8));
    }
    return userSettingsMap;
  }

  private Map<String, Object> getGlobals() {
    Map<String, Object> globalConfig = globalConfigSupplier.get();
    if(globalConfig == null) {
      throw new IllegalStateException("Cannot find the global config.");
    }
    return globalConfig;
  }

  private static String getTableName(Map<String, Object> globals) {
    String table = (String) globals.get(USER_SETTINGS_HBASE_TABLE);
    if(table == null) {
      throw new IllegalStateException("You must configure " + USER_SETTINGS_HBASE_TABLE + "in the global config.");
    }
    return table;
  }

  private static String getColumnFamily(Map<String, Object> globals) {
    String cf = (String) globals.get(USER_SETTINGS_HBASE_CF);
    if(cf == null) {
      throw new IllegalStateException("You must configure " + USER_SETTINGS_HBASE_CF + " in the global config.");
    }
    return cf;
  }

  private static int getMaxScanCount(Map<String, Object> globals) {
    Integer maxScanCount = DEFAULT_MAX_SCAN;
    if(globals.containsKey(USER_SETTINGS_MAX_SCAN)) {
      String value = (String) globals.get(USER_SETTINGS_MAX_SCAN);
      maxScanCount = Integer.valueOf(value);
    }
    return maxScanCount;
  }
}
