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

import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfigurations;
import org.apache.metron.profiler.MessageDistributor;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.profiler.integration.MessageBuilder;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the ProfileBuilderBolt.
 */
public class ProfileBuilderBoltTest extends BaseBoltTest {

  private JSONObject message1;
  private JSONObject message2;
  private ProfileConfig profile1;
  private ProfileConfig profile2;
  private ProfileMeasurementEmitter emitter;
  private ManualFlushSignal flushSignal;
  private ProfileMeasurement measurement;

  @Before
  public void setup() throws Exception {

    message1 = new MessageBuilder()
            .withField("ip_src_addr", "10.0.0.1")
            .withField("value", "22")
            .build();

    message2 = new MessageBuilder()
            .withField("ip_src_addr", "10.0.0.2")
            .withField("value", "22")
            .build();

    profile1 = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withInit("x", "0")
            .withUpdate("x", "x + 1")
            .withResult("x");

    profile2 = new ProfileConfig()
            .withProfile("profile2")
            .withForeach("ip_src_addr")
            .withInit(Collections.singletonMap("x", "0"))
            .withUpdate(Collections.singletonMap("x", "x + 1"))
            .withResult("x");

    measurement = new ProfileMeasurement()
            .withEntity("entity1")
            .withProfileName("profile1")
            .withPeriod(1000, 500, TimeUnit.MILLISECONDS)
            .withProfileValue(22);

    flushSignal = new ManualFlushSignal();
    flushSignal.setFlushNow(false);
  }

  /**
   * The bolt should extract a message and timestamp from a tuple and
   * pass that to a {@code MessageDistributor}.
   */
  @Test
  public void testExtractMessage() throws Exception {

    ProfileBuilderBolt bolt = createBolt();

    // create a mock
    MessageDistributor distributor = mock(MessageDistributor.class);
    bolt.withMessageDistributor(distributor);

    // create a tuple
    final long timestamp1 = 100000000L;
    Tuple tuple1 = createTuple("entity1", message1, profile1, timestamp1);

    // execute the bolt
    TupleWindow tupleWindow = createWindow(tuple1);
    bolt.execute(tupleWindow);

    // the message should have been extracted from the tuple and passed to the MessageDistributor
    verify(distributor).distribute(eq(message1), eq(timestamp1), any(MessageRoute.class), any());
  }


  /**
   * If the {@code FlushSignal} tells the bolt to flush, it should flush the {@code MessageDistributor}
   * and emit the {@code ProfileMeasurement} values from all active profiles.
   */
  @Test
  public void testFlushActiveProfiles() throws Exception {

    ProfileBuilderBolt bolt = createBolt();

    // create a mock that returns the profile measurement above
    MessageDistributor distributor = mock(MessageDistributor.class);
    when(distributor.flush()).thenReturn(Collections.singletonList(measurement));
    bolt.withMessageDistributor(distributor);

    // signal the bolt to flush
    flushSignal.setFlushNow(true);

    // execute the bolt
    Tuple tuple1 = createTuple("entity1", message1, profile1, 1000L);
    TupleWindow tupleWindow = createWindow(tuple1);
    bolt.execute(tupleWindow);

    // a profile measurement should be emitted by the bolt
    List<ProfileMeasurement> measurements = getProfileMeasurements(outputCollector, 1);
    assertEquals(1, measurements.size());
    assertEquals(measurement, measurements.get(0));
  }

  /**
   * If the {@code FlushSignal} tells the bolt NOT to flush, nothing should be emitted.
   */
  @Test
  public void testDoNotFlushActiveProfiles() throws Exception {

    ProfileBuilderBolt bolt = createBolt();

    // create a mock where flush() returns the profile measurement above
    MessageDistributor distributor = mock(MessageDistributor.class);
    when(distributor.flush()).thenReturn(Collections.singletonList(measurement));
    bolt.withMessageDistributor(distributor);

    // there is no flush signal
    flushSignal.setFlushNow(false);

    // execute the bolt
    Tuple tuple1 = createTuple("entity1", message1, profile1, 1000L);
    TupleWindow tupleWindow = createWindow(tuple1);
    bolt.execute(tupleWindow);

    // nothing should have been emitted
    getProfileMeasurements(outputCollector, 0);
  }

  /**
   * Expired profiles should be flushed regularly, even if no input telemetry
   * has been received.
   */
  @Test
  public void testFlushExpiredProfiles() throws Exception {

    ProfileBuilderBolt bolt = createBolt();

    // create a mock where flushExpired() returns the profile measurement above
    MessageDistributor distributor = mock(MessageDistributor.class);
    when(distributor.flushExpired()).thenReturn(Collections.singletonList(measurement));
    bolt.withMessageDistributor(distributor);

    // execute test by flushing expired profiles. this is normally triggered by a timer task.
    bolt.flushExpired();

    // a profile measurement should be emitted by the bolt
    List<ProfileMeasurement> measurements = getProfileMeasurements(outputCollector, 1);
    assertEquals(1, measurements.size());
    assertEquals(measurement, measurements.get(0));
  }

