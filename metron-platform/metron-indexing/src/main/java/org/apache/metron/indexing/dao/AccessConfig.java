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

import org.apache.metron.hbase.TableProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AccessConfig {
  private Integer maxSearchResults;
  private Integer maxSearchGroups;
  private Supplier<Map<String, Object>> globalConfigSupplier;
  private Map<String, String> optionalSettings = new HashMap<>();
  private TableProvider tableProvider = null;
  private Boolean isKerberosEnabled = false;

  /**
   * @return A supplier which will return the current global config.
   */
  public Supplier<Map<String, Object>> getGlobalConfigSupplier() {
    return globalConfigSupplier;
  }

  public void setGlobalConfigSupplier(Supplier<Map<String, Object>> globalConfigSupplier) {
    this.globalConfigSupplier = globalConfigSupplier;
  }

  /**
   * @return The maximum number of search results.
   */
  public Integer getMaxSearchResults() {
    return maxSearchResults;
  }

  public void setMaxSearchResults(Integer maxSearchResults) {
    this.maxSearchResults = maxSearchResults;
  }

  /**
   * @return The maximum number of search groups.
   */
  public Integer getMaxSearchGroups() {
    return maxSearchGroups;
  }

  public void setMaxSearchGroups(Integer maxSearchGroups) {
    this.maxSearchGroups = maxSearchGroups;
  }

  /**
   * @return Optional settings for initializing indices.
   */
  public Map<String, String> getOptionalSettings() {
    return optionalSettings;
  }

  public void setOptionalSettings(Map<String, String> optionalSettings) {
    this.optionalSettings = optionalSettings;
  }

  /**
   * @return The table provider to use for NoSql DAOs
   */
  public TableProvider getTableProvider() {
    return tableProvider;
  }

  public void setTableProvider(TableProvider tableProvider) {
    this.tableProvider = tableProvider;
  }

  /**
   * @return True if clients should be configured for Kerberos
   */
  public Boolean getKerberosEnabled() {
    return isKerberosEnabled;
  }

  public void setKerberosEnabled(Boolean kerberosEnabled) {
    isKerberosEnabled = kerberosEnabled;
  }
}
