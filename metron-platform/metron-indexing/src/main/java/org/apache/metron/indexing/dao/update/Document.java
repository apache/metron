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

package org.apache.metron.indexing.dao.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class Document {
  Long timestamp;
  Map<String, Object> document;
  String guid;
  String sensorType;

  public Document(Map<String, Object> document, String guid, String sensorType, Long timestamp) {
    setDocument(document);
    setGuid(guid);
    setTimestamp(timestamp);
    setSensorType(sensorType);
  }


  public Document(String document, String guid, String sensorType, Long timestamp) throws IOException {
    this(convertDoc(document), guid, sensorType, timestamp);
  }

  public Document(String document, String guid, String sensorType) throws IOException {
    this( document, guid, sensorType, null);
  }

  private static Map<String, Object> convertDoc(String document) throws IOException {
      return JSONUtils.INSTANCE.load(document, new TypeReference<Map<String, Object>>() {
      });
  }

  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp != null?timestamp:System.currentTimeMillis();
  }

  public Map<String, Object> getDocument() {
    return document;
  }

  public void setDocument(Map<String, Object> document) {
    this.document = document;
  }

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }
}
