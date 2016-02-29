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
import java.util.List;
import java.util.Map;

public class SourceConfig {

  final static ObjectMapper _mapper = new ObjectMapper();

  private String index;
  private Map<String, List<String>> enrichmentFieldMap;
  private Map<String, List<String>> threatIntelFieldMap;
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

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public static synchronized SourceConfig load(InputStream is) throws IOException {
    SourceConfig ret = _mapper.readValue(is, SourceConfig.class);
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
