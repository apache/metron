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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class CliConfig {
  public static final String BASE_PATH_DEFAULT = "/apps/metron/pcap";
  public static final String BASE_OUTPUT_PATH_DEFAULT = "/tmp";
  private boolean showHelp;
  private String basePath;
  private String baseOutputPath;
  private long startTime;
  private long endTime;
  private DateFormat dateFormat;

  public CliConfig() {
    showHelp = false;
    basePath = BASE_PATH_DEFAULT;
    baseOutputPath = BASE_OUTPUT_PATH_DEFAULT;
    startTime = -1L;
    endTime = -1L;
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
}
