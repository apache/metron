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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.metron.common.utils.JSONUtils;

public class Document {
  Long timestamp;
  Map<String, Object> document;
  String guid;
  String sensorType;
  Optional<String> index;

  public Document(Map<String, Object> document, String guid, String sensorType, Long timestamp) {
    setDocument(document);
    setGuid(guid);
    setTimestamp(timestamp);
    setSensorType(sensorType);
    index = Optional.empty();
  }

  public Document(Map<String, Object> document, String guid, String sensorType, Long timestamp, Optional<String> index) {
    setDocument(document);
    setGuid(guid);
    setTimestamp(timestamp);
    setSensorType(sensorType);
    setIndex(index);
  }

  public Document(String document, String guid, String sensorType, Long timestamp) throws IOException {
    this(convertDoc(document), guid, sensorType, timestamp);
  }

  public Document(String document, String guid, String sensorType) throws IOException {
    this( document, guid, sensorType, null);
  }

  /**
   * Copy constructor
   * @param other The document to be copied.
   */
  public Document(Document other) {
    this(new HashMap<>(other.getDocument()), other.getGuid(), other.getSensorType(),
        other.getTimestamp());
  }

  private static Map<String, Object> convertDoc(String document) throws IOException {
      return JSONUtils.INSTANCE.load(document, JSONUtils.MAP_SUPPLIER);
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

  public Optional<String> getIndex() {
    return index;
  }

  public void setIndex(Optional<String> index) {
    this.index = index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Document)) {
      return false;
    }
    Document document1 = (Document) o;
    return new EqualsBuilder()
            .append(timestamp, document1.timestamp)
            .append(document, document1.document)
            .append(guid, document1.guid)
            .append(sensorType, document1.sensorType)
            .append(index, document1.index)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(timestamp)
            .append(document)
            .append(guid)
            .append(sensorType)
            .append(index)
            .toHashCode();
  }

  @Override
  public String toString() {
    return "Document{" +
            "timestamp=" + timestamp +
            ", document=" + document +
            ", guid='" + guid + '\'' +
            ", sensorType='" + sensorType + '\'' +
            ", index=" + index +
            '}';
  }
}
