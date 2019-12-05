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

package org.apache.metron.profiler.storm.integration;

import java.nio.charset.StandardCharsets;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.io.FileUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.hbase.mock.MockHTable;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.UnableToStartException;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.profiler.client.stellar.FixedLookback;
import org.apache.metron.profiler.client.stellar.GetProfile;
import org.apache.metron.profiler.client.stellar.WindowLookback;
import org.apache.metron.statistics.OnlineStatisticsProvider;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.storm.Config;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.integration.utils.TestUtils.assertEventually;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_COLUMN_FAMILY;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_HBASE_TABLE_PROVIDER;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_SALT_DIVISOR;
import static org.apache.metron.profiler.storm.KafkaEmitter.ALERT_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.ENTITY_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.PERIOD_END_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.PERIOD_ID_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.PERIOD_START_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.PROFILE_FIELD;
import static org.apache.metron.profiler.storm.KafkaEmitter.TIMESTAMP_FIELD;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * An integration test of the Profiler topology.
 */
public class ProfilerIntegrationTest extends BaseIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TEST_RESOURCES = "../../metron-analytics/metron-profiler-storm/src/test";
  private static final String FLUX_PATH = "src/main/flux/profiler/remote.yaml";
  private static final long timeout = TimeUnit.SECONDS.toMillis(90);

  public static final long startAt = 10;
  public static final String entity = "10.0.0.1";
  private static final String tableName = "profiler";
  private static final String columnFamily = "P";
  private static final String inputTopic = Constants.INDEXING_TOPIC;
  private static final String outputTopic = "profiles";
  private static final int saltDivisor = 10;
  private static final long periodDurationMillis = TimeUnit.SECONDS.toMillis(20);
  private static final long windowLagMillis = TimeUnit.SECONDS.toMillis(10);
  private static final long windowDurationMillis = TimeUnit.SECONDS.toMillis(10);
  private static final long profileTimeToLiveMillis = TimeUnit.SECONDS.toMillis(20);
  private static final long maxRoutesPerBolt = 100000;

  private static ZKServerComponent zkComponent;
  private static FluxTopologyComponent fluxComponent;
  private static KafkaComponent kafkaComponent;
  private static ConfigUploadComponent configUploadComponent;
  private static ComponentRunner runner;
  private static MockHTable profilerTable;
  private static String message1;
  private static String message2;
  private static String message3;
  private StellarStatefulExecutor executor;

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
   *    org.json.simple.JSONArray,
   *    java.util.LinkedHashMap,
   *    org.apache.metron.statistics.OnlineStatisticsProvider
   *  ]
   */
  @Multiline
  private static String kryoSerializers;

  /**
   * {
   *    "profiles": [
   *      {
   *        "profile": "processing-time-test",
   *        "foreach": "ip_src_addr",
   *        "init": { "counter": "0" },
   *        "update": { "counter": "counter + 1" },
   *        "result": "counter"
   *      }
   *    ]
   * }
   */
  @Multiline
  private static String processingTimeProfile;

  @Test
  public void testProcessingTime() throws Exception {
    uploadConfigToZookeeper(ProfilerConfig.fromJSON(processingTimeProfile));

    // start the topology and write 3 test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1);
    kafkaComponent.writeMessages(inputTopic, message2);
    kafkaComponent.writeMessages(inputTopic, message3);

    // retrieve the profile measurement using PROFILE_GET
    String profileGetExpression = "PROFILE_GET('processing-time-test', '10.0.0.1', PROFILE_FIXED('15', 'MINUTES'))";
    List<Integer> measurements = execute(profileGetExpression, List.class);

    // need to keep checking for measurements until the profiler has flushed one out
    int attempt = 0;
    while(measurements.size() == 0 && attempt++ < 10) {

      // wait for the profiler to flush
      long sleep = windowDurationMillis;
      LOG.debug("Waiting {} millis for profiler to flush", sleep);
      Thread.sleep(sleep);

      // write another message to advance time. this ensures we are testing the 'normal' flush mechanism.
      // if we do not send additional messages to advance time, then it is the profile TTL mechanism which
      // will ultimately flush the profile
      kafkaComponent.writeMessages(inputTopic, message2);

      // try again to retrieve the profile measurement using PROFILE_GET
      measurements = execute(profileGetExpression, List.class);
    }

    // expect to see only 1 measurement, but could be more (one for each period) depending on
    // how long we waited for the flush to occur
    assertTrue(measurements.size() > 0);

    // the profile should have counted at least 3 messages; the 3 test messages that were sent.
    // the count could be higher due to the test messages we sent to advance time.
    assertTrue(measurements.get(0) >= 3);
  }

  @Test
  public void testProcessingTimeWithTimeToLiveFlush() throws Exception {
    uploadConfigToZookeeper(ProfilerConfig.fromJSON(processingTimeProfile));

    // start the topology and write 3 test messages to kafka
    fluxComponent.submitTopology();
    kafkaComponent.writeMessages(inputTopic, message1);
    kafkaComponent.writeMessages(inputTopic, message2);
    kafkaComponent.writeMessages(inputTopic, message3);

    // wait a bit beyond the window lag before writing another message.  this allows storm's window manager to close
    // the event window, which then lets the profiler processes the previous messages.
    long sleep = windowLagMillis + periodDurationMillis;
    LOG.debug("Waiting {} millis before sending message to close window", sleep);
    Thread.sleep(sleep);
    kafkaComponent.writeMessages(inputTopic, message3);

    // the profile should have counted 3 messages; the 3 test messages that were sent
    assertEventually(() -> {
      List<Integer> results = execute("PROFILE_GET('processing-time-test', '10.0.0.1', PROFILE_FIXED('15', 'MINUTES'))", List.class);
      assertThat(results, hasItem(3));
    }, timeout);
  }

  /**
   * {
   *    "timestampField": "timestamp",
   *    "profiles": [
   *      {
   *        "profile": "count-by-ip",
   *        "foreach": "ip_src_addr",
   *        "init": { "count": 0 },
   *        "update": { "count" : "count + 1" },
   *        "result": "count"
   *      },
   *      {
   *        "profile": "total-count",
   *        "foreach": "'total'",
   *        "init": { "count": 0 },
   *        "update": { "count": "count + 1" },
   *        "result": "count"
   *      }
   *    ]
   * }
   */
  @Multiline
  private static String eventTimeProfile;

  @Test
  public void testEventTime() throws Exception {
    uploadConfigToZookeeper(ProfilerConfig.fromJSON(eventTimeProfile));

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    List<String> messages = FileUtils.readLines(new File("src/test/resources/telemetry.json"));
    kafkaComponent.writeMessages(inputTopic, messages);

    long timestamp = System.currentTimeMillis();
    LOG.debug("Attempting to close window period by sending message with timestamp = {}", timestamp);
    kafkaComponent.writeMessages(inputTopic, getMessage("192.168.66.1", timestamp));
    kafkaComponent.writeMessages(inputTopic, getMessage("192.168.138.158", timestamp));

    // create the 'window' that looks up to 5 hours before the max timestamp contained in the test data
    assign("maxTimestamp", "1530978728982L");
    assign("window", "PROFILE_WINDOW('from 5 hours ago', maxTimestamp)");

    // wait until the profile flushes both periods.  the first period will flush immediately as subsequent messages
    // advance time.  the next period contains all of the remaining messages, so there are no other messages to
    // advance time.  because of this the next period only flushes after the time-to-live expires

    // there are 14 messages in the first period and 12 in the next where ip_src_addr = 192.168.66.1
    assertEventually(() -> {
      List<Integer> results = execute("PROFILE_GET('count-by-ip', '192.168.66.1', window)", List.class);
      assertThat(results, hasItems(14, 12));
      }, timeout);

    // there are 36 messages in the first period and 38 in the next where ip_src_addr = 192.168.138.158
    assertEventually(() -> {
      List<Integer> results = execute("PROFILE_GET('count-by-ip', '192.168.138.158', window)", List.class);
      assertThat(results, hasItems(36, 38));
      }, timeout);

    // in all there are 50 (36+14) messages in the first period and 50 (38+12) messages in the next
    assertEventually(() -> {
      List<Integer> results = execute("PROFILE_GET('total-count', 'total', window)", List.class);
      assertThat(results, hasItems(50, 50));
      }, timeout);
  }

  /**
   * {
   *    "profiles": [
   *      {
   *        "profile": "profile-with-stats",
   *        "foreach": "'global'",
   *        "init": { "stats": "STATS_INIT()" },
   *        "update": { "stats": "STATS_ADD(stats, 1)" },
   *        "result": "stats"
   *      }
   *    ],
   *    "timestampField": "timestamp"
   * }
   */
  @Multiline
  private static String profileWithStats;

  /**
   * The result produced by a Profile has to be serializable within Storm. If the result is not
   * serializable the topology will crash and burn.
   *
   * This test ensures that if a profile returns a STATS object created using the STATS_INIT and
   * STATS_ADD functions, that it can be correctly serialized and persisted.
   */
  @Test
  public void testProfileWithStatsObject() throws Exception {
    uploadConfigToZookeeper(ProfilerConfig.fromJSON(profileWithStats));

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    List<String> messages = FileUtils.readLines(new File("src/test/resources/telemetry.json"));
    kafkaComponent.writeMessages(inputTopic, messages);

    assertEventually(() -> {
      // validate the measurements written by the batch profiler using `PROFILE_GET`
      // the 'window' looks up to 5 hours before the max timestamp contained in the test data
      assign("maxTimestamp", "1530978728982L");
      assign("window", "PROFILE_WINDOW('from 5 hours ago', maxTimestamp)");

      // retrieve the stats stored by the profiler
      List results = execute("PROFILE_GET('profile-with-stats', 'global', window)", List.class);
      assertTrue(results.size() > 0);
      assertTrue(results.get(0) instanceof OnlineStatisticsProvider);

    }, timeout);
  }

  /**
   * {
   *    "profiles": [
   *      {
   *        "profile": "profile-with-triage",
   *        "foreach": "'global'",
   *        "update": {
   *          "stats": "STATS_ADD(stats, 1)"
   *        },
   *        "result": {
   *          "profile": "stats",
   *          "triage": {
   *            "min": "STATS_MIN(stats)",
   *            "max": "STATS_MAX(stats)",
   *            "mean": "STATS_MEAN(stats)"
   *          }
   *        }
   *      }
   *    ],
   *    "timestampField": "timestamp"
   * }
   */
  @Multiline
  private static String profileWithTriageResult;

  private List<byte[]> outputMessages;
  @Test
  public void testProfileWithTriageResult() throws Exception {
    uploadConfigToZookeeper(ProfilerConfig.fromJSON(profileWithTriageResult));

    // start the topology and write test messages to kafka
    fluxComponent.submitTopology();
    List<String> telemetry = FileUtils.readLines(new File("src/test/resources/telemetry.json"));
    kafkaComponent.writeMessages(inputTopic, telemetry);

    // wait until the triage message is output to kafka
    assertEventually(() -> {
      outputMessages = kafkaComponent.readMessages(outputTopic);
      assertEquals(1, outputMessages.size());
    }, timeout);

    // validate the triage message
    JSONObject message = (JSONObject) new JSONParser().parse(new String(outputMessages.get(0),
        StandardCharsets.UTF_8));
    assertEquals("profile-with-triage", message.get(PROFILE_FIELD));
    assertEquals("global",              message.get(ENTITY_FIELD));
    assertEquals(76548935L,             message.get(PERIOD_ID_FIELD));
    assertEquals(1530978700000L,        message.get(PERIOD_START_FIELD));
    assertEquals(1530978720000L,        message.get(PERIOD_END_FIELD));
    assertEquals("profiler",            message.get(Constants.SENSOR_TYPE));
    assertEquals("true",                message.get(ALERT_FIELD));
    assertEquals(1.0,                   message.get("min"));
    assertEquals(1.0,                   message.get("max"));
    assertEquals(1.0,                   message.get("mean"));
    assertTrue(message.containsKey(TIMESTAMP_FIELD));
    assertTrue(message.containsKey(Constants.GUID));
  }

  private static String getMessage(String ipSource, long timestamp) {
    return new MessageBuilder()
            .withField("ip_src_addr", ipSource)
            .withField("timestamp", timestamp)
            .build()
            .toJSONString();
  }

  @BeforeClass
  public static void setupBeforeClass() throws UnableToStartException {

    // create some messages that contain a timestamp - a really old timestamp; close to 1970
    message1 = getMessage(entity, startAt);
    message2 = getMessage(entity, startAt + 100);
    message3 = getMessage(entity, startAt + (windowDurationMillis * 2));

    // storm topology properties
    final Properties topologyProperties = new Properties() {{

      // storm settings
      setProperty("profiler.workers", "1");
      setProperty("profiler.acker.executors", "0");
      setProperty("profiler.spout.parallelism", "1");
      setProperty("profiler.splitter.parallelism", "1");
      setProperty("profiler.builder.parallelism", "1");
      setProperty("profiler.hbase.writer.parallelism", "1");
      setProperty("profiler.kafka.writer.parallelism", "1");

      setProperty(Config.TOPOLOGY_AUTO_CREDENTIALS, "[]");
      setProperty(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, "60");
      setProperty(Config.TOPOLOGY_MAX_SPOUT_PENDING, "100000");

      // ensure tuples are serialized during the test, otherwise serialization problems
      // will not be found until the topology is run on a cluster with multiple workers
      setProperty(Config.TOPOLOGY_TESTING_ALWAYS_TRY_SERIALIZE, "true");
      setProperty(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, "false");
      setProperty(Config.TOPOLOGY_KRYO_REGISTER, kryoSerializers);

      // kafka settings
      setProperty("profiler.input.topic", inputTopic);
      setProperty("profiler.output.topic", outputTopic);
      setProperty("kafka.start", "EARLIEST");
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

    // global properties
    Map<String, Object> global = new HashMap<String, Object>() {{
      put(PROFILER_HBASE_TABLE.getKey(), tableName);
      put(PROFILER_COLUMN_FAMILY.getKey(), columnFamily);
      put(PROFILER_HBASE_TABLE_PROVIDER.getKey(), MockHBaseTableProvider.class.getName());

      // client needs to use the same period duration
      put(PROFILER_PERIOD.getKey(), Long.toString(periodDurationMillis));
      put(PROFILER_PERIOD_UNITS.getKey(), "MILLISECONDS");

      // client needs to use the same salt divisor
      put(PROFILER_SALT_DIVISOR.getKey(), saltDivisor);
    }};

    // create the stellar execution environment
    executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(GetProfile.class)
                    .withClass(FixedLookback.class)
                    .withClass(WindowLookback.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
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
   * @param profilerConfig The Profiler configuration.
   * @throws Exception
   */
  public void uploadConfigToZookeeper(ProfilerConfig profilerConfig) throws Exception {
    configUploadComponent
            .withProfilerConfiguration(profilerConfig)
            .update();
  }

  /**
   * Assign a value to the result of an expression.
   *
   * @param var The variable to assign.
   * @param expression The expression to execute.
   */
  private void assign(String var, String expression) {
    executor.assign(var, expression, Collections.emptyMap());
  }

  /**
   * Execute a Stellar expression.
   *
   * @param expression The Stellar expression to execute.
   * @param clazz
   * @param <T>
   * @return The result of executing the Stellar expression.
   */
  private <T> T execute(String expression, Class<T> clazz) {
    T results = executor.execute(expression, Collections.emptyMap(), clazz);
    LOG.debug("{} = {}", expression, results);
    return results;
  }
}
