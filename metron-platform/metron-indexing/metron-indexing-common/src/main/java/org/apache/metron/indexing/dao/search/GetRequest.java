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
package org.apache.metron.indexing.dao.search;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.util.Optional;

public class GetRequest {
  private String guid;
  private String sensorType;
  private String index;

  public GetRequest() {
  }

  public GetRequest(String guid, String sensorType) {
    this.guid = guid;
    this.sensorType = sensorType;
  }

  public GetRequest(String guid, String sensorType, String index) {
    this.guid = guid;
    this.sensorType = sensorType;
    this.index = index;
  }

  /**
   * The GUID of the document
   * @return
   */
  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  /**
   * The sensor type of the indices that you're searching.
   * @return
   */
  public String getSensorType() {
    return sensorType;
  }

  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  public Optional<String> getIndex() {
    return index != null ? Optional.of(this.index) : Optional.empty();
  }

  @JsonGetter("index")
  public String getIndexString() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }
}
