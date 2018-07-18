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

// TODO reconcile with pcapmrjob

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.metron.pcap.config.PcapOptions;

public class PcapRequest extends AbstractMapDecorator<String, Object> {

  public PcapRequest() {
    setStartTime(0L);
    setEndTime(System.currentTimeMillis());
    setNumReducers(1);
  }

  public String getBaseOutputPath() {
    return PcapOptions.BASE_INTERIM_RESULT_PATH.get(this, String.class);
  }

  public void setBaseOutputPath(String baseOutputPath) {
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(this, baseOutputPath);
  }

  public String getBasePath() {
    return PcapOptions.BASE_PATH.get(this, String.class);
  }

  public void setBasePath(String basePath) {
    PcapOptions.BASE_PATH.put(this, basePath);
  }

  public Long getStartTime() {
    return PcapOptions.START_TIME_MS.get(this, Long.class);
  }

  public void setStartTime(Long startTime) {
    PcapOptions.START_TIME_MS.put(this, startTime);
  }

  public Long getEndTime() {
    return PcapOptions.END_TIME_MS.get(this, Long.class);
  }

  public void setEndTime(Long endTime) {
    PcapOptions.END_TIME_MS.put(this, endTime);
  }

  public Integer getNumReducers() {
    return PcapOptions.NUM_REDUCERS.get(this, Integer.class);
  }

  public void setNumReducers(Integer numReducers) {
    PcapOptions.NUM_REDUCERS.put(this, numReducers);
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
