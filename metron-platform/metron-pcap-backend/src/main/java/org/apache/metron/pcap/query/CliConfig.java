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
package org.apache.metron.pcap.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.system.Clock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class CliConfig {
  public interface PrefixStrategy extends Function<Clock, String>{}

  private boolean showHelp;
  private String prefix;
  private String basePath;
  private String baseOutputPath;
  private long startTime;
  private long endTime;
  private int numReducers;
  private int numRecordsPerFile;
  private DateFormat dateFormat;


  public CliConfig(PrefixStrategy prefixStrategy) {
    showHelp = false;
    basePath = "";
    baseOutputPath = "";
    startTime = -1L;
    endTime = -1L;
    numReducers = 0;
    prefix = prefixStrategy.apply(new Clock());
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public int getNumReducers() {
    return numReducers;
  }

  public boolean showHelp() {
    return showHelp;
  }

  public void setShowHelp(boolean showHelp) {
    this.showHelp = showHelp;
  }

  public String getBasePath() {
    return basePath;
  }

  public String getBaseOutputPath() {
    return baseOutputPath;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void setBaseOutputPath(String baseOutputPath) {
    this.baseOutputPath = baseOutputPath;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
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
    this.numReducers = numReducers;
  }

  public int getNumRecordsPerFile() {
    return numRecordsPerFile;
  }

  public void setNumRecordsPerFile(int numRecordsPerFile) {
    this.numRecordsPerFile = numRecordsPerFile;
  }
}
