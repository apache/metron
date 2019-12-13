/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.management;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.apache.zookeeper.KeeperException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.Constants.ErrorFields.*;
import static org.apache.metron.common.Constants.Fields.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ParserFunctions} class.
 */
public class ParserFunctionsTest {

  static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  FunctionResolver functionResolver;
  Map<String, Object> variables;
  Context context = null;
  StellarStatefulExecutor executor;

  @BeforeEach
  public void setup() {
    variables = new HashMap<>();
    functionResolver = new SimpleFunctionResolver()
            .withClass(ParserFunctions.ParseFunction.class)
            .withClass(ParserFunctions.InitializeFunction.class)
            .withClass(ParserFunctions.ConfigFunction.class);
    context = new Context.Builder().build();
    executor = new DefaultStellarStatefulExecutor(functionResolver, context);
  }

  /**
   * {
   *  "dns": {
   *  "ts":1402308259.609,
   *  "uid":"CuJT272SKaJSuqO0Ia",
   *  "id.orig_h":"10.122.196.204",
   *  "id.orig_p":33976,
   *  "id.resp_h":"144.254.71.184",
   *  "id.resp_p":53,
   *  "proto":"udp",
   *  "trans_id":62418,
   *  "query":"www.cisco.com",
   *  "qclass":1,
   *  "qclass_name":"C_INTERNET",
   *  "qtype":28,
   *  "qtype_name":"AAAA",
   *  "rcode":0,
   *  "rcode_name":"NOERROR",
   *  "AA":true,
   *  "TC":false,
   *  "RD":true,
   *  "RA":true,
   *  "Z":0,
   *  "answers":["www.cisco.com.akadns.net","origin-www.cisco.com","2001:420:1201:2::a"],
   *  "TTLs":[3600.0,289.0,14.0],
   *  "rejected":false
   *  }
   * }
   */
  @Multiline
  public String broMessage;

  /**
   * {
   *  "parserClassName":"org.apache.metron.parsers.bro.BasicBroParser",
   *  "filterClassName":"org.apache.metron.parsers.filters.StellarFilter",
   *  "sensorTopic":"bro"
   * }
   */
  @Multiline
  private String broParserConfig;

  @Test
  public void testParseBroMessage() {
    // initialize the parser with the sensor config
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    // parse the message
    set("message", broMessage);
    List<JSONObject> messages = execute("PARSER_PARSE(parser, message)", List.class);

    // validate the parsed message
    assertEquals(1, messages.size());
    JSONObject message = messages.get(0);
    assertEquals("bro", message.get(Constants.SENSOR_TYPE));
    assertEquals("10.122.196.204", message.get(SRC_ADDR.getName()));
    assertEquals(33976L, message.get(SRC_PORT.getName()));
    assertEquals("144.254.71.184", message.get(DST_ADDR.getName()));
    assertEquals(53L, message.get(DST_PORT.getName()));
    assertEquals("dns", message.get("protocol"));
  }

  @Test
  public void testParseMultipleMessages() {
    // initialize the parser with the sensor config
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    // parse the message
    set("msg1", broMessage);
    set("msg2", broMessage);
    set("msg3", broMessage);
    List<JSONObject> messages = execute("PARSER_PARSE(parser, [msg1, msg2, msg3])", List.class);

    // expect a parsed message
    assertEquals(3, messages.size());
    for(JSONObject message: messages) {
      assertEquals("bro", message.get(Constants.SENSOR_TYPE));
      assertTrue(message.containsKey(Constants.GUID));
      assertEquals("10.122.196.204", message.get(SRC_ADDR.getName()));
      assertEquals(33976L, message.get(SRC_PORT.getName()));
      assertEquals("144.254.71.184", message.get(DST_ADDR.getName()));
      assertEquals(53L, message.get(DST_PORT.getName()));
      assertEquals("dns", message.get("protocol"));
    }
  }

  @Test
  public void testParseInvalidMessage() {
    // initialize the parser with the sensor config
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    // parse the message
    String invalidMessage = "{ this is an invalid message }}";
    set("message", invalidMessage);
    List<JSONObject> messages = execute("PARSER_PARSE(parser, message)", List.class);

    // validate the parsed message
    assertEquals(1, messages.size());

    // expect an error message to be returned
    JSONObject error = messages.get(0);
    assertEquals(invalidMessage, error.get("raw_message"));
    assertEquals(Constants.ERROR_TYPE, error.get(Constants.SENSOR_TYPE));
    assertEquals("parser_error", error.get(ERROR_TYPE.getName()));
    assertTrue(error.containsKey(MESSAGE.getName()));
    assertTrue(error.containsKey(EXCEPTION.getName()));
    assertTrue(error.containsKey(STACK.getName()));
    assertTrue(error.containsKey(ERROR_HASH.getName()));
    assertTrue(error.containsKey(Constants.GUID));
  }

