/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.profiler.spark.reader;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;

import java.io.Serializable;
import java.util.Properties;

/**
 * A {@link TelemetryReader} is responsible for creating a {@link Dataset} containing
 * telemetry that can be consumed by the {@link org.apache.metron.profiler.spark.BatchProfiler}.
 */
public interface TelemetryReader extends Serializable {

  /**
   * Read in the telemetry
   *
   * @param spark The spark session.
   * @param profilerProps The profiler properties.
   * @param readerProps The properties specific to reading input data.
   * @return A {@link Dataset} containing archived telemetry.
   */
  Dataset<String> read(SparkSession spark, Properties profilerProps, Properties readerProps);
}
