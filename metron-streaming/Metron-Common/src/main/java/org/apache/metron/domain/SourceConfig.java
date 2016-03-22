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

import org.codehaus.jackson.map.ObjectMapper;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceConfig {

  final static ThreadLocal<ObjectMapper> _mapper = new ThreadLocal<ObjectMapper>() {
    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     * <p>
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     */
    @Override
    protected ObjectMapper initialValue() {
      return new ObjectMapper();
    }
  };

  private String index;
  private Map<String, List<String>> enrichmentFieldMap;
  private Map<String, List<String>> threatIntelFieldMap;
  private Map<String, List<String>> fieldToEnrichmentTypeMap = new HashMap<>();
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

  public void setFieldToEnrichmentTypeMap(Map<String, List<String>> fieldToEnrichmentTypeMap) {
    this.fieldToEnrichmentTypeMap = fieldToEnrichmentTypeMap;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SourceConfig that = (SourceConfig) o;

    if (getBatchSize() != that.getBatchSize()) return false;
    if (getIndex() != null ? !getIndex().equals(that.getIndex()) : that.getIndex() != null) return false;
    if (getEnrichmentFieldMap() != null ? !getEnrichmentFieldMap().equals(that.getEnrichmentFieldMap()) : that.getEnrichmentFieldMap() != null)
      return false;
    if (getThreatIntelFieldMap() != null ? !getThreatIntelFieldMap().equals(that.getThreatIntelFieldMap()) : that.getThreatIntelFieldMap() != null)
      return false;
    return getFieldToEnrichmentTypeMap() != null ? getFieldToEnrichmentTypeMap().equals(that.getFieldToEnrichmentTypeMap()) : that.getFieldToEnrichmentTypeMap() == null;

  }

  @Override
  public int hashCode() {
    int result = getIndex() != null ? getIndex().hashCode() : 0;
    result = 31 * result + (getEnrichmentFieldMap() != null ? getEnrichmentFieldMap().hashCode() : 0);
    result = 31 * result + (getThreatIntelFieldMap() != null ? getThreatIntelFieldMap().hashCode() : 0);
    result = 31 * result + (getFieldToEnrichmentTypeMap() != null ? getFieldToEnrichmentTypeMap().hashCode() : 0);
    result = 31 * result + getBatchSize();
    return result;
  }

  public static synchronized SourceConfig load(InputStream is) throws IOException {
    SourceConfig ret = _mapper.get().readValue(is, SourceConfig.class);
    return ret;
  }

  public static synchronized SourceConfig load(byte[] data) throws IOException {
    return load( new ByteArrayInputStream(data));
  }

  public static synchronized SourceConfig load(String s, Charset c) throws IOException {
    return load( s.getBytes(c));
  }
  public static synchronized SourceConfig load(String s) throws IOException {
    return load( s, Charset.defaultCharset());
  }
}
