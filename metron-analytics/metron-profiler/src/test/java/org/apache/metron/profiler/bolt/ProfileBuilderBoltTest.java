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

package org.apache.metron.profiler.bolt;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.manager.InMemoryConfigurationManager;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.ProfileBuilder;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ProfileBuilderBolt.
 */
public class ProfileBuilderBoltTest extends BaseBoltTest {

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "value": "22"
   * }
   */
  @Multiline
  private String input1;
  private JSONObject message1;

  /**
   * {
   *   "ip_src_addr": "10.0.0.2",
   *   "value": "22"
   * }
   */
  @Multiline
  private String input2;
  private JSONObject message2;
  private ProfileConfig definition1;
  private ProfileConfig definition2;
  private long timestamp = 100;

  @Before
  public void setup() throws Exception {
    JSONParser parser = new JSONParser();
    message1 = (JSONObject) parser.parse(input1);
    message2 = (JSONObject) parser.parse(input2);

    definition1 = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 1"))
            .withResult("x");

    definition2 = new ProfileConfig()
            .withProfile("profile2")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 1"))
            .withResult("x");
  }
  
  /**
   * The bolt should create a ProfileBuilder to manage a profile.
   */
  @Test
  public void testCreateProfileBuilder() throws Exception {

    // setup the bolt
    ProfileBuilderBolt bolt = createBolt();

    // setup the tuple window
    final String entity1 = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity1, message1, definition1, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1);
    bolt.execute(window);

    // validate that a builder was created for [profile1, entity1]
    ProfileBuilder builder1 = bolt.getBuilder(tuple1);
    assertEquals(definition1, builder1.getDefinition());
    assertEquals(definition1.getProfile(), builder1.getProfileName());
    assertEquals(entity1, builder1.getEntity());
  }

  /**
   * This test creates two different messages, with different entities that are applied to
   * the same profile.  The bolt should create separate ProfileBuilder objects to handle each
   * profile/entity pair.
   */
  @Test
  public void testCreateProfileBuilderForEachEntity() throws Exception {

    // setup the bolt
    ProfileBuilderBolt bolt = createBolt();

    // create tuples with same profile, but different entities; [profile1, entity1] and [profile1, entity2]
    final String entity1 = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity1, message1, definition1, timestamp);
    final String entity2 = (String) message2.get("ip_src_addr");
    Tuple tuple2 = createTuple(entity2, message2, definition1, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1, tuple2);
    bolt.execute(window);

    {
      // validate that a builder was created for [profile1, entity1]
      ProfileBuilder builder1 = bolt.getBuilder(tuple1);
      assertEquals(definition1, builder1.getDefinition());
      assertEquals(definition1.getProfile(), builder1.getProfileName());
      assertEquals(entity1, builder1.getEntity());
    }
    {
      // validate that a builder was created for [profile1, entity2]
      ProfileBuilder builder2 = bolt.getBuilder(tuple2);
      assertEquals(definition1, builder2.getDefinition());
      assertEquals(definition1.getProfile(), builder2.getProfileName());
      assertEquals(entity2, builder2.getEntity());
    }
  }

  /**
   * The bolt should create separate ProfileBuilder objects to handle each
   * profile/entity pair.
   */
  @Test
  public void testCreateProfileBuilderForEachProfile() throws Exception {

    // setup the bolt
    ProfileBuilderBolt bolt = createBolt();

    // create tuples with same entity, but different profiles; [profile1, entity1] and [profile2, entity1]
    final String entity = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity, message1, definition1, timestamp);
    Tuple tuple2 = createTuple(entity, message1, definition2, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1, tuple2);
    bolt.execute(window);

    {
      // validate that a builder was created for [profile1, entity1]
      ProfileBuilder builder1 = bolt.getBuilder(tuple1);
      assertEquals(definition1, builder1.getDefinition());
      assertEquals(definition1.getProfile(), builder1.getProfileName());
      assertEquals(entity, builder1.getEntity());
    }
    {
      // validate that a builder was created for [profile2, entity1]
      ProfileBuilder builder2 = bolt.getBuilder(tuple2);
      assertEquals(definition2, builder2.getDefinition());
      assertEquals(definition2.getProfile(), builder2.getProfileName());
      assertEquals(entity, builder2.getEntity());
    }
  }

  /**
   * A ProfileMeasurement should be emitted for each profile/entity currently being tracked
   * by the bolt.
   */
  @Test
  public void testEmitProfileMeasurement() throws Exception {

    // setup
    ProfileBuilderBolt bolt = createBolt();

    // create tuples with same entity, but different profiles; [profile1, entity1] and [profile2, entity1]
    final String entity = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity, message1, definition1, timestamp);
    Tuple tuple2 = createTuple(entity, message1, definition2, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1, tuple2);
    bolt.execute(window);

    // validate
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(2)).emit(arg.capture());
    for(Values value : arg.getAllValues()) {

      ProfileMeasurement measurement = (ProfileMeasurement) value.get(0);
      ProfileConfig definition = (ProfileConfig) value.get(1);
      switch(definition.getProfile()) {

        case "profile1": {
          assertEquals(definition1, definition);
          ProfileMeasurement expected = new ProfileMeasurement()
                  .withProfileName(definition1.getProfile())
                  .withEntity(entity)
                  .withValue(1)
                  .withPeriod(timestamp, bolt.getPeriodDurationMillis(), TimeUnit.MILLISECONDS);
          assertEquals(expected, measurement);
          break;
        }

        case "profile2": {
          assertEquals(definition2, definition);
          ProfileMeasurement expected = new ProfileMeasurement()
                  .withProfileName(definition2.getProfile())
                  .withEntity(entity)
                  .withValue(1)
                  .withPeriod(timestamp, bolt.getPeriodDurationMillis(), TimeUnit.MILLISECONDS);
          assertEquals(expected, measurement);
          break;
        }

        default:
          fail("unexpected profile: " + definition.getProfile());
      }
    }
  }

  /**
   * If a user changes a profile definition, we want to start using a freshly minted
   * ProfileBuilder, rather than the original, whose state was built using the old
   * definition.  This gives us a clean cut-over to the new profile definition and
   * minimizes the chance of unexpected interactions between the old and
   * new profile definition.
   */
  @Test
  public void testChangeProfileDefinition() throws Exception {

    // setup the bolt
    ProfileBuilderBolt bolt = createBolt();

    // version 1.0 of the profile definition
    ProfileConfig v1 = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 1"))
            .withResult("x");

    // create a tuple with version 1.0 of the profile definition
    final String entity = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity, message1, v1, timestamp);

    // the version 2.0 of the profile definition, all the same except the update expression
    ProfileConfig v2 = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 2"))
            .withResult("x");

    // the update profile definition will be attached to subsequent tuples
    Tuple tuple2 = createTuple(entity, message1, v2, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1, tuple2);
    bolt.execute(window);

    // validate that a builder was created for version 1.0 of the profile
    assertEquals(v1, bolt.getBuilder(tuple1).getDefinition());
    assertEquals(v2, bolt.getBuilder(tuple2).getDefinition());
    assertNotSame(bolt.getBuilder(tuple1), bolt.getBuilder(tuple2));
  }

  /**
   * If the user creates a bad profile, all of the other functioning profiles should
   * continue to operate.
   */
  @Test
  public void testHandleBadProfile() throws Exception {

    // setup the bolt
    ProfileBuilderBolt bolt = createBolt();

    // bad profile - division by zero
    ProfileConfig badProfile = new ProfileConfig()
            .withProfile("bad-profile")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "2 / 0"))
            .withResult("2 / 0");

    // match a tuple to the bad profile
    final String entity = (String) message1.get("ip_src_addr");
    Tuple tuple1 = createTuple(entity, message1, badProfile, timestamp);

    // good profile
    ProfileConfig goodProfile = new ProfileConfig()
            .withProfile("good-profile")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 2"))
            .withResult("x");

    // the update profile definition will be attached to subsequent tuples
    Tuple tuple2 = createTuple(entity, message1, goodProfile, timestamp);

    // execute
    TupleWindow window = createWindow(tuple1, tuple2);
    bolt.execute(window);

    // validate that a builder was created for each profile
    assertEquals(badProfile, bolt.getBuilder(tuple1).getDefinition());
    assertEquals(goodProfile, bolt.getBuilder(tuple2).getDefinition());

    // validate
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(arg.capture());
    for(Values value : arg.getAllValues()) {
      ProfileConfig definition = (ProfileConfig) value.get(1);
      assertEquals("good-profile", definition.getProfile());
    }
  }

  /**
   * Create a tuple that will contain the message, the entity name, and profile definition.
   * @param entity The entity name
   * @param message The telemetry message.
   * @param profile The profile definition.
   */
  private Tuple createTuple(String entity, JSONObject message, ProfileConfig profile, long timestamp) {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq("message"))).thenReturn(message);
    when(tuple.getValueByField(eq("entity"))).thenReturn(entity);
    when(tuple.getValueByField(eq("profile"))).thenReturn(profile);
    when(tuple.getValueByField(eq("timestamp"))).thenReturn(timestamp);
    return tuple;
  }

  /**
   * Creates a mock TupleWindow containing multiple tuples.
   * @param tuples The tuples to add to the window.
   */
  private TupleWindow createWindow(Tuple... tuples) {
    TupleWindow window = mock(TupleWindow.class);
    when(window.get()).thenReturn(Arrays.asList(tuples));
    return window;
  }

  /**
   * Create a ProfileBuilderBolt to test
   */
  private ProfileBuilderBolt createBolt() throws IOException {

    InMemoryConfigurationManager configurationManager = new InMemoryConfigurationManager();
    configurationManager.open();

    ProfileBuilderBolt bolt = (ProfileBuilderBolt) new ProfileBuilderBolt()
            .withTimeToLive(30, TimeUnit.MINUTES)
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .withZookeeperClient(client)
            .withConfigurationManager(configurationManager)
            .withTumblingWindow(new BaseWindowedBolt.Duration(10, TimeUnit.MINUTES));

    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    return bolt;
  }
}
