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
package org.apache.metron.profiler.spark.function;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.spark.ProfileMeasurementAdapter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_COLUMN_FAMILY;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.HBASE_TABLE_NAME;

public class HBaseWriterFunctionTest {

  Properties profilerProperties;

  @Before
  public void setup() {
    profilerProperties = getProfilerProperties();

    // create a mock table for HBase
    String tableName = HBASE_TABLE_NAME.get(profilerProperties, String.class);
    String columnFamily = HBASE_COLUMN_FAMILY.get(profilerProperties, String.class);
    MockHBaseTableProvider.addToCache(tableName, columnFamily);
  }

  @Test
  public void testWrite() throws Exception {

    JSONObject message = getMessage();
    String entity = (String) message.get("ip_src_addr");
    long timestamp = (Long) message.get("timestamp");
    ProfileConfig profile = getProfile();

    // setup the profile measurements that will be written
    List<ProfileMeasurementAdapter> measurements = createMeasurements(1, entity, timestamp, profile);

    // setup the function to test
    HBaseWriterFunction function = new HBaseWriterFunction(profilerProperties);
    function.withTableProviderImpl(MockHBaseTableProvider.class.getName());

    // write the measurements
    Iterator<Integer> results = function.call(measurements.iterator());

    // validate the result
    List<Integer> counts = IteratorUtils.toList(results);
    Assert.assertEquals(1, counts.size());
    Assert.assertEquals(1, counts.get(0).intValue());
  }

  @Test
  public void testWriteMany() throws Exception {

    JSONObject message = getMessage();
    String entity = (String) message.get("ip_src_addr");
    long timestamp = (Long) message.get("timestamp");
    ProfileConfig profile = getProfile();

    // setup the profile measurements that will be written
    List<ProfileMeasurementAdapter> measurements = createMeasurements(10, entity, timestamp, profile);

    // setup the function to test
    HBaseWriterFunction function = new HBaseWriterFunction(profilerProperties);
    function.withTableProviderImpl(MockHBaseTableProvider.class.getName());

    // write the measurements
    Iterator<Integer> results = function.call(measurements.iterator());

    // validate the result
    List<Integer> counts = IteratorUtils.toList(results);
    Assert.assertEquals(1, counts.size());
    Assert.assertEquals(10, counts.get(0).intValue());
  }

  @Test
  public void testWriteNone() throws Exception {

    // there are no profile measurements to write
    List<ProfileMeasurementAdapter> measurements = new ArrayList<>();

    // setup the function to test
    HBaseWriterFunction function = new HBaseWriterFunction(profilerProperties);
    function.withTableProviderImpl(MockHBaseTableProvider.class.getName());

    // write the measurements
    Iterator<Integer> results = function.call(measurements.iterator());

    // validate the result
    List<Integer> counts = IteratorUtils.toList(results);
    Assert.assertEquals(1, counts.size());
    Assert.assertEquals(0, counts.get(0).intValue());
  }

  /**
   * Create a list of measurements for testing.
   *
   * @param count The number of messages to create.
   * @param entity The entity.
   * @param timestamp The timestamp.
   * @param profile The profile definition.
   * @return
   */
  private List<ProfileMeasurementAdapter> createMeasurements(int count, String entity, long timestamp, ProfileConfig profile) {
    List<ProfileMeasurementAdapter> measurements = new ArrayList<>();

    for(int i=0; i<count; i++) {
      ProfileMeasurement measurement = new ProfileMeasurement()
              .withProfileName(profile.getProfile())
              .withEntity(entity)
              .withPeriod(timestamp, 15, TimeUnit.MINUTES);

      // wrap the measurement using the adapter
      measurements.add(new ProfileMeasurementAdapter(measurement));
    }

    return measurements;
  }

  /**
   * Returns a telemetry message to use for testing.
   */
  private JSONObject getMessage() {
    JSONObject message = new JSONObject();
    message.put("ip_src_addr", "192.168.1.1");
    message.put("status", "red");
    message.put("timestamp", System.currentTimeMillis());
    return message;
  }

  /**
   * Returns profiler properties to use for testing.
   */
  private Properties getProfilerProperties() {
    return new Properties();
  }

  /**
   * Returns a profile definition to use for testing.
   */
  private ProfileConfig getProfile() {
    return new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");

  }
}
