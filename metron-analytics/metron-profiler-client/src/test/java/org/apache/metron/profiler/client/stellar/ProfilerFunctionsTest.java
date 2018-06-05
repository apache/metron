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

package org.apache.metron.profiler.client.stellar;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.profiler.StandAloneProfiler;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.functions.resolver.SimpleFunctionResolver;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD;
import static org.apache.metron.profiler.client.stellar.ProfilerClientConfig.PROFILER_PERIOD_UNITS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests the ProfilerFunctions class.
 */
public class ProfilerFunctionsTest {

  /**
   * {
   *    "ip_src_addr": "10.0.0.1",
   *    "ip_dst_addr": "10.0.0.2",
   *    "source.type": "test",
   * }
   */
  @Multiline
  private String message;

  /**
   * [
   * {
   *    "ip_src_addr": "10.0.0.1",
   *    "ip_dst_addr": "10.0.0.2",
   *    "source.type": "test",
   * },
   * {
   *    "ip_src_addr": "10.0.0.1",
   *    "ip_dst_addr": "10.0.0.2",
   *    "source.type": "test",
   * },
   * {
   *    "ip_src_addr": "10.0.0.1",
   *    "ip_dst_addr": "10.0.0.2",
   *    "source.type": "test",
   * }
   * ]
   */
  @Multiline
  private String messages;

  /**
   * {
   *   "profiles": [
   *        {
   *          "profile":  "hello-world",
   *          "foreach":  "ip_src_addr",
   *          "init":     { "count": 0 },
   *          "update":   { "count": "count + 1" },
   *          "result":   "count"
   *        }
   *   ]
   * }
   */
  @Multiline
  private String helloWorldProfilerDef;

  private static final long periodDuration = 15;
  private static final String periodUnits = "MINUTES";
  private StellarStatefulExecutor executor;
  private Map<String, Object> state;

  private <T> T run(String expression, Class<T> clazz) {
    return executor.execute(expression, state, clazz);
  }

