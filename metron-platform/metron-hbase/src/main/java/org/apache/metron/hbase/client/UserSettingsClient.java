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
package org.apache.metron.hbase.client;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.TableProvider;
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

public class UserSettingsClient {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static String USER_SETTINGS_HBASE_TABLE = "user.settings.hbase.table";
  public static String USER_SETTINGS_HBASE_CF = "user.settings.hbase.cf";

  private HTableInterface userSettingsTable;
  private byte[] cf;
  private Supplier<Map<String, Object>> globalConfigSupplier;
  private TableProvider tableProvider;

  public UserSettingsClient() {
  }

  public UserSettingsClient(Supplier<Map<String, Object>> globalConfigSupplier, TableProvider tableProvider) {
    this.globalConfigSupplier = globalConfigSupplier;
    this.tableProvider = tableProvider;
  }

  public UserSettingsClient(HTableInterface userSettingsTable, byte[] cf) {
    this.userSettingsTable = userSettingsTable;
    this.cf = cf;
  }

  public synchronized void init(Supplier<Map<String, Object>> globalConfigSupplier, TableProvider tableProvider) {
    if (this.userSettingsTable == null) {
      Map<String, Object> globalConfig = globalConfigSupplier.get();
      if(globalConfig == null) {
        throw new IllegalStateException("Cannot find the global config.");
      }
      String table = (String)globalConfig.get(USER_SETTINGS_HBASE_TABLE);
      String cf = (String) globalConfigSupplier.get().get(USER_SETTINGS_HBASE_CF);
      if(table == null || cf == null) {
        throw new IllegalStateException("You must configure " + USER_SETTINGS_HBASE_TABLE + " and " + USER_SETTINGS_HBASE_CF + " in the global config.");
      }
      try {
        userSettingsTable = tableProvider.getTable(HBaseConfiguration.create(), table);
        this.cf = cf.getBytes();
      } catch (IOException e) {
        throw new IllegalStateException("Unable to initialize HBaseDao: " + e.getMessage(), e);
      }

    }
  }

  public HTableInterface getTableInterface() {
    if(userSettingsTable == null) {
      init(globalConfigSupplier, tableProvider);
    }
    return userSettingsTable;
  }

  public Map<String, String> findOne(String user) throws IOException {
    Result result = getResult(user);
    return getAllUserSettings(result);
  }

  public Optional<String> findOne(String user, String type) throws IOException {
    Result result = getResult(user);
    return getUserSettings(result, type);
  }

  public Map<String, Map<String, String>> findAll() throws IOException {
    Scan scan = new Scan();
    ResultScanner results = getTableInterface().getScanner(scan);
    Map<String, Map<String, String>> allUserSettings = new HashMap<>();
    for (Result result : results) {
      allUserSettings.put(new String(result.getRow()), getAllUserSettings(result));
    }
    return allUserSettings;
  }

  public Map<String, Optional<String>> findAll(String type) throws IOException {
    Scan scan = new Scan();
    ResultScanner results = getTableInterface().getScanner(scan);
    Map<String, Optional<String>> allUserSettings = new HashMap<>();
    for (Result result : results) {
      allUserSettings.put(new String(result.getRow()), getUserSettings(result, type));
    }
    return allUserSettings;
  }

  public void save(String user, String type, String userSettings) throws IOException {
    byte[] rowKey = Bytes.toBytes(user);
    Put put = new Put(rowKey);
    put.addColumn(cf, Bytes.toBytes(type), Bytes.toBytes(userSettings));
    getTableInterface().put(put);
  }

  public void delete(String user) throws IOException {
    Delete delete = new Delete(Bytes.toBytes(user));
    getTableInterface().delete(delete);
  }

  public void delete(String user, String type) throws IOException {
    Delete delete = new Delete(Bytes.toBytes(user));
    delete.addColumn(cf, Bytes.toBytes(type));
    getTableInterface().delete(delete);
  }

  private Result getResult(String user) throws IOException {
    byte[] rowKey = Bytes.toBytes(user);
    Get get = new Get(rowKey);
    get.addFamily(cf);
    return getTableInterface().get(get);
  }

  private Optional<String> getUserSettings(Result result, String type) throws IOException {
    Optional<String> userSettings = Optional.empty();
    if (result != null) {
      byte[] value = result.getValue(cf, Bytes.toBytes(type));
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
    NavigableMap<byte[], byte[]> columns = result.getFamilyMap(cf);
    if(columns == null || columns.size() == 0) {
      return new HashMap<>();
    }
    Map<String, String> userSettingsMap = new HashMap<>();
    for(Map.Entry<byte[], byte[]> column: columns.entrySet()) {
      userSettingsMap.put(new String(column.getKey(), StandardCharsets.UTF_8), new String(column.getValue(), StandardCharsets.UTF_8));
    }
    return userSettingsMap;
  }
}
