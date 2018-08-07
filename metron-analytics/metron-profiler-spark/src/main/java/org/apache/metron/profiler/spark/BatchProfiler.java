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
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.spark.function.HBaseWriterFunction;
import org.apache.metron.profiler.spark.function.MessageRouterFunction;
import org.apache.metron.profiler.spark.function.ProfileBuilderFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.KeyValueGroupedDataset;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION_UNITS;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_FORMAT;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.TELEMETRY_INPUT_PATH;
import static org.apache.spark.sql.functions.sum;

/**
 * A Profiler that generates profiles by consuming data in batch from archived telemetry.
 *
 * <p>The Batch Profiler is executed in Spark.
 */
public class BatchProfiler implements Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Execute the Batch Profiler.
   *
   * @param spark The spark session.
   * @param properties The profiler configuration properties.
   * @param profiles The profile definitions.
   * @return The number of profile measurements produced.
   */
  public long execute(SparkSession spark,
                      Properties properties,
                      Properties globalProperties,
                      ProfilerConfig profiles) {
    LOG.debug("Building {} profile(s)", profiles.getProfiles().size());

    Map<String, String> globals = Maps.fromProperties(globalProperties);

    // fetch the archived telemetry
    Dataset<String> telemetry = readTelemetry(spark, properties);

    // find all routes for each message
    Dataset<MessageRoute> routes = findRoutes(telemetry, profiles, globals);

    // group the routes
    KeyValueGroupedDataset<String, MessageRoute> groupedRoutes = groupRoutes(routes, properties);

    // build the profiles
    Dataset<ProfileMeasurementAdapter> measurements = buildProfiles(groupedRoutes, properties, globals);

    // write the profile measurements to HBase
    long count = write(measurements, properties);
    return count;
  }

  /**
   * Read in the archived telemetry.
   *
   * @param spark The Spark session.
   * @param properties The Profiler configuration properties.
   * @return The telemetry that the Profiler will consume.
   */
  private Dataset<String> readTelemetry(SparkSession spark, Properties properties) {
    String inputFormat = TELEMETRY_INPUT_FORMAT.get(properties, String.class);
    String inputPath = TELEMETRY_INPUT_PATH.get(properties, String.class);

    LOG.debug("Loading telemetry from '{}'", inputPath);
    Dataset<String> telemetry = spark
            .read()
            .format(inputFormat)
            .load(inputPath)
            .as(Encoders.STRING());

    LOG.debug("Found {} telemetry record(s)", telemetry.cache().count());
    return telemetry;
  }

  /**
   * Finds all routes for the given telemetry.
   *
   * @param telemetry The input telemetry messages.
   * @param profilerConfig The profile definitions.
   * @param globals The global configuration for Stellar execution.
   * @return The message routes.
   */
  private Dataset<MessageRoute> findRoutes(Dataset<String> telemetry,
                                           ProfilerConfig profilerConfig,
                                           Map<String, String> globals) {
    Dataset<MessageRoute> routes = telemetry
            .flatMap(new MessageRouterFunction(profilerConfig, globals), Encoders.bean(MessageRoute.class));

    LOG.debug("Generated {} message route(s)", routes.cache().count());
    return routes;
  }

  /**
   * Groups routes by {profile, entity, period}. This ensures that all of the required information
   * is aggregated so that profiles can be build.
   *
   * @param routes The message routes to group.
   * @param properties The Profiler configuration properties.
   * @return The group message routes.
   */
  private KeyValueGroupedDataset<String, MessageRoute> groupRoutes(Dataset<MessageRoute> routes,
                                                                   Properties properties) {
    TimeUnit periodDurationUnits = TimeUnit.valueOf(PERIOD_DURATION_UNITS.get(properties, String.class));
    int periodDuration = PERIOD_DURATION.get(properties, Integer.class);

    MapFunction<MessageRoute, String> groupByPeriodFn;
    groupByPeriodFn = route -> {
      ProfilePeriod period = ProfilePeriod.fromTimestamp(route.getTimestamp(), periodDuration, periodDurationUnits);
      return route.getProfileDefinition().getProfile() + "-" + route.getEntity() + "-" + period.getPeriod();
    };

    return routes.groupByKey(groupByPeriodFn, Encoders.STRING());
  }

  /**
   * Builds a profile.
   *
   * @param groupedRoutes All of the routes required to build a single profile.
   * @param properties The Profiler configuration properties.
   * @param globals The global configuration for Stellar execution.
   * @return
   */
  private Dataset<ProfileMeasurementAdapter> buildProfiles(KeyValueGroupedDataset<String, MessageRoute> groupedRoutes,
                                                           Properties properties,
                                                           Map<String, String> globals) {
    Dataset<ProfileMeasurementAdapter> measurements = groupedRoutes
            .mapGroups(new ProfileBuilderFunction(properties, globals), Encoders.bean(ProfileMeasurementAdapter.class));

    LOG.debug("Produced {} profile measurement(s)", measurements.cache().count());
    return measurements;
  }

  /**
   * Writes out the profile measurements.
   *
   * @param measurements The measurements to write.
   * @param properties The Profiler configuration properties.
   * @return The number of profile measurements written.
   */
  private long write(Dataset<ProfileMeasurementAdapter> measurements, Properties properties) {
    long count = measurements
            .mapPartitions(new HBaseWriterFunction(properties), Encoders.INT())
            .agg(sum("value"))
            .head()
            .getLong(0);

    LOG.debug("{} profile measurement(s) written to HBase", count);
    return count;
  }
}