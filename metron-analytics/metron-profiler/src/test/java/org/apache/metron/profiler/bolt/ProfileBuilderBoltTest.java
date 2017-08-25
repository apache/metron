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
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfileBuilder;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.Constants;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.stellar.common.utils.ConversionUtils.convert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
  private String inputOne;
  private JSONObject messageOne;

  /**
   * {
   *   "ip_src_addr": "10.0.0.2",
   *   "value": "22"
   * }
   */
  @Multiline
  private String inputTwo;
  private JSONObject messageTwo;

  /**
   * {
   *   "profile": "profileOne",
   *   "foreach": "ip_src_addr",
   *   "init":   { "x": "0" },
   *   "update": { "x": "x + 1" },
   *   "result": "x"
   * }
   */
  @Multiline
  private String profileOne;


  /**
   * {
   *   "profile": "profileTwo",
   *   "foreach": "ip_src_addr",
   *   "init":   { "x": "0" },
   *   "update": { "x": "x + 1" },
   *   "result": "x"
   * }
   */
  @Multiline
  private String profileTwo;

  public static Tuple mockTickTuple() {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getSourceComponent()).thenReturn(Constants.SYSTEM_COMPONENT_ID);
    when(tuple.getSourceStreamId()).thenReturn(Constants.SYSTEM_TICK_STREAM_ID);
    return tuple;
  }

  @Before
  public void setup() throws Exception {
    JSONParser parser = new JSONParser();
    messageOne = (JSONObject) parser.parse(inputOne);
    messageTwo = (JSONObject) parser.parse(inputTwo);
  }

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfileConfig createDefinition(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfileConfig.class);
  }

  /**
   * Create a tuple that will contain the message, the entity name, and profile definition.
   * @param entity The entity name
   * @param message The telemetry message.
   * @param profile The profile definition.
   */
  private Tuple createTuple(String entity, JSONObject message, ProfileConfig profile) {
    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq("message"))).thenReturn(message);
    when(tuple.getValueByField(eq("entity"))).thenReturn(entity);
    when(tuple.getValueByField(eq("profile"))).thenReturn(profile);
    return tuple;
  }

  /**
   * Create a ProfileBuilderBolt to test
   */
  private ProfileBuilderBolt createBolt() throws IOException {

    ProfileBuilderBolt bolt = new ProfileBuilderBolt("zookeeperURL");
    bolt.setCuratorFramework(client);
    bolt.setTreeCache(cache);
    bolt.withPeriodDuration(10, TimeUnit.MINUTES);
    bolt.withProfileTimeToLive(30, TimeUnit.MINUTES);

    // define the valid destinations for the profiler
    bolt.withDestinationHandler(new HBaseDestinationHandler());
    bolt.withDestinationHandler(new KafkaDestinationHandler());

    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    return bolt;
  }

  /**
   * The bolt should create a ProfileBuilder to manage a profile.
   */
  @Test
  public void testCreateProfileBuilder() throws Exception {

    ProfileBuilderBolt bolt = createBolt();
    ProfileConfig definition = createDefinition(profileOne);
    String entity = (String) messageOne.get("ip_src_addr");
    Tuple tupleOne = createTuple(entity, messageOne, definition);

    // execute - send two tuples with different entities
    bolt.execute(tupleOne);

    // validate - 1 messages applied
    MessageRoute route = new MessageRoute(definition, entity);
    ProfileBuilder builderOne = bolt.getMessageDistributor().getBuilder(route, Context.EMPTY_CONTEXT());
    assertEquals(1, (int) convert(builderOne.valueOf("x"), Integer.class));
  }

  /**
   * This test creates two different messages, with different entities that are applied to
   * the same profile.  The bolt should create separate ProfileBuilder objects to handle each
   * profile/entity pair.
   */
  @Test
  public void testCreateProfileBuilderForEachEntity() throws Exception {

    // setup
    ProfileBuilderBolt bolt = createBolt();
    ProfileConfig definition = createDefinition(profileOne);

    // apply a message to the profile
    String entityOne = (String) messageOne.get("ip_src_addr");
    Tuple tupleOne = createTuple(entityOne, messageOne, definition);
    bolt.execute(tupleOne);
    bolt.execute(tupleOne);

    // apply a different message (with different entity) to the same profile
    String entityTwo = (String) messageTwo.get("ip_src_addr");
    Tuple tupleTwo = createTuple(entityTwo, messageTwo, definition);
    bolt.execute(tupleTwo);

    // validate - 2 messages applied
    MessageRoute routeOne = new MessageRoute(definition, entityOne);
    ProfileBuilder builderOne = bolt.getMessageDistributor().getBuilder(routeOne, Context.EMPTY_CONTEXT());
    assertTrue(builderOne.isInitialized());
    assertEquals(2, (int) convert(builderOne.valueOf("x"), Integer.class));

    // validate - 1 message applied
    MessageRoute routeTwo = new MessageRoute(definition, entityTwo);
    ProfileBuilder builderTwo = bolt.getMessageDistributor().getBuilder(routeTwo, Context.EMPTY_CONTEXT());
    assertTrue(builderTwo.isInitialized());
    assertEquals(1, (int) convert(builderTwo.valueOf("x"), Integer.class));

    assertNotSame(builderOne, builderTwo);
  }

  /**
   * The bolt should create separate ProfileBuilder objects to handle each
   * profile/entity pair.
   */
  @Test
  public void testCreateProfileBuilderForEachProfile() throws Exception {

    // setup - apply one message to different profile definitions
    ProfileBuilderBolt bolt = createBolt();
    String entity = (String) messageOne.get("ip_src_addr");

    // apply a message to the first profile
    ProfileConfig definitionOne = createDefinition(profileOne);
    Tuple tupleOne = createTuple(entity, messageOne, definitionOne);
    bolt.execute(tupleOne);

    // apply the same message to the second profile
    ProfileConfig definitionTwo = createDefinition(profileTwo);
    Tuple tupleTwo = createTuple(entity, messageOne, definitionTwo);
    bolt.execute(tupleTwo);

    // validate - 1 message applied
    MessageRoute routeOne = new MessageRoute(definitionOne, entity);
    ProfileBuilder builderOne = bolt.getMessageDistributor().getBuilder(routeOne, Context.EMPTY_CONTEXT());
    assertTrue(builderOne.isInitialized());
    assertEquals(1, (int) convert(builderOne.valueOf("x"), Integer.class));

    // validate - 1 message applied
    MessageRoute routeTwo = new MessageRoute(definitionTwo, entity);
    ProfileBuilder builderTwo = bolt.getMessageDistributor().getBuilder(routeTwo, Context.EMPTY_CONTEXT());
    assertTrue(builderTwo.isInitialized());
    assertEquals(1, (int) convert(builderTwo.valueOf("x"), Integer.class));

    assertNotSame(builderOne, builderTwo);
  }

  /**
   * A ProfileMeasurement is build for each profile/entity pair.  A measurement for each profile/entity
   * pair should be emitted.
   */
  @Test
  public void testEmitMeasurements() throws Exception {

    // setup
    ProfileBuilderBolt bolt = createBolt();
    final String entity = (String) messageOne.get("ip_src_addr");

    // apply the message to the first profile
    ProfileConfig definitionOne = createDefinition(profileOne);
    Tuple tupleOne = createTuple(entity, messageOne, definitionOne);
    bolt.execute(tupleOne);

    // apply the same message to the second profile
    ProfileConfig definitionTwo = createDefinition(profileTwo);
    Tuple tupleTwo = createTuple(entity, messageOne, definitionTwo);
    bolt.execute(tupleTwo);

    // execute - the tick tuple triggers a flush of the profile
    bolt.execute(mockTickTuple());

    // capture the ProfileMeasurement that should be emitted
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);

    // validate emitted measurements for hbase
    verify(outputCollector, atLeastOnce()).emit(eq("hbase"), arg.capture());
    for (Values value : arg.getAllValues()) {

      ProfileMeasurement measurement = (ProfileMeasurement) value.get(0);
      ProfileConfig definition = measurement.getDefinition();

      if (StringUtils.equals(definitionTwo.getProfile(), definition.getProfile())) {

        // validate measurement emitted for profile two
        assertEquals(definitionTwo, definition);
        assertEquals(entity, measurement.getEntity());
        assertEquals(definitionTwo.getProfile(), measurement.getProfileName());
        assertEquals(1, (int) convert(measurement.getProfileValue(), Integer.class));

      } else if (StringUtils.equals(definitionOne.getProfile(), definition.getProfile())) {

        // validate measurement emitted for profile one
        assertEquals(definitionOne, definition);
        assertEquals(entity, measurement.getEntity());
        assertEquals(definitionOne.getProfile(), measurement.getProfileName());
        assertEquals(1, (int) convert(measurement.getProfileValue(), Integer.class));

      } else {
        fail();
      }
    }
  }

  /**
   * A ProfileMeasurement is build for each profile/entity pair.  The measurement should be emitted to each
   * destination defined by the profile. By default, a profile uses both Kafka and HBase as destinations.
   */
  @Test
  public void testDestinationHandlers() throws Exception {

    // setup
    ProfileBuilderBolt bolt = createBolt();
    ProfileConfig definitionOne = createDefinition(profileOne);

    // apply the message to the first profile
    final String entity = (String) messageOne.get("ip_src_addr");
    Tuple tupleOne = createTuple(entity, messageOne, definitionOne);
    bolt.execute(tupleOne);

    // trigger a flush of the profile
    bolt.execute(mockTickTuple());

    // capture the values that should be emitted
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);

    // validate measurements emitted to HBase
    verify(outputCollector, times(1)).emit(eq("hbase"), arg.capture());
    assertTrue(arg.getValue().get(0) instanceof ProfileMeasurement);

    // validate measurements emitted to Kafka
    verify(outputCollector, times(1)).emit(eq("kafka"), arg.capture());
    assertTrue(arg.getValue().get(0) instanceof JSONObject);
  }
}
