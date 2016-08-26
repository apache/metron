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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.profiler.bolt.ProfileHBaseMapper;
import org.apache.metron.test.mock.MockHTable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
  private String message1;

  /**
   * {
   * "ip_src_addr": "10.0.0.2",
   * "protocol": "HTTP",
   * "length": 20,
   * "bytes_in": 390
   * }
   */
  @Multiline
  private String message2;

  /**
   * {
   * "ip_src_addr": "10.0.0.3",
   * "protocol": "DNS",
   * "length": 30,
   * "bytes_in": 560
   * }
   */
  @Multiline
  private String message3;

  private FluxTopologyComponent fluxComponent;
  private KafkaWithZKComponent kafkaComponent;
  private List<byte[]> input;
  private ComponentRunner runner;
  private MockHTable profilerTable;

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  /**
   * A TableProvider that allows us to mock HBase.
   */
  public static class MockTableProvider implements TableProvider, Serializable {

    MockHTable.Provider provider = new MockHTable.Provider();

    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      return provider.getTable(config, tableName);
    }
  }

  /**
   * Tests the first example contained within the README.
   */
  @Test
  public void testExample1() throws Exception {

    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-1");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - there are 5 'HTTP' each with 390 bytes
    double actual = readDouble(ProfileHBaseMapper.QVALUE);
    Assert.assertEquals(390.0 * 5, actual, 0.01);
  }

  /**
   * Tests the second example contained within the README.
   */
  @Test
  public void testExample2() throws Exception {

    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-2");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - there are 5 'HTTP' and 5 'DNS' messages thus 5/5 = 1
    double actual = readDouble(ProfileHBaseMapper.QVALUE);
    Assert.assertEquals(5.0 / 5.0, actual, 0.01);
  }

  /**
   * Tests the third example contained within the README.
   */
  @Test
  public void testExample3() throws Exception {

    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-3");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    double actual = readDouble(ProfileHBaseMapper.QVALUE);
    Assert.assertEquals(20.0, actual, 0.01);
  }

  @Test
  public void testWriteInteger() throws Exception {

    setup(TEST_RESOURCES + "/config/zookeeper/write-integer");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - there are 5 'HTTP' messages each with a length of 20, thus the average should be 20
    double actual = readInteger(ProfileHBaseMapper.QVALUE);
    Assert.assertEquals(10.0, actual, 0.01);
  }

  @Test
  public void testPercentiles() throws Exception {

    setup(TEST_RESOURCES + "/config/zookeeper/percentiles");

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(Constants.INDEXING_TOPIC, input);

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() > 0,
            timeout(seconds(90)));

    // verify - the 70th percentile of 5 x 20s = 20.0
    double actual = readDouble(ProfileHBaseMapper.QVALUE);
    Assert.assertEquals(20.0, actual, 0.01);
  }

  /**
   * Reads a Double value written by the Profiler.
   * @param columnQual The column qualifier.
   */
  private Double readDouble(byte[] columnQual) throws IOException {
    final byte[] cf = Bytes.toBytes(columnFamily);
    ResultScanner scanner = profilerTable.getScanner(cf, columnQual);

    for (Result result : scanner) {
      byte[] raw = result.getValue(cf, ProfileHBaseMapper.QVALUE);
      return Bytes.toDouble(raw);
    }

    throw new IllegalStateException("No results found");
  }

  /**
   * Reads an Integer value written by the Profiler.
   * @param columnQual The column qualifier.
   */
  private Integer readInteger(byte[] columnQual) throws IOException {
    final byte[] cf = Bytes.toBytes(columnFamily);
    ResultScanner scanner = profilerTable.getScanner(cf, columnQual);

    for (Result result : scanner) {
      byte[] raw = result.getValue(cf, ProfileHBaseMapper.QVALUE);
      return Bytes.toInt(raw);
    }

    throw new IllegalStateException("No results found");
  }

  public void setup(String pathToConfig) throws Exception {

    // create input messages for the profiler to consume
    input = Stream.of(message1, message2, message3)
            .map(Bytes::toBytes)
            .map(m -> Collections.nCopies(5, m))
            .flatMap(l -> l.stream())
            .collect(Collectors.toList());

    // storm topology properties
    final Properties topologyProperties = new Properties() {{
      setProperty("kafka.start", SpoutConfig.Offset.BEGINNING.name());
      setProperty("profiler.workers", "1");
      setProperty("profiler.executors", "0");
      setProperty("profiler.input.topic", Constants.INDEXING_TOPIC);
      setProperty("profiler.flush.interval.seconds", "15");
      setProperty("profiler.hbase.salt.divisor", "10");
      setProperty("profiler.hbase.table", tableName);
      setProperty("profiler.hbase.batch", "10");
      setProperty("profiler.hbase.flush.interval.seconds", "1");
      setProperty("hbase.provider.impl", "" + MockTableProvider.class.getName());
    }};

    // create the mock table
    profilerTable = (MockHTable) MockHTable.Provider.addToCache(tableName, columnFamily);

    // create the input topic
    kafkaComponent = getKafkaComponent(topologyProperties,
            Arrays.asList(new KafkaWithZKComponent.Topic(Constants.INDEXING_TOPIC, 1)));

    // upload profiler configuration to zookeeper
    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfigsPath(pathToConfig)
            .withProfilerConfigsPath(pathToConfig);

    // load flux definition for the profiler topology
    fluxComponent = new FluxTopologyComponent.Builder()
            .withTopologyLocation(new File(FLUX_PATH))
            .withTopologyName("profiler")
            .withTopologyProperties(topologyProperties)
            .build();

    // start all components
    runner = new ComponentRunner.Builder()
            .withComponent("kafka", kafkaComponent)
            .withComponent("config", configUploadComponent)
            .withComponent("storm", fluxComponent)
            .withMillisecondsBetweenAttempts(15000)
            .withNumRetries(10)
            .build();
    runner.start();
  }

  @After
  public void tearDown() throws Exception {
    if (runner != null) {
      runner.stop();
    }
  }
}
