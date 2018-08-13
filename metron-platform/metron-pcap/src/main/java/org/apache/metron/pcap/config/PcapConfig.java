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
package org.apache.metron.pcap.config;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.system.Clock;
import org.apache.metron.common.configuration.ConfigOption;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

public class PcapConfig extends AbstractMapDecorator<String, Object>{
  public interface PrefixStrategy extends Function<Clock, String>{}

  private boolean showHelp;
  private DateFormat dateFormat;
  private String yarnQueue;

  public PcapConfig() {
    super(new HashMap<>());
  }

  public PcapConfig(PrefixStrategy prefixStrategy) {
    this();
    setShowHelp(false);
    setPrintJobStatus(false);
    setBasePath("");
    setBaseInterimResultPath("");
    setStartTimeMs(-1L);
    setEndTimeMs(-1L);
    setNumReducers(0);
    setFinalFilenamePrefix(prefixStrategy.apply(new Clock()));
  }

  public Object getOption(ConfigOption option) {
    Object o = get(option.getKey());
    return option.transform().apply(option.getKey(), o);
  }

  public String getFinalFilenamePrefix() {
    return PcapOptions.FINAL_FILENAME_PREFIX.get(this, String.class);
  }

  public void setFinalFilenamePrefix(String prefix) {
    PcapOptions.FINAL_FILENAME_PREFIX.put(this, prefix);
  }

  public int getNumReducers() {
    return PcapOptions.NUM_REDUCERS.get(this, Integer.class);
  }

  public boolean showHelp() {
    return showHelp;
  }

  public void setShowHelp(boolean showHelp) {
    this.showHelp = showHelp;
  }

  public boolean printJobStatus() {
    return PcapOptions.PRINT_JOB_STATUS.get(this, Boolean.class);
  }

  public void setPrintJobStatus(boolean printJobStatus) {
    PcapOptions.PRINT_JOB_STATUS.put(this, printJobStatus);
  }

  public String getBasePath() {
    return PcapOptions.BASE_PATH.get(this, String.class);
  }

  public String getBaseInterimResultPath() {
    return PcapOptions.BASE_INTERIM_RESULT_PATH.get(this, String.class);
  }

  public long getStartTimeMs() {
    return PcapOptions.START_TIME_MS.get(this, Long.class);
  }

  public long getEndTimeMs() {
    return PcapOptions.END_TIME_MS.get(this, Long.class);
  }

  public void setBasePath(String basePath) {
    PcapOptions.BASE_PATH.put(this, basePath);
  }

  public void setBaseInterimResultPath(String baseOutputPath) {
    PcapOptions.BASE_INTERIM_RESULT_PATH.put(this, baseOutputPath);
  }

  public void setStartTimeMs(long startTime) {
    PcapOptions.START_TIME_MS.put(this, startTime);
  }

  public void setEndTimeMs(long endTime) {
    PcapOptions.END_TIME_MS.put(this, endTime);
  }

  public boolean isNullOrEmpty(String val) {
    return StringUtils.isEmpty(val);
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = new SimpleDateFormat(dateFormat);
  }

  public DateFormat getDateFormat() {
    return dateFormat;
  }

  public void setNumReducers(int numReducers) {
    PcapOptions.NUM_REDUCERS.put(this, numReducers);
  }

  public int getNumRecordsPerFile() {
    return PcapOptions.NUM_RECORDS_PER_FILE.get(this, Integer.class);
  }

  public void setNumRecordsPerFile(int numRecordsPerFile) {
    PcapOptions.NUM_RECORDS_PER_FILE.put(this, numRecordsPerFile);
  }

  public void setYarnQueue(String yarnQueue) {
    this.yarnQueue = yarnQueue;
  }

  public Optional<String> getYarnQueue() {
    return Optional.ofNullable(yarnQueue);
  }
}
