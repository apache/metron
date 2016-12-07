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
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.clock.FixedClock;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

import static org.apache.metron.common.configuration.ConfigurationType.PROFILER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ProfileSplitterBolt.
 */
public class ProfileSplitterBoltTest extends BaseBoltTest {

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20",
   *   "theTimestamp": "1481389144085",
   *   "protocol": "HTTP"
   * }
   */
  @Multiline
  private String input;
  private final long messageTimestamp = 1481389144085L;
  private JSONObject message;

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20",
   *   "protocol": "HTTP"
   * }
   */
  @Multiline
  private String inputNoTimestamp;
  private ProfileSplitterBolt bolt;
  private FixedClock clock;
  private InMemoryConfigurationManager configurationManager;

  @Before
  public void setup() throws Exception {

    // parse the input message
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(input);

    // ensure the tuple returns the expected json message
    when(tuple.getBinary(0)).thenReturn(input.getBytes());

    clock = new FixedClock();
    clock.setTime(1000);

    configurationManager = new InMemoryConfigurationManager();
    configurationManager.open();
    configurationManager.with(PROFILER.getZookeeperRoot());
  }

  /**
   * Ensure that the bolt can support event time processing.
   *
   * If the 'timestampField' is defined by the user, the bolt should 'timestamp' the tuple
   * with the value of that 'timestampField' from the telemetry message.
   */
  @Test
  public void testUseEventTime() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup - define the timestamp field using 'withTimestampField'
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withConfigurationManager(configurationManager)
            .withExecutor(new DefaultStellarExecutor())
            .withTimestampField("theTimestamp");

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // capture the emitted tuple
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(eq(tuple), arg.capture());
    Values actual = arg.getValue();

    // verify - the emit'd timestamp should come from the telemetry message aka event time
    assertEquals(messageTimestamp, actual.get(3));
  }

  /**
   * If the 'timestampField' is defined by the user, the bolt should 'timestamp' the tuple
   * with the value of that field from the telemetry message.
   *
   * Storm will kill the entire topology if a tuple is sent that is missing the expected
   * timestamp field.  Need to ensure a telemetry message thatis missing the timestamp is
   * NOT emitted.
   */
  @Test
  public void testDoNotEmitWhenMissingMessageTimestamp() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup a message with not timestamp field
    when(tuple.getBinary(0)).thenReturn(inputNoTimestamp.getBytes());

    // setup - define the timestamp field using 'withTimestampField'
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withConfigurationManager(configurationManager)
            .withExecutor(new DefaultStellarExecutor())
            .withTimestampField("theTimestamp");

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // validate - do not emit as message is missing the expected timestamp field
    verify(outputCollector, times(0)).emit(any(Tuple.class), any(Values.class));
  }


  /**
   * Ensure that the bolt can support processing time.
   *
   * If the 'timestampField' is NOT defined by the user, the bolt should 'timestamp' the tuple
   * with the wall clock time.
   */
  @Test
  public void testUseWallClockTime() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withConfigurationManager(configurationManager)
            .withExecutor(new DefaultStellarExecutor());

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // capture the emitted tuple
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(eq(tuple), arg.capture());
    Values actual = arg.getValue();

    // verify - the entity name comes from variable resolution in stella
    assertEquals(clock.currentTimeMillis(), actual.get(3));
  }

  /**
   * When the bolt emits a tuple to the downstream builder, it should contain
   * the original telemetry message.
   */
  @Test
  public void testEmitOriginalMessage() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // capture the emitted tuple
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(eq(tuple), arg.capture());
    Values actual = arg.getValue();

    // verify - emit the original message
    assertEquals(message, actual.get(2));
  }

  /**
   * When the bolt emits a tuple to the downstream builder, it should contain
   * the profile definition.
   */
  @Test
  public void testEmitProfileDefinition() throws Exception {

    // setup the profile definition
    final ProfileConfig expected = new ProfileConfig()
            .withProfile("test")
            .withForeach("ip_src_addr")
            .withOnlyif("true")
            .withResult("2");
    configurationManager.setValue(PROFILER.getZookeeperRoot(), new ProfilerConfig()
            .withProfile(expected));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // capture the emitted tuple
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(eq(tuple), arg.capture());
    Values actual = arg.getValue();

    // verify - emit the original message
    assertEquals(expected, actual.get(1));
  }

  /**
   * What happens when a profile's 'onlyif' expression is true?  The message
   * should be applied to the profile.
   */
  @Test
  public void testEmitTupleWhenOnlyIfIsTrue() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1)).emit(eq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }

  /**
   * A profile where the 'onlyif' is not defined, is treated the same as if 'onlyif = true'.
   */
  @Test
  public void testDefaultTrueWhenOnlyIfIsMissing() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);


    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1)).emit(eq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }

  /**
   * What happens when a profile's 'onlyif' expression is false?  The message
   * should NOT be applied to the profile.
   */
  @Test
  public void testDoNotEmitTupleWhenOnlyIfIsFalse() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("false")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // a tuple should NOT be emitted for the downstream profile builder
    verify(outputCollector, times(0)).emit(any(Tuple.class), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }

  /**
   * The entity associated with a ProfileMeasurement can be defined using a variable that is resolved
   * via Stella.  In this case the entity is defined as 'ip_src_addr' which is resolved to
   * '10.0.0.1' based on the data contained within the message.
   */
  @Test
  public void testEmitResolvedEntityName() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("true")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // capture the emitted tuple
    ArgumentCaptor<Values> arg = ArgumentCaptor.forClass(Values.class);
    verify(outputCollector, times(1)).emit(eq(tuple), arg.capture());
    Values actual = arg.getValue();

    // verify - the entity name comes from variable resolution in stella
    assertEquals("10.0.0.1", actual.get(0));
  }

  /**
   * An invalid profile, like one with an invalid 'onlyif' expression, should be handled cleanly
   * without throwing an exception.
   */
  public void testExceptionWhenInvalidExpression() throws Exception {

    // setup the profile definition
    configurationManager.setValue(PROFILER.getZookeeperRoot(),
            new ProfilerConfig()
                    .withProfile(new ProfileConfig()
                            .withProfile("test")
                            .withForeach("ip_src_addr")
                            .withOnlyif("2.f.invalid.condition")
                            .withResult("2")));

    // setup
    ProfileSplitterBolt bolt = new ProfileSplitterBolt()
            .withZookeeperClient(client)
            .withClock(clock)
            .withExecutor(new DefaultStellarExecutor())
            .withConfigurationManager(configurationManager);

    // execute - expect exception
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    bolt.execute(tuple);

    // validate - the error should be reported
    verify(outputCollector, times(1)).reportError(any());
  }
}
