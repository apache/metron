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
package org.apache.metron.rest.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.joda.time.DateTime;

public class SensorParserConfigHistory {

  public SensorParserConfig config;
  private String createdBy;
  private String modifiedBy;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private DateTime createdDate;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private DateTime modifiedByDate;

  public SensorParserConfig getConfig() {
    return config;
  }

  public void setConfig(SensorParserConfig config) {
    this.config = config;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public DateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(DateTime createdDate) {
    this.createdDate = createdDate;
  }

  public DateTime getModifiedByDate() {
    return modifiedByDate;
  }

  public void setModifiedByDate(DateTime modifiedByDate) {
    this.modifiedByDate = modifiedByDate;
  }
}


