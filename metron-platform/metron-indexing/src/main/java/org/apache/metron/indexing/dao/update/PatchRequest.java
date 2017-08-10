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

public class PatchRequest {
  JsonNode patch;
  Map<String, Object> source;
  String guid;
  String sensorType;
  String index;

  /**
   * The index of the document to be updated.  This is optional, but could result in a performance gain if specified.
   * @return
   */
  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * The patch.  This is in the form of a list of RFC 6902 patches.
   * For example:
   * <pre>
   * [
   *   {
   *             "op": "add"
   *            , "path": "/project"
   *            , "value": "metron"
   *   }
   *           ]
   * </pre>
   * @return
   */
  public JsonNode getPatch() {
    return patch;
  }

  public void setPatch(JsonNode patch) {
    this.patch = patch;
  }

  /**
   * The source document.  If this is specified, then it will be used as the basis of the patch rather than the current
   * document in the index.
   * @return
   */
  public Map<String, Object> getSource() {
    return source;
  }

  public void setSource(Map<String, Object> source) {
    this.source = source;
  }

  /**
   * The GUID of the document to be patched.
   * @return
   */
  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
   * The sensor type of the document.
   * @return
   */
  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }
}
