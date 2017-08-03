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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class ReplaceRequest {
  Map<String, Object> replacement;
  String guid;
  String sensorType;
  String index;

  /**
   * Return the index of the request.  This is optional, but could result in better performance if specified.
   * @return
   */
  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * The sensor type of the request. This is mandatory.
   * @return
   */
  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  /**
   * The replacement document.  This is mandatory.
   * @return
   */
  public Map<String, Object> getReplacement() {
    return replacement;
  }

  public void setReplacement(Map<String, Object> replacement) {
    this.replacement = replacement;
  }

  /**
   * The GUID of the document to replace.  This is mandatory.
   * @return
   */
  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }
}
