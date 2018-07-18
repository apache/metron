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

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.metron.pcap.config.PcapOptions;

import java.util.HashMap;

public class PcapRequest extends AbstractMapDecorator<String, Object> {

  public PcapRequest() {
    super(new HashMap<>());
    setStartTimeMs(0L);
    setEndTimeMs(System.currentTimeMillis());
    setNumReducers(10);
  }

  public String getBasePath() {
    return PcapOptions.BASE_PATH.get(this, String.class);
  }

  public void setBasePath(String basePath) {
    PcapOptions.BASE_PATH.put(this, basePath);
  }

  public String getBaseInterimResultPath() {
    return PcapOptions.BASE_INTERIM_RESULT_PATH.get(this, String.class);
  }

  public void setBaseInterimResultPath(String baseInterimResultPath) {
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(this, baseInterimResultPath);
  }

  public String getFinalOutputPath() {
    return PcapOptions.FINAL_OUTPUT_PATH.get(this, String.class);
  }

  public void setFinalOutputPath(String finalOutputPath) {
    PcapOptions.FINAL_OUTPUT_PATH.put(this, finalOutputPath);
  }

  public Long getStartTimeMs() {
    return PcapOptions.START_TIME_MS.get(this, Long.class);
  }

  public void setStartTimeMs(Long startTime) {
    PcapOptions.START_TIME_MS.put(this, startTime);
  }

  public Long getEndTimeMs() {
    return PcapOptions.END_TIME_MS.get(this, Long.class);
  }

  public void setEndTimeMs(Long endTime) {
    PcapOptions.END_TIME_MS.put(this, endTime);
  }

  public Integer getNumReducers() {
    return PcapOptions.NUM_REDUCERS.get(this, Integer.class);
  }

  public void setNumReducers(Integer numReducers) {
    PcapOptions.NUM_REDUCERS.put(this, numReducers);
  }
}