  @Before
  public void setup() {
    state = new HashMap<>();

    // global properties
    Map<String, Object> global = new HashMap<String, Object>() {{
      put(PROFILER_PERIOD.getKey(), Long.toString(periodDuration));
      put(PROFILER_PERIOD_UNITS.getKey(), periodUnits.toString());
    }};

    // create the stellar execution environment
    executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(ProfilerFunctions.ProfilerInit.class)
                    .withClass(ProfilerFunctions.ProfilerApply.class)
                    .withClass(ProfilerFunctions.ProfilerFlush.class),
            new Context.Builder()
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> global)
                    .build());
  }

  @Test
  public void testProfilerInitNoProfiles() {
    state.put("config", "{ \"profiles\" : [] }");
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    assertNotNull(profiler);
    assertEquals(0, profiler.getProfileCount());
    assertEquals(0, profiler.getMessageCount());
    assertEquals(0, profiler.getRouteCount());
  }

  @Test
  public void testProfilerInitWithProfiles() {
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    assertNotNull(profiler);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(0, profiler.getMessageCount());
    assertEquals(0, profiler.getRouteCount());
  }

  @Test(expected = ParseException.class)
  public void testProfilerInitNoArgs() {
    run("PROFILER_INIT()", StandAloneProfiler.class);
  }

  @Test(expected = ParseException.class)
  public void testProfilerInitInvalidArg() {
    run("PROFILER_INIT({ \"invalid\": 2 })", StandAloneProfiler.class);
  }

  @Test
  public void testProfilerInitWithNoGlobalConfig() {
    state.put("config", helloWorldProfilerDef);
    String expression = "PROFILER_INIT(config)";

    // use an executor with no GLOBAL_CONFIG defined in the context
    StellarStatefulExecutor executor = new DefaultStellarStatefulExecutor(
            new SimpleFunctionResolver()
                    .withClass(ProfilerFunctions.ProfilerInit.class)
                    .withClass(ProfilerFunctions.ProfilerApply.class)
                    .withClass(ProfilerFunctions.ProfilerFlush.class),
            Context.EMPTY_CONTEXT());
    StandAloneProfiler profiler = executor.execute(expression, state, StandAloneProfiler.class);

    assertNotNull(profiler);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(0, profiler.getMessageCount());
    assertEquals(0, profiler.getRouteCount());
  }

  @Test
  public void testProfilerApplyWithString() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    state.put("message", message);
    StandAloneProfiler result = run("PROFILER_APPLY(message, profiler)", StandAloneProfiler.class);

    // validate
    assertSame(profiler, result);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(1, profiler.getMessageCount());
    assertEquals(1, profiler.getRouteCount());
  }

  @Test
  public void testProfilerApplyWithJSONObject() throws Exception {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    JSONParser parser = new JSONParser();
    JSONObject jsonObject = (JSONObject) parser.parse(message);
    state.put("jsonObj", jsonObject);
    StandAloneProfiler result = run("PROFILER_APPLY(jsonObj, profiler)", StandAloneProfiler.class);

    // validate
    assertSame(profiler, result);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(1, profiler.getMessageCount());
    assertEquals(1, profiler.getRouteCount());
  }

  @Test
  public void testProfilerApplyWithMultipleMessagesInJSONString() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    state.put("messages", messages);
    StandAloneProfiler result = run("PROFILER_APPLY(messages, profiler)", StandAloneProfiler.class);

    // validate
    assertSame(profiler, result);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(3, profiler.getMessageCount());
    assertEquals(3, profiler.getRouteCount());
  }

  @Test
  public void testProfilerApplyWithListOfMessages() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    state.put("msg", message);
    StandAloneProfiler result = run("PROFILER_APPLY([msg, msg, msg], profiler)", StandAloneProfiler.class);

    // validate
    assertSame(profiler, result);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(3, profiler.getMessageCount());
    assertEquals(3, profiler.getRouteCount());
  }


  @Test
  public void testProfilerApplyWithEmptyList() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    state.put("messages", "[ ]");
    StandAloneProfiler result = run("PROFILER_APPLY(messages, profiler)", StandAloneProfiler.class);

    // validate
    assertSame(profiler, result);
    assertEquals(1, profiler.getProfileCount());
    assertEquals(0, profiler.getMessageCount());
    assertEquals(0, profiler.getRouteCount());
  }

  @Test(expected = ParseException.class)
  public void testProfilerApplyWithNoArgs() {
    run("PROFILER_APPLY()", StandAloneProfiler.class);
  }

  @Test(expected = ParseException.class)
  public void testProfilerApplyWithInvalidArg() {
    run("PROFILER_APPLY(undefined)", StandAloneProfiler.class);
  }

  @Test(expected = ParseException.class)
  public void testProfilerApplyWithNullMessage() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // there is no 'messages' variable - should throw exception
    run("PROFILER_APPLY(messages, profiler)", StandAloneProfiler.class);
  }

  @Test
  public void testProfilerFlush() {

    // initialize the profiler
    state.put("config", helloWorldProfilerDef);
    StandAloneProfiler profiler = run("PROFILER_INIT(config)", StandAloneProfiler.class);
    state.put("profiler", profiler);

    // apply a message to the profiler
    state.put("message", message);
    run("PROFILER_APPLY(message, profiler)", StandAloneProfiler.class);

    // flush the profiles
    List<Map<String, Object>> measurements = run("PROFILER_FLUSH(profiler)", List.class);

    // validate
    assertNotNull(measurements);
    assertEquals(1, measurements.size());

    Map<String, Object> measurement = measurements.get(0);
    assertEquals("hello-world", measurement.get("profile"));
    assertEquals("10.0.0.1", measurement.get("entity"));
    assertEquals(1, measurement.get("value"));
    assertEquals(Collections.emptyList(), measurement.get("groups"));
  }

  @Test(expected = ParseException.class)
  public void testProfilerFlushNoArgs() {
    run("PROFILER_FLUSH()", StandAloneProfiler.class);
  }

  @Test(expected = ParseException.class)
  public void testProfilerFlushInvalidArg() {
    run("PROFILER_FLUSH(undefined)", StandAloneProfiler.class);
  }
}
