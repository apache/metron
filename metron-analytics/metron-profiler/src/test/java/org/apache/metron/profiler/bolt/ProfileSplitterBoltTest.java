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
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.clock.FixedClockFactory;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

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
   *   "protocol": "HTTP",
   *   "timestamp.custom": 2222222222222,
   *   "timestamp.string": "3333333333333"
   * }
   */
  @Multiline
  private String input;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "protocol == 'HTTP'",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profileWithOnlyIfTrue;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "false",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profileWithOnlyIfFalse;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profileWithOnlyIfMissing;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "NOT-VALID",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profileWithOnlyIfInvalid;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ],
   *   "timestampField": "timestamp.custom"
   * }
   */
  @Multiline
  private String profileUsingCustomTimestampField;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ],
   *   "timestampField": "timestamp.missing"
   * }
   */
  @Multiline
  private String profileUsingMissingTimestampField;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "init": {},
   *        "update": {},
   *        "result": "2"
   *      }
   *   ],
   *   "timestampField": "timestamp.string"
   * }
   */
  @Multiline
  private String profileUsingStringTimestampField;

  /**
   * {
   *   "profiles": [
   *   ]
   * }
   */
  @Multiline
  private String noProfilesDefined;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile1",
   *        "foreach": "'global'",
   *        "result": "1"
   *      },
   *      {
   *        "profile": "profile2",
   *        "foreach": "'global'",
   *        "result": "2"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String twoProfilesDefined;

  private JSONObject message;
  private long timestamp = 3333333;

  @Before
  public void setup() throws ParseException {

    // parse the input message
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(input);

    // ensure the tuple returns the expected json message
    when(tuple.getBinary(0)).thenReturn(input.getBytes());
  }

  /**
   * Ensure that a tuple with the correct fields is emitted to downstream bolts
   * when a profile is defined.
   */
  @Test
  public void testEmitTupleWithOneProfile() throws Exception {

    // setup the bolt and execute a tuple
    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfTrue);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // the expected tuple fields
    String expectedEntity = "10.0.0.1";
    ProfileConfig expectedConfig = config.getProfiles().get(0);
    Values expected = new Values(message, timestamp, expectedEntity, expectedConfig);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1))
            .emit(eq(tuple), eq(expected));

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * If there are two profiles that need the same message, then two tuples should
   * be emitted.  One tuple for each profile.
   */
  @Test
  public void testEmitTupleWithTwoProfiles() throws Exception {

    // setup the bolt and execute a tuple
    ProfilerConfig config = toProfilerConfig(twoProfilesDefined);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // the expected tuple fields
    final String expectedEntity = "global";
    {
      // a tuple should be emitted for the first profile
      ProfileConfig profile1 = config.getProfiles().get(0);
      Values expected = new Values(message, timestamp, expectedEntity, profile1);
      verify(outputCollector, times(1))
              .emit(eq(tuple), eq(expected));
    }
    {
      // a tuple should be emitted for the second profile
      ProfileConfig profile2 = config.getProfiles().get(1);
      Values expected = new Values(message, timestamp, expectedEntity, profile2);
      verify(outputCollector, times(1))
              .emit(eq(tuple), eq(expected));
    }

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * No tuples should be emitted, if no profiles are defined.
   */
  @Test
  public void testNoProfilesDefined() throws Exception {

    // setup the bolt and execute a tuple
    ProfilerConfig config = toProfilerConfig(noProfilesDefined);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // no tuple should be emitted
    verify(outputCollector, times(0))
            .emit(any(Tuple.class), any());

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * What happens when a profile's 'onlyif' expression is true?  The message
   * should be applied to the profile.
   */
  @Test
  public void testOnlyIfTrue() throws Exception {

    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfTrue);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1))
            .emit(eq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * All messages are applied to a profile where 'onlyif' is missing.  A profile with no
   * 'onlyif' is treated the same as if 'onlyif=true'.
   */
  @Test
  public void testOnlyIfMissing() throws Exception {

    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfMissing);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1))
            .emit(eq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * What happens when a profile's 'onlyif' expression is false?  The message
   * should NOT be applied to the profile.
   */
  @Test
  public void testOnlyIfFalse() throws Exception {

    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfFalse);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // a tuple should NOT be emitted for the downstream profile builder
    verify(outputCollector, times(0))
            .emit(any());

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * The entity associated with a profile is defined with a Stellar expression.  That expression
   * can refer to any field within the message.
   *
   * In this case the entity is defined as 'ip_src_addr' which is resolved to '10.0.0.1' based on
   * the data contained within the message.
   */
  @Test
  public void testResolveEntityName() throws Exception {

    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfTrue);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // expected values
    String expectedEntity = "10.0.0.1";
    ProfileConfig expectedConfig = config.getProfiles().get(0);
    Values expected = new Values(message, timestamp, expectedEntity, expectedConfig);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1))
            .emit(eq(tuple), eq(expected));

    // the original tuple should be ack'd
    verify(outputCollector, times(1))
            .ack(eq(tuple));
  }

  /**
   * What happens when invalid Stella code is used for 'onlyif'?  The invalid profile should be ignored.
   */
  @Test
  public void testOnlyIfInvalid() throws Exception {

    ProfilerConfig config = toProfilerConfig(profileWithOnlyIfInvalid);
    ProfileSplitterBolt bolt = createBolt(config);
    bolt.execute(tuple);

    // a tuple should NOT be emitted for the downstream profile builder
    verify(outputCollector, times(0))
            .emit(any(Values.class));
  }

  /**
   * Creates a ProfilerConfig based on a string containing JSON.
   *
   * @param configAsJSON The config as JSON.
   * @return The ProfilerConfig.
   * @throws Exception
   */
  private ProfilerConfig toProfilerConfig(String configAsJSON) throws Exception {
    InputStream in = new ByteArrayInputStream(configAsJSON.getBytes("UTF-8"));
    return JSONUtils.INSTANCE.load(in, ProfilerConfig.class);
  }

  /**
   * Create a ProfileSplitterBolt to test
   */
  private ProfileSplitterBolt createBolt(ProfilerConfig config) throws Exception {

    ProfileSplitterBolt bolt = new ProfileSplitterBolt("zookeeperURL");
    bolt.setCuratorFramework(client);
    bolt.setZKCache(cache);
    bolt.getConfigurations().updateProfilerConfig(config);
    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);

    // set the clock factory AFTER calling prepare to use the fixed clock factory
    bolt.setClockFactory(new FixedClockFactory(timestamp));

    return bolt;
  }

}
