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
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.functions.resolver.FunctionResolver;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.common.Constants.ErrorFields.ERROR_HASH;
import static org.apache.metron.common.Constants.ErrorFields.ERROR_TYPE;
import static org.apache.metron.common.Constants.ErrorFields.EXCEPTION;
import static org.apache.metron.common.Constants.ErrorFields.MESSAGE;
import static org.apache.metron.common.Constants.ErrorFields.STACK;
import static org.apache.metron.common.Constants.Fields.DST_ADDR;
import static org.apache.metron.common.Constants.Fields.DST_PORT;
import static org.apache.metron.common.Constants.Fields.SRC_ADDR;
import static org.apache.metron.common.Constants.Fields.SRC_PORT;

/**
 * Tests the {@link ParserFunctions} class.
 */
public class ParserFunctionsTest {

  static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  FunctionResolver functionResolver;
  Map<String, Object> variables;
  Context context = null;
  StellarStatefulExecutor executor;

  @Before
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
    Assert.assertEquals(1, messages.size());
    JSONObject message = messages.get(0);
    Assert.assertEquals("bro", message.get(Constants.SENSOR_TYPE));
    Assert.assertEquals("10.122.196.204", message.get(SRC_ADDR.getName()));
    Assert.assertEquals(33976L, message.get(SRC_PORT.getName()));
    Assert.assertEquals("144.254.71.184", message.get(DST_ADDR.getName()));
    Assert.assertEquals(53L, message.get(DST_PORT.getName()));
    Assert.assertEquals("dns", message.get("protocol"));
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
    Assert.assertEquals(3, messages.size());
    for(JSONObject message: messages) {
      Assert.assertEquals("bro", message.get(Constants.SENSOR_TYPE));
      Assert.assertTrue(message.containsKey(Constants.GUID));
      Assert.assertEquals("10.122.196.204", message.get(SRC_ADDR.getName()));
      Assert.assertEquals(33976L, message.get(SRC_PORT.getName()));
      Assert.assertEquals("144.254.71.184", message.get(DST_ADDR.getName()));
      Assert.assertEquals(53L, message.get(DST_PORT.getName()));
      Assert.assertEquals("dns", message.get("protocol"));
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
    Assert.assertEquals(1, messages.size());

    // expect an error message to be returned
    JSONObject error = messages.get(0);
    Assert.assertEquals(invalidMessage, error.get("raw_message"));
    Assert.assertEquals(Constants.ERROR_TYPE, error.get(Constants.SENSOR_TYPE));
    Assert.assertEquals("parser_error", error.get(ERROR_TYPE.getName()));
    Assert.assertTrue(error.containsKey(MESSAGE.getName()));
    Assert.assertTrue(error.containsKey(EXCEPTION.getName()));
    Assert.assertTrue(error.containsKey(STACK.getName()));
    Assert.assertTrue(error.containsKey(ERROR_HASH.getName()));
    Assert.assertTrue(error.containsKey(Constants.GUID));
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
    Assert.assertEquals(2, messages.size());
    Assert.assertEquals(1, messages.stream().filter(msg -> isBro(msg)).count());
    Assert.assertEquals(1, messages.stream().filter(msg -> isError(msg)).count());
  }

  @Test
  public void testConfig() throws Exception {
    // initialize the parser
    set("config", broParserConfig);
    assign("parser", "PARSER_INIT('bro', config)");

    String config = execute("PARSER_CONFIG(parser)", String.class);
    Assert.assertNotNull(config);
    Assert.assertNotNull(SensorParserConfig.fromBytes(config.getBytes()));
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
