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

package org.apache.metron.profiler.integration;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.RowKeyBuilder;
import org.apache.metron.profiler.hbase.SaltyRowKeyBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * An integration test of the Profiler topology.
 */
public class ProfilerIntegrationTest extends BaseIntegrationTest {

  private static final String TEST_RESOURCES = "../../metron-analytics/metron-profiler/src/test";
  private static final String FLUX_PATH = "../metron-profiler/src/main/flux/profiler/remote.yaml";

  public static final long startAt = 10;
  public static final String entity = "10.0.0.1";

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private static final String inputTopic = Constants.INDEXING_TOPIC;
  private static final String outputTopic = "profiles";
  private static final int saltDivisor = 10;

  private static final long windowLagMillis = TimeUnit.SECONDS.toMillis(1);
  private static final long windowDurationMillis = TimeUnit.SECONDS.toMillis(5);
  private static final long periodDurationMillis = TimeUnit.SECONDS.toMillis(10);
  private static final long profileTimeToLiveMillis = TimeUnit.SECONDS.toMillis(15);
  private static final long maxRoutesPerBolt = 100000;

  private static ColumnBuilder columnBuilder;
  private static ZKServerComponent zkComponent;
  private static FluxTopologyComponent fluxComponent;
  private static KafkaComponent kafkaComponent;
  private static ConfigUploadComponent configUploadComponent;
  private static ComponentRunner runner;
  private static MockHTable profilerTable;

  private static String message1;
  private static String message2;
  private static String message3;

  /**
   * [
   *    org.apache.metron.profiler.ProfileMeasurement,
   *    org.apache.metron.profiler.ProfilePeriod,
   *    org.apache.metron.common.configuration.profiler.ProfileResult,
   *    org.apache.metron.common.configuration.profiler.ProfileResultExpressions,
   *    org.apache.metron.common.configuration.profiler.ProfileTriageExpressions,
   *    org.apache.metron.common.configuration.profiler.ProfilerConfig,
   *    org.apache.metron.common.configuration.profiler.ProfileConfig,
   *    org.json.simple.JSONObject,
   *    java.util.LinkedHashMap,
   *    org.apache.metron.statistics.OnlineStatisticsProvider
   *  ]
   */
  @Multiline
  private static String kryoSerializers;

  /**
   * The Profiler can generate profiles based on processing time.  With processing time,
   * the Profiler builds profiles based on when the telemetry was processed.
   *
   * <p>Not defining a 'timestampField' within the Profiler configuration tells the Profiler
   * to use processing time.
   */
  @Test
  public void testProcessingTime() throws Exception {

    // upload the config to zookeeper
    uploadConfig(TEST_RESOURCES + "/config/zookeeper/processing-time-test");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();

    // the messages that will be applied to the profile
    kafkaComponent.writeMessages(inputTopic, message1);
    kafkaComponent.writeMessages(inputTopic, message2);
    kafkaComponent.writeMessages(inputTopic, message3);

    // storm needs at least one message to close its event window
    int attempt = 0;
    while(profilerTable.getPutLog().size() == 0 && attempt++ < 10) {

      // sleep, at least beyond the current window
      Thread.sleep(windowDurationMillis + windowLagMillis);

      // send another message to help close the current event window
      kafkaComponent.writeMessages(inputTopic, message2);
    }

    // validate what was flushed
    List<Integer> actuals = read(
            profilerTable.getPutLog(),
            columnFamily,
            columnBuilder.getColumnQualifier("value"),
            Integer.class);
    assertEquals(1, actuals.size());
    assertTrue(actuals.get(0) >= 3);
  }

  /**
   * The Profiler can generate profiles using event time.  With event time processing,
   * the Profiler uses timestamps contained in the source telemetry.
   *
   * <p>Defining a 'timestampField' within the Profiler configuration tells the Profiler
   * from which field the timestamp should be extracted.
   */
  @Test
  public void testEventTime() throws Exception {

    // upload the profiler config to zookeeper
    uploadConfig(TEST_RESOURCES + "/config/zookeeper/event-time-test");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1);
    kafkaComponent.writeMessages(inputTopic, message2);
    kafkaComponent.writeMessages(inputTopic, message3);

