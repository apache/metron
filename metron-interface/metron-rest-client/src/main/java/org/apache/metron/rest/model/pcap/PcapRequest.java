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
package org.apache.metron.rest.model.pcap;

public class PcapRequest {

  private String baseOutputPath;
  private String basePath;
  private Long startTime = 0L;
  private Long endTime = System.currentTimeMillis();
  private Integer numReducers = 1;

  public String getBaseOutputPath() {
    return baseOutputPath;
  }

  public void setBaseOutputPath(String baseOutputPath) {
    this.baseOutputPath = baseOutputPath;
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Integer getNumReducers() {
    return numReducers;
  }

  public void setNumReducers(Integer numReducers) {
    this.numReducers = numReducers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PcapRequest pcapRequest = (PcapRequest) o;

    return (getBaseOutputPath() != null ? getBaseOutputPath().equals(pcapRequest.getBaseOutputPath()) : pcapRequest.getBaseOutputPath() != null) &&
            (getBasePath() != null ? getBasePath().equals(pcapRequest.getBasePath()) : pcapRequest.getBasePath() == null) &&
            (getStartTime() != null ? getStartTime().equals(pcapRequest.getStartTime()) : pcapRequest.getStartTime() == null) &&
            (getEndTime() != null ? getEndTime().equals(pcapRequest.getEndTime()) : pcapRequest.getEndTime() == null) &&
            (getNumReducers() != null ? getNumReducers().equals(pcapRequest.getNumReducers()) : pcapRequest.getNumReducers() == null);
  }

  @Override
  public int hashCode() {
    int result = getBaseOutputPath() != null ? getBaseOutputPath().hashCode() : 0;
    result = 31 * result + (getBasePath() != null ? getBasePath().hashCode() : 0);
    result = 31 * result + (getStartTime() != null ? getStartTime().hashCode() : 0);
    result = 31 * result + (getEndTime() != null ? getEndTime().hashCode() : 0);
    result = 31 * result + (getNumReducers() != null ? getNumReducers().hashCode() : 0);
    return result;
  }
}
