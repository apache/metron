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

package org.apache.metron.common.configuration.enrichment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrichmentConfig {
  private Map<String, List<String>> fieldMap = new HashMap<>();
  private Map<String, List<String>> fieldToTypeMap = new HashMap<>();
  private Map<String, Object> config = new HashMap<>();

  public Map<String, Object> getConfig() {
    return config;
  }

  public void setConfig(Map<String, Object> config) {
    this.config = config;
  }

  public Map<String, List<String>> getFieldMap() {
    return fieldMap;
  }

  public void setFieldMap(Map<String, List<String>> fieldMap) {
    this.fieldMap = fieldMap;
  }

  public Map<String, List<String>> getFieldToTypeMap() {
    return fieldToTypeMap;
  }

  public void setFieldToTypeMap(Map<String, List<String>> fieldToTypeMap) {
    this.fieldToTypeMap = fieldToTypeMap;
  }

  @Override
  public String toString() {
    return "EnrichmentConfig{" +
            "fieldMap=" + fieldMap +
            ", fieldToTypeMap=" + fieldToTypeMap +
            ", config=" + config +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EnrichmentConfig that = (EnrichmentConfig) o;

    if (getFieldMap() != null ? !getFieldMap().equals(that.getFieldMap()) : that.getFieldMap() != null) return false;
    if (getFieldToTypeMap() != null ? !getFieldToTypeMap().equals(that.getFieldToTypeMap()) : that.getFieldToTypeMap() != null)
      return false;
    return getConfig() != null ? getConfig().equals(that.getConfig()) : that.getConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getFieldMap() != null ? getFieldMap().hashCode() : 0;
    result = 31 * result + (getFieldToTypeMap() != null ? getFieldToTypeMap().hashCode() : 0);
    result = 31 * result + (getConfig() != null ? getConfig().hashCode() : 0);
    return result;
  }
}