  /**
   * A {@link ProfileMeasurement} is built for each profile/entity pair.  The measurement should be emitted to each
   * destination defined by the profile. By default, a profile uses both Kafka and HBase as destinations.
   */
  @Test
  public void testEmitters() throws Exception {

    // defines the zk configurations accessible from the bolt
    ProfilerConfigurations configurations = new ProfilerConfigurations();
    configurations.updateGlobalConfig(Collections.emptyMap());

    // create the bolt with 3 destinations
    ProfileBuilderBolt bolt = (ProfileBuilderBolt) new ProfileBuilderBolt()
            .withProfileTimeToLive(30, TimeUnit.MINUTES)
            .withPeriodDuration(10, TimeUnit.MINUTES)
            .withMaxNumberOfRoutes(Long.MAX_VALUE)
            .withZookeeperClient(client)
            .withZookeeperCache(cache)
            .withEmitter(new TestEmitter("destination1"))
            .withEmitter(new TestEmitter("destination2"))
            .withEmitter(new TestEmitter("destination3"))
            .withProfilerConfigurations(configurations)
            .withTumblingWindow(new BaseWindowedBolt.Duration(10, TimeUnit.MINUTES));
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);

    // signal the bolt to flush
    bolt.withFlushSignal(flushSignal);
    flushSignal.setFlushNow(true);

    // execute the bolt
    Tuple tuple1 = createTuple("entity", message1, profile1, System.currentTimeMillis());
    TupleWindow window = createWindow(tuple1);
    bolt.execute(window);

    // validate measurements emitted to each
    verify(outputCollector, times(1)).emit(eq("destination1"), any());
    verify(outputCollector, times(1)).emit(eq("destination2"), any());
    verify(outputCollector, times(1)).emit(eq("destination3"), any());
  }

  @Test
  public void testExceptionWhenFlushingExpiredProfiles() throws Exception {
    // create an emitter that will throw an exception when emit() called
    ProfileMeasurementEmitter badEmitter = mock(ProfileMeasurementEmitter.class);
    doThrow(new RuntimeException("flushExpired() should catch this exception"))
            .when(badEmitter)
            .emit(any(), any());

    // create a distributor that will return a profile measurement
    MessageDistributor distributor = mock(MessageDistributor.class);
    when(distributor.flushExpired()).thenReturn(Collections.singletonList(measurement));

    // the bolt will use the bad emitter when flushExpired() is called
    ProfileBuilderBolt bolt = (ProfileBuilderBolt) new ProfileBuilderBolt()
            .withEmitter(badEmitter)
            .withMessageDistributor(distributor);

    // the exception thrown by the emitter should not bubble up
    bolt.flushExpired();
  }

  /**
   * Retrieves the ProfileMeasurement(s) (if any) that have been emitted.
   *
   * @param collector The Storm output collector.
   * @param expected The number of measurements expected.
   * @return A list of ProfileMeasurement(s).
   */
  private List<ProfileMeasurement> getProfileMeasurements(OutputCollector collector, int expected) {

    // the 'streamId' is defined by the DestinationHandler being used by the bolt
    final String streamId = emitter.getStreamId();

    // capture the emitted tuple(s)
    ArgumentCaptor<Values> argCaptor = ArgumentCaptor.forClass(Values.class);
    verify(collector, times(expected))
            .emit(eq(streamId), argCaptor.capture());

    // return the profile measurements that were emitted
    return argCaptor.getAllValues()
            .stream()
            .map(val -> (ProfileMeasurement) val.get(0))
            .collect(Collectors.toList());
  }

  /**
   * Create a tuple that will contain the message, the entity name, and profile definition.
   * @param entity The entity name
   * @param message The telemetry message.
   * @param profile The profile definition.
   */
  private Tuple createTuple(String entity, JSONObject message, ProfileConfig profile, long timestamp) {

    Tuple tuple = mock(Tuple.class);
    when(tuple.getValueByField(eq(ProfileSplitterBolt.MESSAGE_TUPLE_FIELD))).thenReturn(message);
    when(tuple.getValueByField(eq(ProfileSplitterBolt.TIMESTAMP_TUPLE_FIELD))).thenReturn(timestamp);
    when(tuple.getValueByField(eq(ProfileSplitterBolt.ENTITY_TUPLE_FIELD))).thenReturn(entity);
    when(tuple.getValueByField(eq(ProfileSplitterBolt.PROFILE_TUPLE_FIELD))).thenReturn(profile);

    return tuple;
  }

  /**
   * Create a ProfileBuilderBolt to test.
   * @return A {@link ProfileBuilderBolt} to test.
   */
  private ProfileBuilderBolt createBolt() throws IOException {

    // defines the zk configurations accessible from the bolt
    ProfilerConfigurations configurations = new ProfilerConfigurations();
    configurations.updateGlobalConfig(Collections.emptyMap());

    emitter = new HBaseEmitter();
    ProfileBuilderBolt bolt = (ProfileBuilderBolt) new ProfileBuilderBolt()
            .withProfileTimeToLive(30, TimeUnit.MINUTES)
            .withMaxNumberOfRoutes(Long.MAX_VALUE)
            .withZookeeperClient(client)
            .withZookeeperCache(cache)
            .withEmitter(emitter)
            .withProfilerConfigurations(configurations)
            .withPeriodDuration(1, TimeUnit.MINUTES)
            .withTumblingWindow(new BaseWindowedBolt.Duration(30, TimeUnit.SECONDS));
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);

    // set the flush signal AFTER calling 'prepare'
    bolt.withFlushSignal(flushSignal);

    return bolt;
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
   * An implementation for testing purposes only.
   */
  private class TestEmitter implements ProfileMeasurementEmitter {

    private String streamId;

    public TestEmitter(String streamId) {
      this.streamId = streamId;
    }

    @Override
    public String getStreamId() {
      return streamId;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declareStream(getStreamId(), new Fields("measurement"));
    }

    @Override
    public void emit(ProfileMeasurement measurement, OutputCollector collector) {
      collector.emit(getStreamId(), new Values(measurement));
    }
  }
}
