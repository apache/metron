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
package org.apache.metron.common.configuration.extensions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ParserExtensionConfig implements Serializable{
  private String extensionAssemblyName;
  private String extensionBundleName;
  private String extensionsBundleID;
  private String extensionsBundleVersion;
  private Set<String> parserExtensionParserNames;
  private Map<String,SensorParserConfig> defaultParserConfigs;
  private Map<String,SensorEnrichmentConfig> defaultEnrichementConfigs;
  private Map<String,Map<String,Object>> defaultIndexingConfigs;

  public String getExtensionAssemblyName() {
    return extensionAssemblyName;
  }

  public void setExtensionAssemblyName(String extensionAssemblyName) {
    this.extensionAssemblyName = extensionAssemblyName;
  }

  public String getExtensionBundleName() {
    return extensionBundleName;
  }

  public void setExtensionBundleName(String extensionBundleName) {
    this.extensionBundleName = extensionBundleName;
  }

  public String getExtensionsBundleID() {
    return extensionsBundleID;
  }

  public void setExtensionsBundleID(String extensionsBundleID) {
    this.extensionsBundleID = extensionsBundleID;
  }

  public String getExtensionsBundleVersion() {
    return extensionsBundleVersion;
  }

  public void setExtensionsBundleVersion(String extensionsBundleVersion) {
    this.extensionsBundleVersion = extensionsBundleVersion;
  }

  public Set<String>  getParserExtensionParserNames() {
    if(this.parserExtensionParserNames != null) {
      return ImmutableSet.copyOf(this.parserExtensionParserNames);
    }
    return ImmutableSet.of();
  }

  public void setParserExtensionParserNames(Set<String> parserExtensionParserNames) {
    this.parserExtensionParserNames = new HashSet(parserExtensionParserNames);
  }

  public Map<String,SensorParserConfig> getDefaultParserConfigs() {
    if(this.defaultParserConfigs != null){
      return ImmutableMap.copyOf(this.defaultParserConfigs);
    }
    return ImmutableMap.of();
  }

  public void setDefaultParserConfigs(Map<String,SensorParserConfig> defaultParserConfigs) {
    this.defaultParserConfigs = new HashMap<>(defaultParserConfigs);
  }

  public Map<String,SensorEnrichmentConfig> getDefaultEnrichementConfigs() {
    if(this.defaultEnrichementConfigs != null){
      return ImmutableMap.copyOf(this.defaultEnrichementConfigs);
    }
    return ImmutableMap.of();
  }

  public void setDefaultEnrichementConfigs(Map<String,SensorEnrichmentConfig> defaultEnrichementConfigs) {
    this.defaultEnrichementConfigs = new HashMap<>(defaultEnrichementConfigs);
  }

  public Map<String,Map<String, Object>> getDefaultIndexingConfigs() {
    if(this.defaultIndexingConfigs != null){
      return ImmutableMap.copyOf(this.defaultIndexingConfigs);
    }
    return ImmutableMap.of();
  }

  public void setDefaultIndexingConfigs(Map<String,Map<String, Object>> defaultIndexingConfigs) {
    this.defaultIndexingConfigs = new HashMap<>(defaultIndexingConfigs);
  }

  public void setParserExtensionParserName(Collection<String> parserExtensionParserNames) {
    this.parserExtensionParserNames = new HashSet();
    this.parserExtensionParserNames.addAll(parserExtensionParserNames);
  }



  public static ParserExtensionConfig fromBytes(byte[] config) throws IOException {
    ParserExtensionConfig ret = JSONUtils.INSTANCE.load(new String(config), ParserExtensionConfig.class);
    return ret;
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

  @Override
  public String toString() {
    return "SensorParserConfig{" +
            "extensionAssemblyName='" + extensionAssemblyName + '\'' +
            ", extensionBundleName='" + extensionBundleName + '\'' +
            ", extensionsBundleID='" + extensionsBundleID + '\'' +
            ", parserExtensionParserNames='" + String.join(",",this.parserExtensionParserNames) + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ParserExtensionConfig that = (ParserExtensionConfig) o;

    if (getExtensionAssemblyName() != null ? !getExtensionAssemblyName().equals(that.getExtensionAssemblyName()) : that.getExtensionAssemblyName() != null)
      return false;
    if (getExtensionBundleName() != null ? !getExtensionBundleName().equals(that.getExtensionBundleName()) : that.getExtensionBundleName() != null)
      return false;
    if (getExtensionsBundleID() != null ? !getExtensionsBundleID().equals(that.getExtensionsBundleID()) : that.getExtensionsBundleID() != null)
      return false;
    if (getExtensionsBundleVersion() != null ? !getExtensionsBundleVersion().equals(that.getExtensionsBundleVersion()) : that.getExtensionsBundleVersion() != null)
      return false;
    if (getDefaultParserConfigs() != null ? !getDefaultParserConfigs().equals(that.getDefaultParserConfigs()) : that.getDefaultParserConfigs() != null)
      return false;
    if (getDefaultEnrichementConfigs() != null ? !getDefaultEnrichementConfigs().equals(that.getDefaultEnrichementConfigs()) : that.getDefaultEnrichementConfigs() != null)
      return false;
    if (getDefaultIndexingConfigs() != null ? !getDefaultIndexingConfigs().equals(that.getDefaultIndexingConfigs()) : that.getDefaultIndexingConfigs() != null)
      return false;
    return getParserExtensionParserNames() != null ? getParserExtensionParserNames().equals(that.getParserExtensionParserNames()) : that.getParserExtensionParserNames() == null;
  }

  @Override
  public int hashCode() {
    int result = getExtensionAssemblyName() != null ? getExtensionAssemblyName().hashCode() : 0;
    result = 31 * result + (getExtensionBundleName() != null ? getExtensionBundleName().hashCode() : 0);
    result = 31 * result + (getExtensionsBundleID() != null ? getExtensionsBundleID().hashCode() : 0);
    result = 31 * result + (getExtensionsBundleVersion() != null ? getExtensionsBundleVersion().hashCode() : 0);
    result = 31 * result + (getDefaultParserConfigs() != null ? getDefaultParserConfigs().hashCode() : 0);
    result = 31 * result + (getDefaultEnrichementConfigs() != null ? getDefaultEnrichementConfigs().hashCode() : 0);
    result = 31 * result + (getDefaultIndexingConfigs() != null ? getDefaultIndexingConfigs().hashCode() : 0);
    result = 31 * result + (getParserExtensionParserNames() != null ? getParserExtensionParserNames().hashCode() : 0);
    return result;
  }
}
