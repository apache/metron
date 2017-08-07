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

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


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

  private static Map<String, Object> variables = new HashMap<>();
  private static ZKServerComponent zkServerComponent;
  private static KafkaComponent kafkaComponent;
  private static ComponentRunner runner;
  private static Properties global;

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

  @Before
  public void setup() {

    // messages that will be read/written during the tests
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
   * Write one message, read one message.
   */
  @Test
  public void testOneMessage() {
    run("KAFKA_PUT('topic1', [message1])");
    Object actual = run("KAFKA_GET('topic1')");
    assertEquals(Collections.singletonList(message1), actual);
  }

  /**
   * Multiple messages can be read with a single call.
   */
  @Test
  public void testMultipleMessages() {
    run("KAFKA_PUT('topic2', [message1, message2, message3])");
    Object actual = run("KAFKA_GET('topic2', 3)");

    List<String> expected = new ArrayList<String>() {{
      add(message1);
      add(message2);
      add(message3);
    }};
    assertEquals(expected, actual);
  }

  /**
   * Does the client maintain the consumer offset correctly?
   */
  @Test
  public void testConsumerOffsets() {
    run("KAFKA_PUT('topic3', [message1, message2, message3])");

    // the offsets must be maintained correctly for us to read each message, in order,
    // sequentially across separate calls to KAFKA_GET
    assertEquals(Collections.singletonList(message1), run("KAFKA_GET('topic3', 1)"));
    assertEquals(Collections.singletonList(message2), run("KAFKA_GET('topic3', 1)"));
    assertEquals(Collections.singletonList(message3), run("KAFKA_GET('topic3', 1)"));
  }

  /**
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
   * @param expr The expression to run.
   */
  private Object run(String expr) {

    // make the global properties available to the function
    Context context = new Context.Builder()
            .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
            .build();

    FunctionResolver functionResolver = new SimpleFunctionResolver()
            .withClass(KafkaFunctions.KafkaGet.class)
            .withClass(KafkaFunctions.KafkaPut.class)
            .withClass(KafkaFunctions.KafkaProps.class)
            .withClass(KafkaFunctions.KafkaTail.class);

    StellarProcessor processor = new StellarProcessor();
    return processor.parse(expr, new DefaultVariableResolver(x -> variables.get(x),x -> variables.containsKey(x)), functionResolver, context);
  }

}
