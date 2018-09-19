/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.profiler.spark;

import com.google.common.collect.Maps;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.spark.function.GroupByPeriodFunction;
import org.apache.metron.profiler.spark.function.HBaseWriterFunction;
import org.apache.metron.profiler.spark.function.MessageRouterFunction;
import org.apache.metron.profiler.spark.function.ProfileBuilderFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.apache.spark.sql.functions.sum;

/**
 * The 'Batch Profiler' that generates profiles by consuming data in batch from archived telemetry.
 *
 * <p>The Batch Profiler is executed in Spark.
 */
public class BatchProfiler implements Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Execute the Batch Profiler.
   *
   * @param spark The spark session.
   * @param profilerProps The profiler configuration properties.
   * @param globalProperties The Stellar global properties.
   * @param readerProps The properties passed to the {@link org.apache.spark.sql.DataFrameReader}.
   * @param profiles The profile definitions.
   * @return The number of profile measurements produced.
   */
  public long run(SparkSession spark,
                  Properties profilerProps,
                  Properties globalProperties,
                  Properties readerProps,
                  ProfilerConfig profiles) {

    LOG.debug("Building {} profile(s)", profiles.getProfiles().size());
    Map<String, String> globals = Maps.fromProperties(globalProperties);

    String inputFormat = TELEMETRY_INPUT_FORMAT.get(profilerProps, String.class);
    String inputPath = TELEMETRY_INPUT_PATH.get(profilerProps, String.class);
    LOG.debug("Loading telemetry from '{}'", inputPath);

    // fetch the archived telemetry
    Dataset<String> telemetry = spark
            .read()
            .options(Maps.fromProperties(readerProps))
            .format(inputFormat)
            .load(inputPath)
            .as(Encoders.STRING());
    LOG.debug("Found {} telemetry record(s)", telemetry.cache().count());

    // find all routes for each message
    Dataset<MessageRoute> routes = telemetry
            .flatMap(new MessageRouterFunction(profiles, globals), Encoders.bean(MessageRoute.class));
    LOG.debug("Generated {} message route(s)", routes.cache().count());

    // build the profiles
    Dataset<ProfileMeasurementAdapter> measurements = routes
            .groupByKey(new GroupByPeriodFunction(profilerProps), Encoders.STRING())
            .mapGroups(new ProfileBuilderFunction(profilerProps, globals), Encoders.bean(ProfileMeasurementAdapter.class));
    LOG.debug("Produced {} profile measurement(s)", measurements.cache().count());

    // write the profile measurements to HBase
    long count = measurements
            .mapPartitions(new HBaseWriterFunction(profilerProps), Encoders.INT())
            .agg(sum("value"))
            .head()
            .getLong(0);
    LOG.debug("{} profile measurement(s) written to HBase", count);

    return count;
  }
}
