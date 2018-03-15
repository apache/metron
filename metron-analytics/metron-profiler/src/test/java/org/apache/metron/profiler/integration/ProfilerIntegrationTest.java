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

import com.google.common.base.Joiner;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.math.util.MathUtils;
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
import org.apache.metron.statistics.OnlineStatisticsProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
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

/**
 * An integration test of the Profiler topology.
 */
public class ProfilerIntegrationTest extends BaseIntegrationTest {

  private static final String TEST_RESOURCES = "../../metron-analytics/metron-profiler/src/test";
  private static final String FLUX_PATH = "../metron-profiler/src/main/flux/profiler/remote.yaml";

  /**
   * {
   * "ip_src_addr": "10.0.0.1",
   * "protocol": "HTTPS",
   * "length": 10,
   * "bytes_in": 234
   * }
   */
  @Multiline
  private static String message1;

  /**
   * {
   * "ip_src_addr": "10.0.0.2",
   * "protocol": "HTTP",
   * "length": 20,
   * "bytes_in": 390
   * }
   */
  @Multiline
  private static String message2;

  /**
   * {
   * "ip_src_addr": "10.0.0.3",
   * "protocol": "DNS",
   * "length": 30,
   * "bytes_in": 560
   * }
   */
  @Multiline
  private static String message3;

  private static ColumnBuilder columnBuilder;
  private static ZKServerComponent zkComponent;
  private static FluxTopologyComponent fluxComponent;
  private static KafkaComponent kafkaComponent;
  private static ConfigUploadComponent configUploadComponent;
  private static ComponentRunner runner;
  private static MockHTable profilerTable;

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private static final double epsilon = 0.001;
  private static final String inputTopic = Constants.INDEXING_TOPIC;
  private static final String outputTopic = "profiles";
  private static final int saltDivisor = 10;

  private static final long windowLagMillis = TimeUnit.SECONDS.toMillis(5);
  private static final long windowDurationMillis = TimeUnit.SECONDS.toMillis(5);
  private static final long periodDurationMillis = TimeUnit.SECONDS.toMillis(15);
  private static final long profileTimeToLiveMillis = TimeUnit.SECONDS.toMillis(20);
  private static final long maxRoutesPerBolt = 100000;

  /**
   * Tests the first example contained within the README.
   */
  @Test
  public void testExample1() throws Exception {

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/readme-example-1");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message1, message1);
    kafkaComponent.writeMessages(inputTopic, message2, message2, message2);
    kafkaComponent.writeMessages(inputTopic, message3, message3, message3);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(180)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily,
            columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - there are 3 'HTTP' each with 390 bytes
    Assert.assertTrue(actuals.stream().anyMatch(val ->
            MathUtils.equals(390.0 * 3, val, epsilon)
    ));
  }

  /**
   * Tests the second example contained within the README.
   */
  @Test
  public void testExample2() throws Exception {

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/readme-example-2");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message1, message1);
    kafkaComponent.writeMessages(inputTopic, message2, message2, message2);
    kafkaComponent.writeMessages(inputTopic, message3, message3, message3);

    // expect 2 values written by the profile; one for 10.0.0.2 and another for 10.0.0.3
    final int expected = 2;

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= expected,
            timeout(seconds(90)));

    // verify - expect 2 results as 2 hosts involved; 10.0.0.2 sends 'HTTP' and 10.0.0.3 send 'DNS'
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily,
            columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - 10.0.0.3 -> 1/4
    Assert.assertTrue( "Could not find a value near 1/4. Actual values read are are: " + Joiner.on(",").join(actuals),
            actuals.stream().anyMatch(val -> MathUtils.equals(val, 1.0/4.0, epsilon)
    ));

    // verify - 10.0.0.2 -> 4/1
    Assert.assertTrue("Could not find a value near 4. Actual values read are are: " + Joiner.on(",").join(actuals),
            actuals.stream().anyMatch(val -> MathUtils.equals(val, 4.0/1.0, epsilon)
    ));
  }

  /**
   * Tests the third example contained within the README.
   */
  @Test
  public void testExample3() throws Exception {

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/readme-example-3");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message1, message1);
    kafkaComponent.writeMessages(inputTopic, message2, message2, message2);
    kafkaComponent.writeMessages(inputTopic, message3, message3, message3);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily,
            columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals),
            actuals.stream().anyMatch(val -> MathUtils.equals(val, 20.0, epsilon)
    ));
  }

  /**
   * Tests the fourth example contained within the README.
   */
  @Test
  public void testExample4() throws Exception {

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/readme-example-4");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message1, message1);
    kafkaComponent.writeMessages(inputTopic, message2, message2, message2);
    kafkaComponent.writeMessages(inputTopic, message3, message3, message3);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    byte[] column = columnBuilder.getColumnQualifier("value");
    List<OnlineStatisticsProvider> actuals = read(profilerTable.getPutLog(), columnFamily, column, OnlineStatisticsProvider.class);

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals),
            actuals.stream().anyMatch(val -> MathUtils.equals(val.getMean(), 20.0, epsilon)
    ));
  }

  @Test
  public void testPercentiles() throws Exception {

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/percentiles");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message1, message1);
    kafkaComponent.writeMessages(inputTopic, message2, message2, message2);
    kafkaComponent.writeMessages(inputTopic, message3, message3, message3);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily,
            columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - the 70th percentile of x3, 20s = 20.0
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals),
            actuals.stream().anyMatch(val -> MathUtils.equals(val, 20.0, epsilon)));
  }

  /**
   * The Profiler can optionally perform event time processing.  With event time processing,
   * the Profiler uses timestamps contained in the source telemetry.
   *
   * <p>Defining a 'timestampField' within the Profiler configuration tells the Profiler
   * from which field the timestamp should be extracted.
   */
  @Test
  public void testEventTimeProcessing() throws Exception {

    // constants used for the test
    final long startAt = 10;
    final String entity = "10.0.0.1";
    final String profileName = "event-time-test";

    // create some messages that contain a timestamp - a really old timestamp; close to 1970
    String message1 = new MessageBuilder()
            .withField("ip_src_addr", entity)
            .withField("timestamp", startAt)
            .build()
            .toJSONString();

    String message2 = new MessageBuilder()
            .withField("ip_src_addr", entity)
            .withField("timestamp", startAt + 100)
            .build()
            .toJSONString();

    uploadConfig(TEST_RESOURCES + "/config/zookeeper/event-time-test");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1, message2);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    List<Put> puts = profilerTable.getPutLog();
    assertEquals(1, puts.size());

    // inspect the row key to ensure the profiler used event time correctly.  the timestamp
    // embedded in the row key should match those in the source telemetry
    byte[] expectedRowKey = generateExpectedRowKey(profileName, entity, startAt);
    byte[] actualRowKey = puts.get(0).getRow();
    String msg = String.format("expected '%s', got '%s'",
            new String(expectedRowKey, "UTF-8"),
            new String(actualRowKey, "UTF-8"));
    assertArrayEquals(msg, expectedRowKey, actualRowKey);
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
