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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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

  @Rule
  public TestName testName = new TestName();

  @BeforeClass
  public static void setupExecutor() {
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterClass
  public static void tearDownExecutor() {
    if(executor != null && !executor.isShutdown()) {
      executor.shutdown();
    }
  }

  @BeforeClass
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

  @BeforeClass
  public static void setupFunctionResolver() {

    // used when executing Stellar expressions
    functionResolver = new SimpleFunctionResolver()
            .withClass(KafkaFunctions.KafkaGet.class)
            .withClass(KafkaFunctions.KafkaPut.class)
            .withClass(KafkaFunctions.KafkaProps.class)
            .withClass(KafkaFunctions.KafkaTail.class)
            .withClass(MapFunctions.MapGet.class);
  }

  @Before
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

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    runner.stop();
  }

  @After
  public void tearDown() {
    runner.reset();
  }

  /**
   * KAFKA_PUT should be able to write one message to a topic.
   * KAFKA_GET should be able to read one message from a topic.
   */
  @Test
  public void testKafkaPut() {

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
    variables.put("topic", topicName);

    // put a message onto the topic
    run("KAFKA_PUT(topic, [message1])");

    // get a message from the topic
    Object actual = run("KAFKA_GET(topic)");

    // validate
    assertEquals(Collections.singletonList(message1), actual);
  }

  /**
   * KAFKA_PUT should be able to write multiple messages passed as a List.
   * KAFKA_GET should be able to read multiple messages at once.
   */
  @Test
  public void testKafkaPutThenGetWithMultipleMessages() {

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
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
  public void testKafkaGetWithSequentialReads() {

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
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
  public void testKafkaGetWithNonExistentTopic() {

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
    variables.put("topic", topicName);

    // no more messages left to read
    assertEquals(Collections.emptyList(), run("KAFKA_GET(topic, 1)"));
  }

  /**
   * KAFKA_TAIL should return new messages from the end of a topic.
   */
  @Test
  public void testKafkaTail() throws Exception {

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
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
  public void testKafkaTailNone() {

    // shorten the max wait time so we do not have to wait so long
    global.put(KafkaFunctions.MAX_WAIT_PROPERTY, 2000);

    // use a unique topic name for this test
    final String topicName = testName.getMethodName();
    variables.put("topic", topicName);

    // put multiple messages onto the topic
    run("KAFKA_PUT(topic, [message1, message2, message3])");

    // no messages to read as KAFKA_TAIL should "seek to end" of the topic
    assertEquals(Collections.emptyList(), run("KAFKA_TAIL(topic, 1)"));
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
