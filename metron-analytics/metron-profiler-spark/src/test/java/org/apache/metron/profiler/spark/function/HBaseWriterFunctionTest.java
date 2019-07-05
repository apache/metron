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
import org.apache.metron.hbase.ColumnList;
import org.apache.metron.hbase.client.FakeHBaseClient;
import org.apache.metron.hbase.client.HBaseClientFactory;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
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

import static org.apache.metron.hbase.client.FakeHBaseClient.Mutation;

public class HBaseWriterFunctionTest {

  private HBaseWriterFunction function;
  private Properties profilerProperties;
  private RowKeyBuilder rowKeyBuilder;
  private ColumnBuilder columnBuilder;
  private FakeHBaseClient hbaseClient;
  private HBaseClientFactory hBaseClientFactory;

  private static final JSONObject message = getMessage();
  private static final String entity = (String) message.get("ip_src_addr");
  private static final long timestamp = (Long) message.get("timestamp");
  private static final ProfileConfig profile = getProfile();

  @Before
  public void setup() {
    profilerProperties = getProfilerProperties();
    hbaseClient = new FakeHBaseClient();
    hbaseClient.deleteAll();
    hBaseClientFactory = (x, y, z) -> hbaseClient;
    rowKeyBuilder = new SaltyRowKeyBuilder();
    columnBuilder = new ValueOnlyColumnBuilder();
    function = new HBaseWriterFunction(profilerProperties)
            .withRowKeyBuilder(rowKeyBuilder)
            .withColumnBuilder(columnBuilder)
            .withClientFactory(hBaseClientFactory);
  }

  @Test
  public void testWrite() throws Exception {
    // write a profile measurement
    List<ProfileMeasurementAdapter> measurements = createMeasurements(1, entity, timestamp, profile);
    Iterator<Integer> results = function.call(measurements.iterator());

    // validate the results
    List<Integer> counts = IteratorUtils.toList(results);
    Assert.assertEquals(1, counts.size());
    Assert.assertEquals(1, counts.get(0).intValue());

    // 1 record should have been written to the hbase client
    List<Mutation> written = hbaseClient.getAllPersisted();
    Assert.assertEquals(1, written.size());

    // validate the row key used to write to hbase
    ProfileMeasurement m = measurements.get(0).toProfileMeasurement();
    byte[] expectedRowKey = rowKeyBuilder.rowKey(m);
    Assert.assertArrayEquals(expectedRowKey, written.get(0).rowKey);

    // validate the columns used to write to hbase.
    ColumnList expectedCols = columnBuilder.columns(m);
    Assert.assertEquals(expectedCols, written.get(0).columnList);
  }

  @Test
  public void testWriteMany() throws Exception {
    // setup the profile measurements that will be written
    List<ProfileMeasurementAdapter> measurements = createMeasurements(10, entity, timestamp, profile);

    // setup the function to test
    HBaseWriterFunction function = new HBaseWriterFunction(profilerProperties);
    function.withClientFactory(hBaseClientFactory);

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
    function.withClientFactory(hBaseClientFactory);

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
  private static JSONObject getMessage() {
    JSONObject message = new JSONObject();
    message.put("ip_src_addr", "192.168.1.1");
    message.put("status", "red");
    message.put("timestamp", System.currentTimeMillis());
    return message;
  }

  /**
   * Returns profiler properties to use for testing.
   */
  private static Properties getProfilerProperties() {
    return new Properties();
  }

  /**
   * Returns a profile definition to use for testing.
   */
  private static ProfileConfig getProfile() {
    return new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");

  }
}