    // wait until the profile is flushed
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0, timeout(seconds(90)));

    List<Put> puts = profilerTable.getPutLog();
    assertEquals(1, puts.size());

    // inspect the row key to ensure the profiler used event time correctly.  the timestamp
    // embedded in the row key should match those in the source telemetry
    byte[] expectedRowKey = generateExpectedRowKey("event-time-test", entity, startAt);
    byte[] actualRowKey = puts.get(0).getRow();
    assertArrayEquals(failMessage(expectedRowKey, actualRowKey), expectedRowKey, actualRowKey);
  }

  /**
   * The result produced by a Profile has to be serializable within Storm. If the result is not
   * serializable the topology will crash and burn.
   *
   * This test ensures that if a profile returns a STATS object created using the STATS_INIT and
   * STATS_ADD functions, that it can be correctly serialized and persisted.
   */
  @Test
  public void testProfileWithStatsObject() throws Exception {

    // upload the profiler config to zookeeper
    uploadConfig(TEST_RESOURCES + "/config/zookeeper/profile-with-stats");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1);
    kafkaComponent.writeMessages(inputTopic, message2);
    kafkaComponent.writeMessages(inputTopic, message3);

    // wait until the profile is flushed
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0, timeout(seconds(90)));

    // ensure that a value was persisted in HBase
    List<Put> puts = profilerTable.getPutLog();
    assertEquals(1, puts.size());

    // generate the expected row key. only the profile name, entity, and period are used to generate the row key
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName("profile-with-stats")
            .withEntity("global")
            .withPeriod(startAt, periodDurationMillis, TimeUnit.MILLISECONDS);
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder(saltDivisor, periodDurationMillis, TimeUnit.MILLISECONDS);
    byte[] expectedRowKey = rowKeyBuilder.rowKey(measurement);

    // ensure the correct row key was generated
    byte[] actualRowKey = puts.get(0).getRow();
    assertArrayEquals(failMessage(expectedRowKey, actualRowKey), expectedRowKey, actualRowKey);
  }

  /**
   * Generates an error message for if the byte comparison fails.
   *
   * @param expected The expected value.
   * @param actual The actual value.
   * @return
   * @throws UnsupportedEncodingException
   */
  private String failMessage(byte[] expected, byte[] actual) throws UnsupportedEncodingException {
    return String.format("expected '%s', got '%s'",
              new String(expected, "UTF-8"),
              new String(actual, "UTF-8"));
  }

  /**
   * Generates the expected row key.
   *
   * @param profileName The name of the profile.
   * @param entity The entity.
   * @param whenMillis A timestamp in epoch milliseconds.
   * @return A row key.
   */
  private byte[] generateExpectedRowKey(String profileName, String entity, long whenMillis) {

    // only the profile name, entity, and period are used to generate the row key
    ProfileMeasurement measurement = new ProfileMeasurement()
            .withProfileName(profileName)
            .withEntity(entity)
            .withPeriod(whenMillis, periodDurationMillis, TimeUnit.MILLISECONDS);

    // build the row key
    RowKeyBuilder rowKeyBuilder = new SaltyRowKeyBuilder(saltDivisor, periodDurationMillis, TimeUnit.MILLISECONDS);
    return rowKeyBuilder.rowKey(measurement);
  }

  /**
   * Reads a value written by the Profiler.
   *
   * @param family The column family.
   * @param qualifier The column qualifier.
   * @param clazz The expected type of the value.
   * @param <T> The expected type of the value.
   * @return The value written by the Profiler.
   */
  private <T> List<T> read(List<Put> puts, String family, byte[] qualifier, Class<T> clazz) {
    List<T> results = new ArrayList<>();

    for(Put put: puts) {
      List<Cell> cells = put.get(Bytes.toBytes(family), qualifier);
      for(Cell cell : cells) {
        T value = SerDeUtils.fromBytes(cell.getValue(), clazz);
        results.add(value);
      }
    }

    return results;
  }

  @BeforeClass
  public static void setupBeforeClass() throws UnableToStartException {

    // create some messages that contain a timestamp - a really old timestamp; close to 1970
    message1 = new MessageBuilder()
            .withField("ip_src_addr", entity)
            .withField("timestamp", startAt)
            .build()
            .toJSONString();

    message2 = new MessageBuilder()
            .withField("ip_src_addr", entity)
            .withField("timestamp", startAt + 100)
            .build()
            .toJSONString();

    message3 = new MessageBuilder()
            .withField("ip_src_addr", entity)
            .withField("timestamp", startAt + (windowDurationMillis * 2))
            .build()
            .toJSONString();

    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    // storm topology properties
    final Properties topologyProperties = new Properties() {{

      // storm settings
      setProperty("profiler.workers", "1");
      setProperty("profiler.executors", "0");
      setProperty("storm.auto.credentials", "[]");
      setProperty("topology.auto-credentials", "[]");
      setProperty("topology.message.timeout.secs", "60");
      setProperty("topology.max.spout.pending", "100000");

      // ensure tuples are serialized during the test, otherwise serialization problems
      // will not be found until the topology is run on a cluster with multiple workers
      setProperty("topology.testing.always.try.serialize", "true");
      setProperty("topology.fall.back.on.java.serialization", "false");
      setProperty("topology.kryo.register", kryoSerializers);

      // kafka settings
      setProperty("profiler.input.topic", inputTopic);
      setProperty("profiler.output.topic", outputTopic);
      setProperty("kafka.start", "UNCOMMITTED_EARLIEST");
      setProperty("kafka.security.protocol", "PLAINTEXT");

      // hbase settings
      setProperty("profiler.hbase.salt.divisor", Integer.toString(saltDivisor));
      setProperty("profiler.hbase.table", tableName);
      setProperty("profiler.hbase.column.family", columnFamily);
      setProperty("profiler.hbase.batch", "10");
      setProperty("profiler.hbase.flush.interval.seconds", "1");
      setProperty("hbase.provider.impl", "" + MockHBaseTableProvider.class.getName());

      // profile settings
      setProperty("profiler.period.duration", Long.toString(periodDurationMillis));
      setProperty("profiler.period.duration.units", "MILLISECONDS");
      setProperty("profiler.ttl", Long.toString(profileTimeToLiveMillis));
      setProperty("profiler.ttl.units", "MILLISECONDS");
      setProperty("profiler.window.duration", Long.toString(windowDurationMillis));
      setProperty("profiler.window.duration.units", "MILLISECONDS");
      setProperty("profiler.window.lag", Long.toString(windowLagMillis));
      setProperty("profiler.window.lag.units", "MILLISECONDS");
      setProperty("profiler.max.routes.per.bolt", Long.toString(maxRoutesPerBolt));
    }};

    // create the mock table
    profilerTable = (MockHTable) MockHBaseTableProvider.addToCache(tableName, columnFamily);

    zkComponent = getZKServerComponent(topologyProperties);

    // create the input and output topics
    kafkaComponent = getKafkaComponent(topologyProperties, Arrays.asList(
            new KafkaComponent.Topic(inputTopic, 1),
            new KafkaComponent.Topic(outputTopic, 1)));

    // upload profiler configuration to zookeeper
    configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties);

    // load flux definition for the profiler topology
    fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(FLUX_PATH))
            .withTopologyName("profiler")
            .withTopologyProperties(topologyProperties)
            .build();

    // start all components
    runner = new ComponentRunner.Builder()
            .withComponent("zk",zkComponent)
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("storm", fluxComponent)
            .withMillisecondsBetweenAttempts(15000)
            .withNumRetries(10)
            .withCustomShutdownOrder(new String[] {"storm","config","kafka","zk"})
            .build();
    runner.start();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    MockHBaseTableProvider.clear();
    if (runner != null) {
      runner.stop();
    }
  }

  @Before
  public void setup() {
    // create the mock table
    profilerTable = (MockHTable) MockHBaseTableProvider.addToCache(tableName, columnFamily);
  }

  @After
  public void tearDown() throws Exception {
    MockHBaseTableProvider.clear();
    profilerTable.clear();
    if (runner != null) {
      runner.reset();
    }
  }

  /**
   * Uploads config values to Zookeeper.
   * @param path The path on the local filesystem to the config values.
   * @throws Exception
   */
  public void uploadConfig(String path) throws Exception {
    configUploadComponent
            .withGlobalConfiguration(path)
            .withProfilerConfiguration(path)
            .update();
  }
}
