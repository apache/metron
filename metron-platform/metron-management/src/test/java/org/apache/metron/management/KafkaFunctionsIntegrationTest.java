/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.management;

import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.functions.MapFunctions;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the KafkaFunctions class.
 *
 * Labelled as an integration test as the tests stand-up a Kafka Broker for the
 * Stellar Kafka functions to interact with.
 */
public class KafkaFunctionsIntegrationTest extends BaseIntegrationTest {

  private static final String message1 = "{ \"ip_src_addr\": \"10.0.0.1\", \"value\": 14687 }";
  private static final String message2 = "{ \"ip_src_addr\": \"10.0.0.1\", \"value\": 23 }";
  private static final String message3 = "{ \"ip_src_addr\": \"10.0.0.1\", \"value\": 29011 }";

  private static Map<String, Object> variables;
  private static ZKServerComponent zkServerComponent;
  private static KafkaComponent kafkaComponent;
  private static ComponentRunner runner;
  private static Properties global;
  private static FunctionResolver functionResolver;
  private static ExecutorService executor;

  @BeforeAll
  public static void setupExecutor() {
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterAll
  public static void tearDownExecutor() {
    if(executor != null && !executor.isShutdown()) {
      executor.shutdown();
    }
  }

  @BeforeAll
  public static void setupKafka() throws Exception {

    Properties properties = new Properties();
    zkServerComponent = getZKServerComponent(properties);
    kafkaComponent = getKafkaComponent(properties, new ArrayList<>());
    runner = new ComponentRunner.Builder()
            .withComponent("zk", zkServerComponent)
            .withComponent("kafka", kafkaComponent)
            .withMillisecondsBetweenAttempts(5000)
            .withNumRetries(5)
            .withCustomShutdownOrder(new String[]{"kafka","zk"})
            .build();
    runner.start();
  }

  @BeforeAll
  public static void setupFunctionResolver() {

    // used when executing Stellar expressions
    functionResolver = new SimpleFunctionResolver()
            .withClass(KafkaFunctions.KafkaGet.class)
            .withClass(KafkaFunctions.KafkaPut.class)
            .withClass(KafkaFunctions.KafkaProps.class)
            .withClass(KafkaFunctions.KafkaTail.class)
            .withClass(KafkaFunctions.KafkaFind.class)
            .withClass(KafkaFunctions.KafkaSeek.class)
            .withClass(MapFunctions.MapGet.class);
  }

  @BeforeEach
  public void setup() {

    // messages that will be read/written during the tests
    variables = new HashMap<>();
    variables.put("message1", message1);
    variables.put("message2", message2);
    variables.put("message3", message3);

    // global properties
    global = new Properties();
    global.put("bootstrap.servers", kafkaComponent.getBrokerList());

    // start reading from the earliest offset, which is necessary for these tests
    global.put("auto.offset.reset", "earliest");
  }

  @AfterAll
  public static void tearDownAfterClass() {
    runner.stop();
  }

  @AfterEach
  public void tearDown() {
    runner.reset();
  }

  /**
   * KAFKA_PUT should be able to write one message to a topic.
   * KAFKA_GET should be able to read one message from a topic.
   */
  @Test
  public void testKafkaPut(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put a message onto the topic
    assertEquals(1, run("KAFKA_PUT(topic, [message1])"));

    // validate the message in the topic
    assertEquals(Collections.singletonList(message1), run("KAFKA_GET(topic)"));
  }

  /**
   * KAFKA_PUT should be able to write multiple message to a topic.
   */
  @Test
  public void testKafkaPutMultipleMessages(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put a message onto the topic
    assertEquals(2, run("KAFKA_PUT(topic, [message1, message2])"));

    // validate the message in the topic
    List<String> expected = new ArrayList<String>() {{
      add(message1);
      add(message2);
    }};
    assertEquals(expected, run("KAFKA_GET(topic, 2)"));
  }

  /**
   * KAFKA_PUT should be able to write a message passed as a String, rather than a List.
   */
  @Test
  public void testKafkaPutOneMessagePassedAsString(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put a message onto the topic - the message is just a string, not a list
    run("KAFKA_PUT(topic, message1)");

    // get a message from the topic
    Object actual = run("KAFKA_GET(topic)");

    // validate
    assertEquals(Collections.singletonList(message1), actual);
  }

  /**
   * KAFKA_PUT should be able to write a message passed as a String, rather than a List.
   */
  @Test
  public void testKafkaPutWithRichView(TestInfo testInfo) {

    // configure a detailed view of each message
    global.put(KafkaFunctions.MESSAGE_VIEW_PROPERTY, KafkaFunctions.MESSAGE_VIEW_RICH);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put a message onto the topic - the message is just a string, not a list
    Object actual = run("KAFKA_PUT(topic, message1)");

    // validate
    assertTrue(actual instanceof List);
    List<Object> results = (List) actual;
    assertEquals(1, results.size());

    // expect a 'rich' view of the record
    Map<String, Object> view = (Map) results.get(0);
    assertEquals(topicName, view.get("topic"));
    assertEquals(0, view.get("partition"));
    assertEquals(0L, view.get("offset"));
    assertNotNull(view.get("timestamp"));

  }

  /**
   * KAFKA_GET should allow a user to see a detailed view of each Kafka record.
   */
  @Test
  public void testKafkaGetWithRichView(TestInfo testInfo) {

    // configure a detailed view of each message
    global.put(KafkaFunctions.MESSAGE_VIEW_PROPERTY, KafkaFunctions.MESSAGE_VIEW_RICH);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put a message onto the topic - the message is just a string, not a list
    run("KAFKA_PUT(topic, message1)");

    // get a message from the topic
    Object actual = run("KAFKA_GET(topic)");

    // validate
    assertTrue(actual instanceof List);
    List<Object> results = (List) actual;
    assertEquals(1, results.size());

    // expect a 'rich' view of the record
    Map<String, Object> view = (Map) results.get(0);
    assertNull(view.get("key"));
    assertEquals(0L, view.get("offset"));
    assertEquals(0, view.get("partition"));
    assertEquals(topicName, view.get("topic"));
    assertEquals(message1, view.get("value"));
  }

  /**
   * KAFKA_PUT should be able to write multiple messages passed as a List.
   * KAFKA_GET should be able to read multiple messages at once.
   */
  @Test
  public void testKafkaPutThenGetWithMultipleMessages(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic
    run("KAFKA_PUT(topic, [message1, message2, message3])");

    // get 3 messages from the topic
    Object actual = run("KAFKA_GET(topic, 3)");

    // validate that all 3 messages were read
    List<String> expected = new ArrayList<String>() {{
      add(message1);
      add(message2);
      add(message3);
    }};
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_GET should maintain its consumer offsets and reuse them across subsequent calls.
   *
   * Does the client maintain the consumer offset correctly?
   *
   * The offsets must be maintained correctly to read messages sequentially, in order
   * across separate executions of KAKFA_GET
   */
  @Test
  public void testKafkaGetWithSequentialReads(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic
    run("KAFKA_PUT(topic, [message1, message2, message3])");

    // read the first message
    assertEquals(Collections.singletonList(message1), run("KAFKA_GET(topic, 1)"));

    // pick-up from where we left off and read the second message
    assertEquals(Collections.singletonList(message2), run("KAFKA_GET(topic, 1)"));

    // pick-up from where we left off and read the third message
    assertEquals(Collections.singletonList(message3), run("KAFKA_GET(topic, 1)"));

    // no more messages left to read
    assertEquals(Collections.emptyList(), run("KAFKA_GET(topic, 1)"));
  }

  /**
   * KAFKA_GET should return nothing if a topic does not exist
   */
  @Test
  public void testKafkaGetWithNonExistentTopic(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // no more messages left to read
    assertEquals(Collections.emptyList(), run("KAFKA_GET(topic, 1)"));
  }

  /**
   * KAFKA_TAIL should return new messages from the end of a topic.
   */
  @Test
  public void testKafkaTail(TestInfo testInfo) throws Exception {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic; KAFKA tail should NOT retrieve these
    run("KAFKA_PUT(topic, [message2, message2, message2])");

    // get a message from the topic; will block until messages arrive
    Future<Object> tailFuture = runAsync("KAFKA_TAIL(topic, 1)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message1])"));

    // expect to receive message1, which were added to the topic while KAFKA_TAIL was running
    Object actual = tailFuture.get(10, TimeUnit.SECONDS);
    List<String> expected = Collections.singletonList(message1);
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_TAIL should always seek to end of the topic.  If no messages arrives after the 'seek to end'
   * then no messages will be returned.
   */
  @Test
  public void testKafkaTailNone(TestInfo testInfo) {

    // shorten the max wait time so we do not have to wait so long
    global.put(KafkaFunctions.MAX_WAIT_PROPERTY, 2000);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic
    run("KAFKA_PUT(topic, [message1, message2, message3])");

    // no messages to read as KAFKA_TAIL should "seek to end" of the topic
    assertEquals(Collections.emptyList(), run("KAFKA_TAIL(topic, 1)"));
  }

  /**
   * KAFKA_TAIL should allow a user to see a rich view of each Kafka record.
   */
  @Test
  public void testKafkaTailWithRichView(TestInfo testInfo) throws Exception {

    // configure a detailed view of each message
    global.put(KafkaFunctions.MESSAGE_VIEW_PROPERTY, KafkaFunctions.MESSAGE_VIEW_RICH);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic; KAFKA tail should NOT retrieve these
    run("KAFKA_PUT(topic, [message2, message2, message2])");

    // get a message from the topic; will block until messages arrive
    Future<Object> tailFuture = runAsync("KAFKA_TAIL(topic, 1)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message1])"));

    // wait for KAFKA_TAIL to complete
    Object actual = tailFuture.get(10, TimeUnit.SECONDS);

    // validate
    assertTrue(actual instanceof List);
    List<Object> results = (List) actual;
    assertEquals(1, results.size());

    // expect a 'rich' view of the record
    Map<String, Object> view = (Map) results.get(0);
    assertNull(view.get("key"));
    assertEquals(0, view.get("partition"));
    assertEquals(topicName, view.get("topic"));
    assertEquals(message1, view.get("value"));
    assertNotNull(view.get("offset"));
  }

  /**
   * KAFKA_PROPS should return the set of properties used to configure the Kafka consumer
   *
   * The properties used for the KAFKA_* functions are calculated by compiling the default, global and user
   * properties into a single set of properties.  The global properties should override any default properties.
   */
  @Test
  public void testKafkaPropsWithGlobalOverride() {

    // setup - override a key in the global properties
    final String overriddenKey = "bootstrap.servers";
    final String expected = "foo.global.override.com:9092";
    global.setProperty(overriddenKey, expected);

    // validate - ensure the global overrides the default property value
    Map<String, String> properties = (Map<String, String>) run("KAFKA_PROPS()");
    assertEquals(expected, properties.get(overriddenKey));
  }

  /**
   * KAFKA_PROPS should allow the global properties to be overridden
   *
   * The properties used for the KAFKA_* functions are calculated by compiling the default, global and user
   * properties into a single set of properties.  The user properties should override any default or global properties.
   */
  @Test
  public void testKafkaPropsWithUserOverride() {

    // setup - override a key in the global properties
    final String overriddenKey = "bootstrap.servers";
    global.setProperty(overriddenKey, "foo.global.override.com:9092");

    // setup - override the same key in the user properties
    final String expected = "foo.user.override.com:9092";
    String expression = String.format("KAFKA_PROPS({ '%s' : '%s' })", overriddenKey, expected);

    // validate - ensure the user properties override the global and defaults
    Map<String, String> properties = (Map<String, String>) run(expression);
    assertEquals(expected, properties.get(overriddenKey));
  }

  /**
   * KAFKA_FIND should only return messages that satisfy a filter expression.
   */
  @Test
  public void testKafkaFind(TestInfo testInfo) throws Exception {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // find all messages satisfying the filter expression
    Future<Object> future = runAsync("KAFKA_FIND(topic, m -> MAP_GET('value', m) == 23)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message2])"));

    // only expect `message2` where value == 23 to be returned
    Object actual = future.get(10, TimeUnit.SECONDS);
    List<String> expected = Collections.singletonList(message2);
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_FIND should return no messages, if none match the filter expression.
   */
  @Test
  public void testKafkaFindNone(TestInfo testInfo) throws Exception {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // find all messages satisfying the filter expression
    Future<Object> future = runAsync("KAFKA_FIND(topic, m -> false)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message1])"));

    // no messages satisfy the filter expression
    Object actual = future.get(10, TimeUnit.SECONDS);
    List<String> expected = Collections.emptyList();
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_FIND should allow a user to see a detailed view of each Kafka record.
   */
  @Test
  public void testKafkaFindWithRichView(TestInfo testInfo) throws Exception {

    // configure a detailed view of each message
    global.put(KafkaFunctions.MESSAGE_VIEW_PROPERTY, KafkaFunctions.MESSAGE_VIEW_RICH);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // find all messages satisfying the filter expression
    Future<Object> future = runAsync("KAFKA_FIND(topic, m -> MAP_GET('value', m) == 23)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message2])"));

    // validate
    Object actual = future.get(10, TimeUnit.SECONDS);
    assertTrue(actual instanceof List);
    List<Object> results = (List) actual;
    assertEquals(1, results.size());

    // expect a 'rich' view of the record
    Map<String, Object> view = (Map) results.get(0);
    assertNull(view.get("key"));
    assertNotNull(view.get("offset"));
    assertEquals(0, view.get("partition"));
    assertEquals(topicName, view.get("topic"));
    assertEquals(message2, view.get("value"));
  }

