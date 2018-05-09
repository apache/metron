/*
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

import com.fasterxml.jackson.annotation.JsonGetter;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.indexing.dao.search.AlertComment;

public class CommentAddRemoveRequest {
  private String guid;
  private String sensorType;
  private String comment;
  private String username;
  private long timestamp;
  private String index;

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public Optional<String> getIndex() {
    return index != null ? Optional.of(this.index) : Optional.empty();
  }

  public void setIndex(String index) {
    this.index = index;
  }

  @JsonGetter("index")
  public String getIndexString() {
    return index;
  }

  @Override
  public String toString() {
    return "CommentAddRemoveRequest{" +
        "guid='" + guid + '\'' +
        ", sensorType='" + sensorType + '\'' +
        ", comment='" + comment + '\'' +
        ", username='" + username + '\'' +
        ", timestamp=" + timestamp +
        ", index='" + index + '\'' +
        '}';
  }
}
