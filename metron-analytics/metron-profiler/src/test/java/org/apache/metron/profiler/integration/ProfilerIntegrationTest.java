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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.Constants;
import org.apache.metron.common.spout.kafka.SpoutConfig;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.profiler.hbase.ColumnBuilder;
import org.apache.metron.profiler.hbase.ValueOnlyColumnBuilder;
import org.apache.metron.statistics.OnlineStatisticsProvider;
import org.apache.metron.test.mock.MockHTable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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
  private String httpsMessage;

  /**
   * {
   * "ip_src_addr": "10.0.0.2",
   * "protocol": "HTTP",
   * "length": 20,
   * "bytes_in": 390
   * }
   */
  @Multiline
  private String httpMessage;

  /**
   * {
   * "ip_src_addr": "10.0.0.3",
   * "protocol": "DNS",
   * "length": 30,
   * "bytes_in": 560
   * }
   */
  @Multiline
  private String dnsMessage;

  private ColumnBuilder columnBuilder;
  private ZKServerComponent zkComponent;
  private FluxTopologyComponent fluxComponent;
  private KafkaComponent kafkaComponent;
  private ComponentRunner runner;
  private MockHTable profilerTable;

  private static final String tableName = "profiler";
  private static final String columnFamily = "P";

  /**
   * Tests the first example contained within the README.
   *
   * The test uses the Profiler's ability to drive profile creation based on the time when
   * the input data actually occurred versus when that data is being processed.  This is
   * known as event time processing.
   *
   * The test generates 24 hours worth of telemetry, sending each telemetry message type
   * (there are 3) once per minute.  The Profiler consumes this data and flushes the profile
   * every 15 minutes.
   *
   * This produces a large set of profile measurements that should be mostly consistent.
   * Depending on where the windows are cut some of the profile measurements will be skewed,
   * especially early in the testing cycle.  To validate the test, we expect at least 95%
   * of them are accurate.
   */
  @Test
  public void testExample1() throws Exception {

    final long testDuration = TimeUnit.HOURS.toMillis(24);
    final int periodDuration = 15;
    final long periodDurationMillis = TimeUnit.MINUTES.toMillis(periodDuration);
    final long messageFrequency = TimeUnit.MINUTES.toMillis(1);
    final long numberOfPeriods = testDuration / periodDurationMillis;
    final long numberOfEntities = 1;  // only 10.0.0.2 sends HTTP messages that will be applied to this profile
    final long numberOfFlushes = numberOfPeriods * numberOfEntities;
    final long messagesPerPeriod = periodDurationMillis / messageFrequency;
    final double percentageCorrect = 0.95;

    Properties properties = createProperties(periodDuration, TimeUnit.MINUTES);
    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-1", properties);

    // start the profiler
    fluxComponent.submitTopology();

    // create the messages that will be consumed by the Profiler
    kafkaComponent.writeMessages(
            Constants.INDEXING_TOPIC,
            createMessages(messageFrequency, testDuration, httpsMessage, httpMessage, dnsMessage));

    // verify - ensure the profile is being persisted
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= numberOfFlushes,
            timeout(seconds(90)));

    // validate - fetch the actual data that was written by the profiler
    Multiset<Double> actuals = HashMultiset.create(
            read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class));

    // validate - each 'HTTP' message sent by 10.0.0.2 has a value of 390 bytes
    final double expected = 390.0 * messagesPerPeriod;
    final long expectedCount = (long) Math.floor(numberOfPeriods * percentageCorrect);
    Assert.assertTrue(String.format("expected to see '%s' more than %s time(s), but got %s", expected, expectedCount, actuals),
            actuals.count(expected) > expectedCount);
  }

  /**
   * Tests the second example contained within the README.
   *
   * The test uses the Profiler's ability to drive profile creation based on the time when
   * the input data actually occurred versus when that data is being processed.  This is
   * known as event time processing.
   *
   * The test generates 24 hours worth of telemetry, sending each telemetry message type
   * (there are 3) once per minute.  The Profiler consumes this data and flushes the profile
   * every 15 minutes.
   *
   * This produces a large set of profile measurements that should be mostly consistent.
   * Depending on where the windows are cut some of the profile measurements will be skewed,
   * especially early in the testing cycle.  To validate the test, we expect at least 95%
   * of them are accurate.
   */
  @Test
  public void testExample2() throws Exception {

    final long testDuration = TimeUnit.HOURS.toMillis(24);
    final int periodDuration = 15;
    final long periodDurationMillis = TimeUnit.MINUTES.toMillis(periodDuration);
    final long messageFrequency = TimeUnit.MINUTES.toMillis(1);
    final long numberOfPeriods = testDuration / periodDurationMillis;
    final long numberOfEntities = 2;  // 10.0.0.2 and 10.0.0.3 produce data that will be included in the profile
    final long numberOfFlushes = numberOfPeriods * numberOfEntities;
    final long messagesPerPeriod = periodDurationMillis / messageFrequency;
    final double percentageCorrect = 0.95;

    Properties properties = createProperties(periodDuration, TimeUnit.MINUTES);
    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-2", properties);

    // start the profiler
    fluxComponent.submitTopology();

    // create the messages that will be consumed by the Profiler
    kafkaComponent.writeMessages(
            Constants.INDEXING_TOPIC,
            createMessages(messageFrequency, testDuration, httpsMessage, httpMessage, dnsMessage));

    // wait for the expected number of writes to hbase
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= numberOfFlushes,
            timeout(seconds(90)));

    // validate - fetch the actual data that was written by the profiler
    Multiset<Double> actuals = HashMultiset.create(
            read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class));

    {
      // validate the profile generated for 10.0.0.3 which sends DNS only
      final double expected = messagesPerPeriod + 1.0;
      final long expectedCount = (long) Math.floor(numberOfPeriods * percentageCorrect);
      Assert.assertTrue(String.format("expected to see '%s' more than %s time(s), but got %s", expected, expectedCount, actuals),
              actuals.count(expected) > expectedCount);
    }
    {
      // validate the profile generated for 10.0.0.2 which sends HTTP only
      final double expected = 1.0 / (messagesPerPeriod + 1.0);
      final long expectedCount = (long) Math.floor(numberOfPeriods * percentageCorrect);
      Assert.assertTrue(String.format("expected to see '%s' more than %s time(s), but got %s", expected, expectedCount, actuals),
              actuals.count(expected) > expectedCount);
    }
  }

  /**
   * Tests the third example contained within the README.
   *
   * The test uses the Profiler's ability to drive profile creation based on the time when
   * the input data actually occurred versus when that data is being processed.  This is
   * known as event time processing.
   *
   * The test generates 24 hours worth of telemetry, sending each telemetry message type
   * (there are 3) once per minute.  The Profiler consumes this data and flushes the profile
   * every 15 minutes.
   *
   * This produces a large set of profile measurements that should be mostly consistent.
   * Depending on where the windows are cut some of the profile measurements will be skewed,
   * especially early in the testing cycle.  To validate the test, we expect at least 95%
   * of them are accurate.
   */
  @Test
  public void testExample3() throws Exception {

    final long testDuration = TimeUnit.HOURS.toMillis(24);
    final int periodDuration = 15;
    final long periodDurationMillis = TimeUnit.MINUTES.toMillis(periodDuration);
    final long messageFrequency = TimeUnit.MINUTES.toMillis(1);
    final long numberOfPeriods = testDuration / periodDurationMillis;
    final long numberOfEntities = 1;  // only 10.0.0.2 sends HTTP messages that will be applied to this profile
    final long numberOfFlushes = numberOfPeriods * numberOfEntities;
    final double percentageCorrect = 0.95;

    Properties properties = createProperties(periodDuration, TimeUnit.MINUTES);
    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-3", properties);

    // start the profiler
    fluxComponent.submitTopology();

    // create the messages that will be consumed by the Profiler
    kafkaComponent.writeMessages(
            Constants.INDEXING_TOPIC,
            createMessages(messageFrequency, testDuration, httpsMessage, httpMessage, dnsMessage));

    // wait for the expected number of writes to hbase
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= numberOfFlushes,
            timeout(seconds(90)));

    // validate - fetch the actual data that was written by the profiler
    Multiset<Double> actuals = HashMultiset.create(
            read(profilerTable.getPutLog(), columnFamily, columnBuilder.getColumnQualifier("value"), Double.class));

    // verify - only 10.0.0.2 sends 'HTTP', each with a length of 20, thus the average should be 20
    final double expected = 20.0;
    final long expectedCount = (long) Math.floor(numberOfPeriods * percentageCorrect);
    Assert.assertTrue(String.format("expected to see '%s' more than %s time(s), but got %s", expected, expectedCount, actuals),
            actuals.count(expected) > expectedCount);
  }

  /**
   * Tests the fourth example contained within the README.
   *
   * The test uses the Profiler's ability to drive profile creation based on the time when
   * the input data actually occurred versus when that data is being processed.  This is
   * known as event time processing.
   *
   * The test generates 24 hours worth of telemetry, sending each telemetry message type
   * (there are 3) once per minute.  The Profiler consumes this data and flushes the profile
   * every 15 minutes.
   *
   * This produces a large set of profile measurements that should be mostly consistent.
   * Depending on where the windows are cut some of the profile measurements will be skewed,
   * especially early in the testing cycle.  To validate the test, we expect at least 95%
   * of them are accurate.
   */
  @Test
  public void testExample4() throws Exception {

    final long testDuration = TimeUnit.HOURS.toMillis(24);
    final int periodDuration = 15;
    final long periodDurationMillis = TimeUnit.MINUTES.toMillis(periodDuration);
    final long messageFrequency = TimeUnit.MINUTES.toMillis(1);
    final long numberOfPeriods = testDuration / periodDurationMillis;
    final long numberOfEntities = 1;  // only 10.0.0.2 sends HTTP messages that will be applied to this profile
    final long numberOfFlushes = numberOfPeriods * numberOfEntities;
    final double percentageCorrect = 0.95;

    Properties properties = createProperties(periodDuration, TimeUnit.MINUTES);
    setup(TEST_RESOURCES + "/config/zookeeper/readme-example-4", properties);

    // start the profiler
    fluxComponent.submitTopology();

    // create the messages that will be consumed by the Profiler
    kafkaComponent.writeMessages(
            Constants.INDEXING_TOPIC,
            createMessages(messageFrequency, testDuration, httpsMessage, httpMessage, dnsMessage));

    // wait for the expected number of writes to hbase
    waitOrTimeout(() -> profilerTable.getPutLog().size() >= numberOfFlushes,
            timeout(seconds(90)));

    // the profiler stored statistical summaries of the data
    List<OnlineStatisticsProvider> summaries = read(
            profilerTable.getPutLog(),
            columnFamily,
            columnBuilder.getColumnQualifier("value"),
            OnlineStatisticsProvider.class);

    // use the statistical summaries to calculate the mean
    Multiset<Double> actuals = HashMultiset.create(
            summaries.stream().map(s -> s.getMean()).collect(Collectors.toList()));

    // verify - only 10.0.0.2 sends 'HTTP', each with a length of 20, thus the average should be 20
    final double expected = 20.0;
    final long expectedCount = (long) Math.floor(numberOfPeriods * percentageCorrect);
    Assert.assertTrue(String.format("expected to see '%s' more than %s time(s), but got %s", expected, expectedCount, actuals),
            actuals.count(expected) > expectedCount);
  }

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

  /**
   * Parse a JSON string to a JSONObject.  Only needed to catch the checked exception.
   * @param json The JSON string.
   * @param parser The JSON parser.
   */
  private JSONObject parse(String json, JSONParser parser) {
    try {
      return (JSONObject) parser.parse(json);
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Parse a JSON string to a JSONObject.  Only needed to catch the checked exception.
   * @param json The JSON string.
   * @param parser The JSON parser.
   */
  private JSONObject parseAndAddField(String json, JSONParser parser, String field, Long value) {
    try {
      JSONObject parsed = (JSONObject) parser.parse(json);
      parsed.put(field, value);
      return parsed;

    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  private List<byte[]> createMessages(long frequencyMillis, long durationMillis, String... messages) {

    List<JSONObject> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    long count = durationMillis / frequencyMillis;
    for(String message : messages) {
      LongStream
              .iterate(0, i -> i + frequencyMillis)
              .limit(count)
              .mapToObj(i -> parseAndAddField(message, parser, "timestamp", i))
              .forEach(o -> results.add(o));
    }

    // sort and serialize each message
    return results
            .stream()
            .sorted(Comparator.comparingLong(m -> (Long) m.get("timestamp")))
            .map(json -> json.toJSONString())
            .map(Bytes::toBytes)
            .collect(Collectors.toList());
  }

  public void setup(String pathToConfig, Properties topologyProperties) throws Exception {
    columnBuilder = new ValueOnlyColumnBuilder(columnFamily);

    // create the mock table
    profilerTable = (MockHTable) MockHTable.Provider.addToCache(tableName, columnFamily);

    // zookeeper
    zkComponent = getZKServerComponent(topologyProperties);

    // create the input topic
    kafkaComponent = getKafkaComponent(topologyProperties,
            Arrays.asList(new KafkaComponent.Topic(Constants.INDEXING_TOPIC, 1)));

    // upload profiler configuration to zookeeper
    ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
            .withTopologyProperties(topologyProperties)
            .withGlobalConfiguration(pathToConfig)
            .withProfilerConfiguration(pathToConfig);

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

  /**
   * Creates the set of properties used by the Profiler topology.
   */
  private Properties createProperties(int periodDuration, TimeUnit periodUnits) {
    return new Properties() {{

      setProperty("profiler.workers", "1");
      setProperty("profiler.executors", "0");

      // how the profiler consumes messages
      setProperty("profiler.input.topic", Constants.INDEXING_TOPIC);
      setProperty("kafka.start", SpoutConfig.Offset.BEGINNING.name());

      // duration of each profile period
      setProperty("profiler.period.duration", Integer.toString(periodDuration));
      setProperty("profiler.period.duration.units", periodUnits.toString());

      // lifespan of a tuple - must be greater than twice the profile period
      setProperty("topology.message.timeout.secs", Long.toString(periodUnits.toSeconds(periodDuration) * 3));

      // lifespan of a profile - will be forgotten if no messages received within
      setProperty("profiler.ttl", Integer.toString(periodDuration * 3));
      setProperty("profiler.ttl.units", periodUnits.toString());

      // where profiles are written to in hbase
      setProperty("profiler.hbase.table", tableName);
      setProperty("profiler.hbase.column.family", columnFamily);

      // how profiles are written to hbase
      setProperty("profiler.hbase.batch", "1");
      setProperty("profiler.hbase.flush.interval.seconds", "1");
      setProperty("hbase.provider.impl", "" + MockTableProvider.class.getName());
      setProperty("profiler.hbase.salt.divisor", "10");

      // event time processing
      setProperty("profiler.event.timestamp.field","timestamp");
      setProperty("profiler.event.time.lag","0");
      setProperty("profiler.event.time.lag.units","SECONDS");
    }};
  }

  @After
  public void tearDown() throws Exception {
    MockHTable.Provider.clear();
    if (runner != null) {
      runner.stop();
    }
  }
}