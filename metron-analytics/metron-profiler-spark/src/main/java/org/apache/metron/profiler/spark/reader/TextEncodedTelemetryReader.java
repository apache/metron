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
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;

/**
 * A {@link TelemetryReader} that consumes telemetry stored as raw text.
 *
 * <p>This {@link TelemetryReader} is useful for any text-encoded formats like JSON and CSV.
 */
public class TextEncodedTelemetryReader implements TelemetryReader {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The input format to use when reading telemetry.
   */
  private String inputFormat;

  /**
   * Creates a {@link TextEncodedTelemetryReader}.
   *
   * <p>The input format used to read the telemetry is defined by the
   * BatchProfilerConfig.TELEMETRY_INPUT_PATH property.
   */
  public TextEncodedTelemetryReader() {
    this.inputFormat = null;
  }

  /**
   * Creates a {@link TextEncodedTelemetryReader}.
   *
   * @param inputFormat The input format to use when reading telemetry.
   */
  public TextEncodedTelemetryReader(String inputFormat) {
    this.inputFormat = inputFormat;
  }

  @Override
  public Dataset<String> read(SparkSession spark, Properties profilerProps, Properties readerProps) {
    String inputPath = TELEMETRY_INPUT_PATH.get(profilerProps, String.class);
    if(inputFormat == null) {
      inputFormat = TELEMETRY_INPUT_FORMAT.get(profilerProps, String.class);
    }
    LOG.debug("Loading telemetry; inputPath={}, inputFormat={}", inputPath, inputFormat);

    return spark
            .read()
            .options(Maps.fromProperties(readerProps))
            .format(inputFormat)
            .load(inputPath)
            .as(Encoders.STRING());
  }
}
