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

package org.apache.metron.indexing.dao.search;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AlertComment {

  private static final String COMMENT_FIELD = "comment";
  private static final String COMMENT_USERNAME_FIELD = "username";
  private static final String COMMENT_TIMESTAMP_FIELD = "timestamp";
  private String comment;
  private String username;
  private long timestamp;

  private JSONParser parser = new JSONParser();

  public AlertComment(String comment, String username, long timestamp) {
    this.comment = comment;
    this.username = username;
    this.timestamp = timestamp;
  }

  public AlertComment(String json) throws ParseException {
    JSONObject parsed = (JSONObject) parser.parse(json);
    this.comment = (String) parsed.get(COMMENT_FIELD);
    this.username = (String) parsed.get(COMMENT_USERNAME_FIELD);
    this.timestamp = (long) parsed.get(COMMENT_TIMESTAMP_FIELD);
  }

  public AlertComment(Map<String, Object> comment) {
    this.comment = (String) comment.get(COMMENT_FIELD);
    this.username = (String) comment.get(COMMENT_USERNAME_FIELD);
    this.timestamp = (long) comment.get(COMMENT_TIMESTAMP_FIELD);
  }

  public String getComment() {
    return comment;
  }

  public String getUsername() {
    return username;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @SuppressWarnings("unchecked")
  public String asJson() {
    return asJSONObject().toJSONString();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> asMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(COMMENT_FIELD, comment);
    map.put(COMMENT_USERNAME_FIELD, username);
    map.put(COMMENT_TIMESTAMP_FIELD, timestamp);
    return map;
  }

  @SuppressWarnings("unchecked")
  public JSONObject asJSONObject() {
    JSONObject json = new JSONObject();
    json.put(COMMENT_FIELD, comment);
    json.put(COMMENT_USERNAME_FIELD, username);
    json.put(COMMENT_TIMESTAMP_FIELD, timestamp);
    return json;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AlertComment that = (AlertComment) o;

    if (getTimestamp() != that.getTimestamp()) {
      return false;
    }
    if (getComment() != null ? !getComment().equals(that.getComment())
        : that.getComment() != null) {
      return false;
    }
    return getUsername() != null ? getUsername().equals(that.getUsername())
        : that.getUsername() == null;
  }

  @Override
  public int hashCode() {
    int result = getComment() != null ? getComment().hashCode() : 0;
    result = 31 * result + (getUsername() != null ? getUsername().hashCode() : 0);
    result = 31 * result + (int) (getTimestamp() ^ (getTimestamp() >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "AlertComment{" +
        "comment='" + comment + '\'' +
        ", username='" + username + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
