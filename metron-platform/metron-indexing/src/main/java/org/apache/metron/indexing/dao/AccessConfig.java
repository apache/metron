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
package org.apache.metron.indexing.dao;

import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AccessConfig {
  private Integer maxSearchResults;
  private Supplier<Map<String, Object>> globalConfigSupplier;
  private Map<String, String> optionalSettings = new HashMap<>();
  private TableProvider tableProvider = null;
  private String columnFamily;
  private String table;

  public Supplier<Map<String, Object>> getGlobalConfigSupplier() {
    return globalConfigSupplier;
  }

  public void setGlobalConfigSupplier(Supplier<Map<String, Object>> globalConfigSupplier) {
    this.globalConfigSupplier = globalConfigSupplier;
  }

  public Integer getMaxSearchResults() {
    return maxSearchResults;
  }

  public void setMaxSearchResults(Integer maxSearchResults) {
    this.maxSearchResults = maxSearchResults;
  }

  public Map<String, String> getOptionalSettings() {
    return optionalSettings;
  }

  public void setOptionalSettings(Map<String, String> optionalSettings) {
    this.optionalSettings = optionalSettings;
  }

  public TableProvider getTableProvider() {
    return tableProvider;
  }

  public void setTableProvider(TableProvider tableProvider) {
    this.tableProvider = tableProvider;
  }

  public String getColumnFamily() {
    return columnFamily;
  }

  public void setColumnFamily(String columnFamily) {
    this.columnFamily = columnFamily;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }
}
