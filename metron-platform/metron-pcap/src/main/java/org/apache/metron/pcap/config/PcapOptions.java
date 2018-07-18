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

import java.util.function.BiFunction;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.configuration.ConfigOption;

public enum PcapOptions implements ConfigOption {
  JOB_NAME("jobName"),
  FINAL_FILENAME_PREFIX("finalFilenamePrefix"),
  BASE_PATH("basePath", (s, o) -> o == null ? null : new Path(o.toString())),
  INTERIM_RESULT_PATH("interimResultPath", (s, o) -> o == null ? null : new Path(o.toString())),
  BASE_INTERIM_RESULT_PATH("baseInterimResultPath", (s, o) -> o == null ? null : new Path(o.toString())),
  FINAL_OUTPUT_PATH("finalOutputPath", (s, o) -> o == null ? null : new Path(o.toString())),
  NUM_REDUCERS("numReducers"),
  START_TIME_MS("startTimeMs"),
  END_TIME_MS("endTimeMs"),
  START_TIME_NS("startTimeNs"),
  END_TIME_NS("endTimeNs"),
  NUM_RECORDS_PER_FILE("numRecordsPerFile"),
  FIELDS("fields"),
  FILTER_IMPL("filterImpl"),
  HADOOP_CONF("hadoopConf"),
  FILESYSTEM("fileSystem");

  public static final BiFunction<String, Object, Path> STRING_TO_PATH =
      (s, o) -> o == null ? null : new Path(o.toString());
  private String key;
  private BiFunction<String, Object, Object> transform = (s, o) -> o;

  PcapOptions(String key) {
    this.key = key;
  }

  PcapOptions(String key, BiFunction<String, Object, Object> transform) {
    this.key = key;
    this.transform = transform;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public BiFunction<String, Object, Object> transform() {
    return transform;
  }
}