  /**
   * KAFKA_FIND should return no more messages than its limit.
   */
  @Test
  public void testKafkaFindMultiple(TestInfo testInfo) throws Exception {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // find all messages satisfying the filter expression
    Future<Object> future = runAsync("KAFKA_FIND(topic, m -> true, 2)");

    // put 10 messages onto the topic for KAFKA_TAIL to grab
    runAsyncAndWait(Collections.nCopies(10, "KAFKA_PUT(topic, [message2])"));

    // all messages should satisfy the filter
    List<String> expected = new ArrayList<String>() {{
      add(message2);
      add(message2);
    }};
    Object actual = future.get(10, TimeUnit.SECONDS);
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_FIND should wait no more than a maximum time before returning, even if no matching
   * messages are found.
   */
  @Test
  public void testKafkaFindExceedsMaxWait(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // write all 3 messages to the topic
    run("KAFKA_PUT(topic, [message1, message2, message3])");

    // execute the test - none of the messages satisfy the filter
    long before = System.currentTimeMillis();
    Object actual = run("KAFKA_FIND(topic, m -> false, 10, { 'stellar.kafka.max.wait.millis': 1000 })");

    // expect not to have waited more than roughly 1000 millis
    long wait = System.currentTimeMillis() - before;
    assertTrue(wait < 2 * 1000, "Expected wait not to exceed max wait; actual wait = " + wait);

    // expect no messages
    List<String> expected = Collections.emptyList();
    assertEquals(expected, actual);
  }

  /**
   * KAFKA_SEEK should return the message at a given offset.
   */
  @Test
  public void testKafkaSeek(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put 3 messages into the topic
    run("KAFKA_PUT(topic, [ message1, message2, message3 ])");
    {
      // get the 3rd message from the topic
      Object actual = run("KAFKA_SEEK(topic, 0, 2)");
      assertEquals(message3, actual);
    }
    {
      // get the 2nd message from the topic
      Object actual = run("KAFKA_SEEK(topic, 0, 1)");
      assertEquals(message2, actual);
    }
    {
      // get the 1st message from the topic
      Object actual = run("KAFKA_SEEK(topic, 0, 0)");
      assertEquals(message1, actual);
    }
  }

  /**
   * KAFKA_SEEK should return null if the offset does not exist
   */
  @Test
  public void testKafkaSeekToMissingOffset(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put 3 messages into the topic
    run("KAFKA_PUT(topic, [ message1, message2, message3 ])");

    // get the 3rd message from the topic
    Object actual = run("KAFKA_SEEK(topic, 0, 9999)");
    assertNull(actual);
  }

  /**
   * KAFKA_SEEK should return null if the partition does not exist
   */
  @Test
  public void testKafkaSeekToMissingPartition(TestInfo testInfo) {

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    // put 3 messages into the topic
    run("KAFKA_PUT(topic, [ message1, message2, message3 ])");

    // get the 3rd message from the topic
    Object actual = run("KAFKA_SEEK(topic, 99999, 0)");
    assertNull(actual);
  }

  /**
   * KAFKA_SEEK should allow a user to see a detailed view of each Kafka record.
   */
  @Test
  public void testKafkaSeekWithRichView(TestInfo testInfo) {

    // configure a detailed view of each message
    global.put(KafkaFunctions.MESSAGE_VIEW_PROPERTY, KafkaFunctions.MESSAGE_VIEW_RICH);

    // use a unique topic name for this test
    final String topicName = testInfo.getTestMethod().get().getName();
    variables.put("topic", topicName);

    run("KAFKA_PUT(topic, [ message1, message2, message3 ])");
    Object actual = run("KAFKA_SEEK(topic, 0, 0)");

    // expect a 'rich' view of the record
    assertTrue(actual instanceof Map);
    Map<String, Object> view = (Map) actual;
    assertNull(view.get("key"));
    assertNotNull(view.get("offset"));
    assertEquals(0, view.get("partition"));
    assertEquals(topicName, view.get("topic"));
    assertEquals(message1, view.get("value"));
  }

  /**
   * Runs a Stellar expression.
   * @param expression The expression to run.
   */
  private Object run(String expression) {

    // make the global properties available to the function
    Context context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .build();

    // execute the expression
    StellarProcessor processor = new StellarProcessor();
    return processor.parse(expression,
            new DefaultVariableResolver(
                    x -> variables.get(x),
                    x -> variables.containsKey(x)),
            functionResolver,
            context);
  }

  /**
   * Runs a Stellar expression asynchronously.
   *
   * <p>Does not block until the expression completes.
   *
   * @param expression The expression to run.
   * @return The result of executing the expression.
   */
  private Future<Object> runAsync(String expression) {
    return executor.submit(() -> run(expression));
  }

  /**
   * Runs a set of Stellar expression asynchronously and waits
   * for each to complete before returning.
   *
   * @param expressions The expressions to complete.
   * @throws Exception
   */
  private void runAsyncAndWait(Iterable<String> expressions) throws Exception {

    // put multiple messages onto the topic asynchronously for KAFKA_TAIL to grab
    List<Future<Object>> putFutures = new ArrayList<>();
    for(String expression: expressions) {
      Future<Object> future = runAsync(expression);
      putFutures.add(future);
    }

    // wait for the puts to complete
    for(Future<Object> future: putFutures) {
      future.get(5, TimeUnit.SECONDS);
    }
  }
}