  @Test
  public void testParseSomeGoodSomeBadMessages() {
    // initialize the parser with the sensor config
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    // parse the message
    String invalidMessage = "{ this is an invalid message }}";
    set("msg1", broMessage);
    set("msg2", invalidMessage);
    List<JSONObject> messages = execute("PARSER_PARSE(parser, [msg1, msg2])", List.class);

    // expect 2 messages to be returned - 1 success and 1 error
    assertEquals(2, messages.size());
    assertEquals(1, messages.stream().filter(msg -> isBro(msg)).count());
    assertEquals(1, messages.stream().filter(msg -> isError(msg)).count());
  }

  @Test
  public void testConfig() throws Exception {
    // initialize the parser
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    String config = execute("PARSER_CONFIG(parser)", String.class);
    assertNotNull(config);
    assertNotNull(SensorParserConfig.fromBytes(config.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testInitFromString() throws Exception {
    set("configAsString", broParserConfig);
    StellarParserRunner runner = execute("PARSER_INIT('bro', configAsString)", StellarParserRunner.class);

    assertNotNull(runner);
    SensorParserConfig actual = runner.getParserConfigurations().getSensorParserConfig("bro");
    SensorParserConfig expected = SensorParserConfig.fromBytes(broParserConfig.getBytes(
        StandardCharsets.UTF_8));
    assertEquals(expected, actual);
  }

  @Test
  public void testInitFromMap() throws Exception {
    Map<String, Object> configAsMap = (JSONObject) new JSONParser().parse(broParserConfig);
    set("configAsMap", configAsMap);
    StellarParserRunner runner = execute("PARSER_INIT('bro', configAsMap)", StellarParserRunner.class);

    assertNotNull(runner);
    SensorParserConfig actual = runner.getParserConfigurations().getSensorParserConfig("bro");
    SensorParserConfig expected = SensorParserConfig.fromBytes(broParserConfig.getBytes(
        StandardCharsets.UTF_8));
    assertEquals(expected, actual);
  }

  @Test
  public void testInitFromInvalidValue() {
    assertThrows(ParseException.class, () -> execute("PARSER_INIT('bro', 22)", StellarParserRunner.class));
  }

  @Test
  public void testInitFromZookeeper() throws Exception {
    byte[] configAsBytes = broParserConfig.getBytes(StandardCharsets.UTF_8);
    CuratorFramework zkClient = zkClientForPath("/metron/topology/parsers/bro", configAsBytes);
    context.addCapability(Context.Capabilities.ZOOKEEPER_CLIENT, () -> zkClient);

    StellarParserRunner runner = execute("PARSER_INIT('bro')", StellarParserRunner.class);

    assertNotNull(runner);
    SensorParserConfig actual = runner.getParserConfigurations().getSensorParserConfig("bro");
    SensorParserConfig expected = SensorParserConfig.fromBytes(broParserConfig.getBytes(
        StandardCharsets.UTF_8));
    assertEquals(expected, actual);
  }

  @Test
  public void testInitMissingFromZookeeper() throws Exception {
    // there is no config for 'bro' in zookeeper
    CuratorFramework zkClient = zkClientMissingPath("/metron/topology/parsers/bro");
    context.addCapability(Context.Capabilities.ZOOKEEPER_CLIENT, () -> zkClient);

    assertThrows(ParseException.class, () -> execute("PARSER_INIT('bro')", StellarParserRunner.class));
  }

  /**
   * Create a mock Zookeeper client that returns a value for a given path.
   *
   * @param path The path within Zookeeper that will be requested.
   * @param value The value to return when the path is requested.
   * @return The mock Zookeeper client.
   */
  private CuratorFramework zkClientForPath(String path, byte[] value) throws Exception {
    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(path)).thenReturn(value);

    CuratorFramework zkClient = mock(CuratorFramework.class);
    when(zkClient.getData()).thenReturn(getDataBuilder);

    return zkClient;
  }

  /**
   * Create a mock Zookeeper client that will indicate the given path does not exist.
   *
   * @param path The path that will 'not exist'.
   * @return The mock Zookeeper client.
   */
  private CuratorFramework zkClientMissingPath(String path) throws Exception {

    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(path)).thenThrow(new KeeperException.NoNodeException(path));

    CuratorFramework zkClient = mock(CuratorFramework.class);
    when(zkClient.getData()).thenReturn(getDataBuilder);

    return zkClient;
  }

  private boolean isError(JSONObject message) {
    String sensorType = String.class.cast(message.get(Constants.SENSOR_TYPE));
    return Constants.ERROR_TYPE.equals(sensorType);
  }

  private boolean isBro(JSONObject message) {
    String sensorType = String.class.cast(message.get(Constants.SENSOR_TYPE));
    return "bro".equals(sensorType);
  }

  /**
   * Set the value of a variable.
   *
   * @param var The variable to assign.
   * @param value The value to assign.
   */
  private void set(String var, Object value) {
    executor.assign(var, value);
  }

  /**
   * Assign a value to the result of an expression.
   *
   * @param var The variable to assign.
   * @param expression The expression to execute.
   */
  private Object assign(String var, String expression) {
    executor.assign(var, expression, Collections.emptyMap());
    return executor.getState().get(var);
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
