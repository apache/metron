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

import com.google.common.collect.Maps;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;

/**
 * Reads in a {@link Dataset} then converts all of the {@link Dataset}'s column
 * into a single JSON-formatted string.
 *
 * <p>This {@link TelemetryReader} is useful for any column-oriented format that
 * is supported by Spark.  For example, ORC and Parquet.
 */
public class ColumnEncodedTelemetryReader implements TelemetryReader {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Dataset<String> read(SparkSession spark, Properties properties) {
    String inputFormat = TELEMETRY_INPUT_FORMAT.get(properties, String.class);
    String inputPath = TELEMETRY_INPUT_PATH.get(properties, String.class);
    LOG.debug("Loading telemetry; inputPath={}, inputFormat={}", inputPath, inputFormat);

    return spark
            .read()
            .options(Maps.fromProperties(properties))
            .format(inputFormat)
            .load(inputPath)
            .toJSON();
  }
}
