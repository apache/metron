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
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.profiler.hbase.ColumnBuilder;
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;

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
  private static List<byte[]> input;
  private static ComponentRunner runner;
  private static MockHTable profilerTable;

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private static final double epsilon = 0.001;
  private static final String inputTopic = Constants.INDEXING_TOPIC;
  private static final String outputTopic = "profiles";


  /**
   * Tests the first example contained within the README.
   */
  @Test
  public void testExample1() throws Exception {

    update(TEST_RESOURCES + "/config/zookeeper/readme-example-1");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - there are 5 'HTTP' each with 390 bytes
    Assert.assertTrue(actuals.stream().anyMatch(val ->
            MathUtils.equals(390.0 * 5, val, epsilon)
    ));
  }

  /**
   * Tests the second example contained within the README.
   */
  @Test
  public void testExample2() throws Exception {

    update(TEST_RESOURCES + "/config/zookeeper/readme-example-2");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, input);

    // expect 2 values written by the profile; one for 10.0.0.2 and another for 10.0.0.3
    final int expected = 2;

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= expected,
            timeout(seconds(90)));

    // verify - expect 2 results as 2 hosts involved; 10.0.0.2 sends 'HTTP' and 10.0.0.3 send 'DNS'
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - 10.0.0.3 -> 1/6
    Assert.assertTrue( "Could not find a value near 1/6. Actual values read are are: " + Joiner.on(",").join(actuals)
                     , actuals.stream().anyMatch(val -> MathUtils.equals(val, 1.0/6.0, epsilon)
    ));

    // verify - 10.0.0.2 -> 6/1
    Assert.assertTrue("Could not find a value near 6. Actual values read are are: " + Joiner.on(",").join(actuals)
            ,actuals.stream().anyMatch(val -> MathUtils.equals(val, 6.0/1.0, epsilon)
    ));
  }

  /**
   * Tests the third example contained within the README.
   */
  @Test
  public void testExample3() throws Exception {

    update(TEST_RESOURCES + "/config/zookeeper/readme-example-3");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals)
                     , actuals.stream().anyMatch(val -> MathUtils.equals(val, 20.0, epsilon)
    ));
  }

  /**
   * Tests the fourth example contained within the README.
   */
  @Test
  public void testExample4() throws Exception {

    update(TEST_RESOURCES + "/config/zookeeper/readme-example-4");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - only 10.0.0.2 sends 'HTTP', thus there should be only 1 value
    byte[] column = columnBuilder.getColumnQualifier("value");
    List<OnlineStatisticsProvider> actuals = read(profilerTable.getPutLog(), columnFamily, column, OnlineStatisticsProvider.class);

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals)
                     , actuals.stream().anyMatch(val -> MathUtils.equals(val.getMean(), 20.0, epsilon)
    ));
  }

  @Test
  public void testPercentiles() throws Exception {

    update(TEST_RESOURCES + "/config/zookeeper/percentiles");


    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    List<Double> actuals = read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class);

    // verify - the 70th percentile of 5 x 20s = 20.0
    Assert.assertTrue("Could not find a value near 20. Actual values read are are: " + Joiner.on(",").join(actuals)
                     , actuals.stream().anyMatch(val -> MathUtils.equals(val, 20.0, epsilon)));
  }

  /**
   * Reads a value written by the Profiler.
   * @param family The column family.
   * @param qualifier The column qualifier.
   * @param clazz The expected type of the value.
   * @param <T> The expected type of the value.
   * @return The value written by the Profiler.
   */
  private <T> List<T> read(List<Put> puts, String family, byte[] qualifier, Class<T> clazz) {
    List<T> results = new ArrayList<>();

    for(Put put: puts) {
      for(Cell cell: put.get(Bytes.toBytes(family), qualifier)) {
        T value = SerDeUtils.fromBytes(cell.getValue(), clazz);
        results.add(value);
      }
    }

    return results;
  }

  @BeforeClass
  public static void setupBeforeClass() throws UnableToStartException {
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    List<String> inputNew = Stream.of(message1, message2, message3)
        .map(m -> Collections.nCopies(5, m))
        .flatMap(l -> l.stream())
        .collect(Collectors.toList());

    // create input messages for the profiler to consume
    input = Stream.of(message1, message2, message3)
            .map(Bytes::toBytes)
            .map(m -> Collections.nCopies(5, m))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());

    // storm topology properties
    final Properties topologyProperties = new Properties() {{
      setProperty("kafka.start", "UNCOMMITTED_EARLIEST");
      setProperty("profiler.workers", "1");
      setProperty("profiler.executors", "0");
      setProperty("profiler.input.topic", inputTopic);
      setProperty("profiler.output.topic", outputTopic);
      setProperty("profiler.period.duration", "20");
      setProperty("profiler.period.duration.units", "SECONDS");
      setProperty("profiler.ttl", "30");
      setProperty("profiler.ttl.units", "MINUTES");
      setProperty("profiler.hbase.salt.divisor", "10");
      setProperty("profiler.hbase.table", tableName);
      setProperty("profiler.hbase.column.family", columnFamily);
      setProperty("profiler.hbase.batch", "10");
      setProperty("profiler.hbase.flush.interval.seconds", "1");
      setProperty("profiler.profile.ttl", "20");
      setProperty("hbase.provider.impl", "" + MockHBaseTableProvider.class.getName());
      setProperty("storm.auto.credentials", "[]");
      setProperty("kafka.security.protocol", "PLAINTEXT");
    }};

    // create the mock table
    profilerTable = (MockHTable) MockHBaseTableProvider.addToCache(tableName, columnFamily);

    zkComponent = getZKServerComponent(topologyProperties);

    // create the input topic
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

  public void update(String path) throws Exception {
    configUploadComponent.withGlobalConfiguration(path)
        .withProfilerConfiguration(path);
    configUploadComponent.update();
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
}