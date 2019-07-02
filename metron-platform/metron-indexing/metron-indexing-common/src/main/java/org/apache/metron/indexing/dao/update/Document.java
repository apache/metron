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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.metron.common.Constants.Fields.TIMESTAMP;
import static org.apache.metron.common.Constants.GUID;
import static org.apache.metron.common.Constants.SENSOR_TYPE;
import static org.apache.metron.indexing.dao.IndexDao.COMMENTS_FIELD;

import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.indexing.dao.search.AlertComment;

public class Document {

  Long timestamp;
  Map<String, Object> document;
  String guid;
  String sensorType;
  String documentID;

  public static Document fromJSON(Map<String, Object> json) {
    String guid = getGUID(json);
    Long timestamp = getTimestamp(json).orElse(0L);
    String sensorType = getSensorType(json);
    return new Document(json, guid, sensorType, timestamp);
  }

  public Document(Map<String, Object> document, String guid, String sensorType, Long timestamp) {
    this(document, guid, sensorType, timestamp, null);
  }

  public Document(Map<String, Object> document, String guid, String sensorType, Long timestamp, String documentID) {
    setDocument(document);
    setGuid(guid);
    setTimestamp(timestamp);
    setSensorType(sensorType);
    setDocumentID(documentID);
  }

  public Document(String document, String guid, String sensorType, Long timestamp) throws IOException {
    this(convertDoc(document), guid, sensorType, timestamp);
  }

  public Document(String document, String guid, String sensorType) throws IOException {
    this(document, guid, sensorType, null);
  }

  /**
   * Copy constructor
   * @param other The document to be copied.
   */
  public Document(Document other) {
    this(new HashMap<>(other.getDocument()),
            other.getGuid(),
            other.getSensorType(),
            other.getTimestamp(),
            other.getDocumentID().orElse(null));
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

  /**
   * Returns the unique identifier that is used when persisting this document.
   *
   * <p>This value will be different than the Metron guid.
   *
   * <p>Only present when a document has been retrieved from a store
   * that supports a document ID, like Elasticsearch.  This value will
   * not be present when retrieved from HBase.
   */
  public Optional<String> getDocumentID() {
    return Optional.ofNullable(documentID);
  }

  public void setDocumentID(Optional<String> documentID) {
    this.documentID = documentID.orElse(null);
  }

  public void setDocumentID(String documentID) {
    this.documentID = documentID;
  }

  private static Optional<Long> getTimestamp(Map<String, Object> document) {
    Object value = document.get(TIMESTAMP.getName());
    if(value != null && value instanceof Long) {
      return Optional.of(Long.class.cast(value));
    }
    return Optional.empty();
  }

  private static String getGUID(Map<String, Object> document) {
    Object value = document.get(GUID);
    if(value != null && value instanceof String) {
      return String.class.cast(value);
    }

    throw new IllegalStateException(String.format("Missing '%s' field", GUID));
  }

  private static String getSensorType(Map<String, Object> document) {
    Object value = document.get(SENSOR_TYPE);
    if(value != null && value instanceof String) {
      return String.class.cast(value);
    }

    value = document.get(SENSOR_TYPE.replace(".", ":"));
    if(value != null && value instanceof String) {
      return String.class.cast(value);
    }

    throw new IllegalStateException(String.format("Missing '%s' field", SENSOR_TYPE));
  }

  public void addComment(AlertComment comment) {
    List<AlertComment> comments = getComments();
    comments.add(comment);
    saveComments(comments);
  }

  public boolean removeComment(AlertComment comment) {
    List<AlertComment> comments = getComments();
    boolean wasRemoved = comments.remove(comment);
    saveComments(comments);
    return wasRemoved;
  }

  private void saveComments(List<AlertComment> comments) {
    // need to persist comments as JSON
    List<String> commentsAsJson = comments
            .stream()
            .map(AlertComment::asJson)
            .collect(Collectors.toList());
    if(commentsAsJson.size() > 0) {
      // overwrite the comments field
      document.put(COMMENTS_FIELD, commentsAsJson);

    } else {
      // there are no longer any comments
      document.remove(COMMENTS_FIELD);
    }
  }

  public List<AlertComment> getComments() {
    List<AlertComment> alertComments = new ArrayList<>();
    List<Object> comments = (List<Object>) document.getOrDefault(COMMENTS_FIELD, new ArrayList<>());
    for (Object commentObj: new ArrayList<>(comments)) {
      AlertComment comment;
      if(commentObj instanceof Map) {
        comment = new AlertComment((Map<String, Object>) commentObj);
      } else if(commentObj instanceof  String) {
        comment = new AlertComment((String) commentObj);
      } else {
        throw new IllegalStateException("Unexpected comment; got " + commentObj);
      }
      alertComments.add(comment);
    }

    return alertComments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Document)) return false;
    Document document1 = (Document) o;
    return Objects.equals(timestamp, document1.timestamp) &&
            Objects.equals(document, document1.document) &&
            Objects.equals(guid, document1.guid) &&
            Objects.equals(sensorType, document1.sensorType) &&
            Objects.equals(documentID, document1.documentID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, document, guid, sensorType, documentID);
  }

  @Override
  public String toString() {
    return "Document{" +
            "timestamp=" + timestamp +
            ", document=" + document +
            ", guid='" + guid + '\'' +
            ", sensorType='" + sensorType + '\'' +
            ", documentID=" + documentID +
            '}';
  }
}
