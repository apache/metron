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

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.system.Clock;
import org.apache.metron.pcap.ConfigOption;
import org.apache.metron.pcap.ConfigOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.function.Function;

public class CliConfig extends AbstractMapDecorator<String, Object>{
  public interface PrefixStrategy extends Function<Clock, String>{}

  private boolean showHelp;
  private DateFormat dateFormat;

  public CliConfig() {
    super(new HashMap<>());
  }

  public CliConfig(PrefixStrategy prefixStrategy) {
    this();
    setShowHelp(false);
    setBasePath("");
    setBaseOutputPath("");
    setStartTime(-1L);
    setEndTime(-1L);
    setNumReducers(0);
    setPrefix(prefixStrategy.apply(new Clock()));
  }

  public Object getOption(ConfigOption option) {
    Object o = get(option.getKey());
    return option.transform().apply(option.getKey(), o);
  }

  public String getPrefix() {
    return ConfigOptions.PREFIX.get(this, String.class);
  }

  public void setPrefix(String prefix) {
    ConfigOptions.PREFIX.put(this, prefix);
  }

  public int getNumReducers() {
    return ConfigOptions.NUM_REDUCERS.get(this, Integer.class);
  }

  public boolean showHelp() {
    return showHelp;
  }

  public void setShowHelp(boolean showHelp) {
    this.showHelp = showHelp;
  }

  public String getBasePath() {
    return ConfigOptions.BASE_PATH.get(this, String.class);
  }

  public String getBaseOutputPath() {
    return ConfigOptions.INTERRIM_RESULT_PATH.get(this, String.class);
  }

  public long getStartTime() {
    return ConfigOptions.START_TIME.get(this, Long.class);
  }

  public long getEndTime() {
    return ConfigOptions.END_TIME.get(this, Long.class);
  }

  public void setBasePath(String basePath) {
    ConfigOptions.BASE_PATH.put(this, basePath);
  }

  public void setBaseOutputPath(String baseOutputPath) {
    ConfigOptions.INTERRIM_RESULT_PATH.put(this, baseOutputPath);
  }

  public void setStartTime(long startTime) {
    ConfigOptions.START_TIME.put(this, startTime);
  }

  public void setEndTime(long endTime) {
    ConfigOptions.END_TIME.put(this, endTime);
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
    ConfigOptions.NUM_REDUCERS.put(this, numReducers);
  }

  public int getNumRecordsPerFile() {
    return ConfigOptions.NUM_RECORDS_PER_FILE.get(this, Integer.class);
  }

  public void setNumRecordsPerFile(int numRecordsPerFile) {
    ConfigOptions.NUM_RECORDS_PER_FILE.put(this, numRecordsPerFile);
  }
}
