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

package org.apache.metron.profiler;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.clock.Clock;
import org.apache.metron.profiler.clock.FixedClock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.metron.common.utils.ConversionUtils.convert;
import static org.junit.Assert.assertEquals;

/**
 * Tests the ProfileBuilder class.
 */
public class ProfileBuilderTest {

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20",
   *   "value": 100
   * }
   */
  @Multiline
  private String input;
  private JSONObject message;
  private ProfileBuilder builder;
  private ProfileConfig definition;

  @Before
  public void setup() throws Exception {
    message = (JSONObject) new JSONParser().parse(input);
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "init": {
   *     "x": "100",
   *     "y": "200"
   *   },
   *   "result": "x + y"
   * }
   */
  @Multiline
  private String testInitProfile;

  /**
   * Ensure that the 'init' block is executed correctly.
   */
  @Test
  public void testInit() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testInitProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    builder.apply(message);
    ProfileMeasurement m = builder.flush();

    // validate that x = 100, y = 200
    assertEquals(100 + 200, (int) convert(m.getValue(), Integer.class));
  }

  /**
   * The 'init' block is executed only when the first message is received.  If no message
   * has been received, the 'init' block will not be executed.
   */
  @Test
  public void testInitWithNoMessage() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testInitProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    ProfileMeasurement m = builder.flush();

    // validate that x = 0 and y = 0 as no initialization occurred
    assertEquals(0, (int) convert(m.getValue(), Integer.class));
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "init": {
   *     "x": "0",
   *     "y": "0"
   *   },
   *   "update": {
   *     "x": "x + 1",
   *     "y": "y + 2"
   *   },
   *   "result": "x + y"
   * }
   */
  @Multiline
  private String testUpdateProfile;

  /**
   * Ensure that the 'update' expressions are executed for each message applied to the profile.
   */
  @Test
  public void testUpdate() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testUpdateProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    int count = 10;
    for(int i=0; i<count; i++) {
      builder.apply(message);
    }
    ProfileMeasurement m = builder.flush();

    // validate that x=0, y=0 then x+=1, y+=2 for each message
    assertEquals(count*1 + count*2, (int) convert(m.getValue(), Integer.class));
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "init": { "x": "100" },
   *   "result": "x"
   * }
   */
  @Multiline
  private String testResultProfile;

  /**
   * Ensure that the result expression is executed on a flush.
   */
  @Test
  public void testResult() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testResultProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    builder.apply(message);
    ProfileMeasurement m = builder.flush();

    // validate
    assertEquals(100, (int) convert(m.getValue(), Integer.class));
  }

  /**
   * Ensure that time advances properly on each flush.
   */
  @Test
  public void testProfilePeriodOnFlush() throws Exception {
    // setup
    FixedClock clock = new FixedClock();
    clock.setTime(100);

    definition = JSONUtils.INSTANCE.load(testResultProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .withClock(clock)
            .build();

    {
      // apply a message and flush
      builder.apply(message);
      ProfileMeasurement m = builder.flush();

      // validate the profile period
      ProfilePeriod expected = new ProfilePeriod(clock.currentTimeMillis(), 10, TimeUnit.MINUTES);
      assertEquals(expected, m.getPeriod());
    }
    {
      // advance time by at least one period - 10 minutes
      clock.setTime(clock.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));

      // apply a message and flush again
      builder.apply(message);
      ProfileMeasurement m = builder.flush();

      // validate the profile period
      ProfilePeriod expected = new ProfilePeriod(clock.currentTimeMillis(), 10, TimeUnit.MINUTES);
      assertEquals(expected, m.getPeriod());
    }
  }


  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "init": { "x": "100" },
   *   "groupBy": ["x * 1", "x * 2"],
   *   "result": "100.0"
   * }
   */
  @Multiline
  private String testGroupByProfile;

  /**
   * Ensure that the 'groupBy' expression is executed correctly.
   */
  @Test
  public void testGroupBy() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testGroupByProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    builder.apply(message);
    ProfileMeasurement m = builder.flush();

    // validate
    assertEquals(2, m.getGroups().size());
    assertEquals(100, (int) convert(m.getGroups().get(0), Integer.class));
    assertEquals(200, (int) convert(m.getGroups().get(1), Integer.class));
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "init": {
   *     "x": "0",
   *     "y": "0"
   *   },
   *   "update": {
   *     "x": "x + 1",
   *     "y": "y + 2"
   *   },
   *   "result": "x + y"
   * }
   */
  @Multiline
  private String testFlushProfile;

  @Test
  public void testFlushClearsState() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testFlushProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute - accumulate some state then flush it
    int count = 10;
    for(int i=0; i<count; i++) {
      builder.apply(message);
    }
    builder.flush();

    // apply another message to accumulate new state, then flush again to validate original state was cleared
    builder.apply(message);
    ProfileMeasurement m = builder.flush();

    // validate
    assertEquals(3, (int) convert(m.getValue(), Integer.class));
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "result": "100"
   * }
   */
  @Multiline
  private String testEntityProfile;

  /**
   * Ensure that the entity is correctly set on the resulting profile measurements.
   */
  @Test
  public void testEntity() throws Exception {
    // setup
    final String entity = "10.0.0.1";
    definition = JSONUtils.INSTANCE.load(testFlushProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity(entity)
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // execute
    builder.apply(message);
    ProfileMeasurement m = builder.flush();

    // validate
    assertEquals(entity, m.getEntity());
  }

  /**
   * {
   *   "profile": "test",
   *   "foreach": "ip_src_addr",
   *   "tickUpdate": {
   *     "ticks": "ticks + 1"
   *   },
   *   "result": "if exists(ticks) then ticks else 0"
   * }
   */
  @Multiline
  private String testTickUpdateProfile;

  @Test
  public void testTickUpdate() throws Exception {
    // setup
    definition = JSONUtils.INSTANCE.load(testTickUpdateProfile, ProfileConfig.class);
    builder = new ProfileBuilder.Builder()
            .withDefinition(definition)
            .withEntity("10.0.0.1")
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .build();

    // 'tickUpdate' only executed when flushed - 'result' only has access to the 'old' tick value, not latest
    {
      ProfileMeasurement m = builder.flush();
      assertEquals(0, (int) convert(m.getValue(), Integer.class));
    }

    // execute many flushes
    int count = 10;
    for(int i=0; i<count; i++) {
      builder.flush();
    }

    {
      // validate - the tickUpdate state should not be cleared between periods and is only run once per period
      ProfileMeasurement m = builder.flush();
      assertEquals(11, (int) convert(m.getValue(), Integer.class));
    }
  }
}
