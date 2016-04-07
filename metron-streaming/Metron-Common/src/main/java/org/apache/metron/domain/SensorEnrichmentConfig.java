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
package org.apache.metron.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.metron.utils.JSONUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorEnrichmentConfig {

  private String index;
  private Map<String, List<String>> enrichmentFieldMap;
  private Map<String, List<String>> threatIntelFieldMap;
  private Map<String, List<String>> fieldToEnrichmentTypeMap = new HashMap<>();
  private Map<String, List<String>> fieldToThreatIntelTypeMap = new HashMap<>();
  private int batchSize;

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public Map<String, List<String>> getEnrichmentFieldMap() {
    return enrichmentFieldMap;
  }

  public void setEnrichmentFieldMap(Map<String, List<String>> enrichmentFieldMap) {
    this.enrichmentFieldMap = enrichmentFieldMap;
  }

  public Map<String, List<String>> getThreatIntelFieldMap() {
    return threatIntelFieldMap;
  }

  public void setThreatIntelFieldMap(Map<String, List<String>> threatIntelFieldMap) {
    this.threatIntelFieldMap = threatIntelFieldMap;
  }

  public Map<String, List<String>> getFieldToEnrichmentTypeMap() {
    return fieldToEnrichmentTypeMap;
  }

  public Map<String, List<String>> getFieldToThreatIntelTypeMap() {
    return fieldToThreatIntelTypeMap;
  }
  public void setFieldToEnrichmentTypeMap(Map<String, List<String>> fieldToEnrichmentTypeMap) {
    this.fieldToEnrichmentTypeMap = fieldToEnrichmentTypeMap;
  }

  public void setFieldToThreatIntelTypeMap(Map<String, List<String>> fieldToThreatIntelTypeMap) {
    this.fieldToThreatIntelTypeMap= fieldToThreatIntelTypeMap;
  }
  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public static SensorEnrichmentConfig fromBytes(byte[] config) throws IOException {
    return JSONUtils.INSTANCE.load(new String(config), SensorEnrichmentConfig.class);
  }
  public String toJSON(boolean pretty) throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, pretty);
  }
  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorEnrichmentConfig that = (SensorEnrichmentConfig) o;

    if (getBatchSize() != that.getBatchSize()) return false;
    if (getIndex() != null ? !getIndex().equals(that.getIndex()) : that.getIndex() != null) return false;
    if (getEnrichmentFieldMap() != null ? !getEnrichmentFieldMap().equals(that.getEnrichmentFieldMap()) : that.getEnrichmentFieldMap() != null)
      return false;
    if (getThreatIntelFieldMap() != null ? !getThreatIntelFieldMap().equals(that.getThreatIntelFieldMap()) : that.getThreatIntelFieldMap() != null)
      return false;
    if (getFieldToEnrichmentTypeMap() != null ? !getFieldToEnrichmentTypeMap().equals(that.getFieldToEnrichmentTypeMap()) : that.getFieldToEnrichmentTypeMap() != null)
      return false;
    return getFieldToThreatIntelTypeMap() != null ? getFieldToThreatIntelTypeMap().equals(that.getFieldToThreatIntelTypeMap()) : that.getFieldToThreatIntelTypeMap() == null;

  }

  @Override
  public int hashCode() {
    int result = getIndex() != null ? getIndex().hashCode() : 0;
    result = 31 * result + (getEnrichmentFieldMap() != null ? getEnrichmentFieldMap().hashCode() : 0);
    result = 31 * result + (getThreatIntelFieldMap() != null ? getThreatIntelFieldMap().hashCode() : 0);
    result = 31 * result + (getFieldToEnrichmentTypeMap() != null ? getFieldToEnrichmentTypeMap().hashCode() : 0);
    result = 31 * result + (getFieldToThreatIntelTypeMap() != null ? getFieldToThreatIntelTypeMap().hashCode() : 0);
    result = 31 * result + getBatchSize();
    return result;
  }
}
