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

import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.profiler.stellar.DefaultStellarExecutor;
import org.apache.metron.test.bolt.BaseBoltTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

/**
 * Tests the ProfileSplitterBolt.
 */
public class ProfileSplitterBoltTest extends BaseBoltTest {

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20",
   *   "protocol": "HTTP"
   * }
   */
  @Multiline
  private String input;

  /**
   * {
   *   "inputTopic": "enrichment",
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "protocol == 'HTTP'",
   *        "init": {},
   *        "update": {},
   *        "result": 2
   *      }
   *   ]
   * }
   */
  @Multiline
  private String onlyIfTrue;

  /**
   * {
   *   "inputTopic": "enrichment",
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "false",
   *        "init": {},
   *        "update": {},
   *        "result": 2
   *      }
   *   ]
   * }
   */
  @Multiline
  private String onlyIfFalse;

  /**
   * {
   *   "inputTopic": "enrichment",
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "onlyif": "NOT-VALID",
   *        "init": {},
   *        "update": {},
   *        "result": 2
   *      }
   *   ]
   * }
   */
  @Multiline
  private String invalidIf;

  /**
   * {
   *   "inputTopic": "enrichment",
   *   "profiles": [
   *      {
   *        "profile": "test",
   *        "foreach": "ip_src_addr",
   *        "init": {},
   *        "update": {},
   *        "result": 2
   *      }
   *   ]
   * }
   */
  @Multiline
  private String missingIf;

  private JSONObject message;

  @Before
  public void setup() throws ParseException {

    // parse the input message
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(input);

    // ensure the tuple returns the expected json message
    when(tuple.getBinary(0)).thenReturn(input.getBytes());
  }

  /**
   * Create a ProfileSplitterBolt to test
   */
  private ProfileSplitterBolt createBolt(String profilerConfig) throws IOException {

    ProfileSplitterBolt bolt = new ProfileSplitterBolt("zookeeperURL");
    bolt.setCuratorFramework(client);
    bolt.setTreeCache(cache);
    bolt.getConfigurations().updateProfilerConfig(profilerConfig.getBytes("UTF-8"));
    bolt.setExecutor(new DefaultStellarExecutor());

    bolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    return bolt;
  }

  /**
   * What happens when a message is received that is needed by a profile?
   *
   * This occurs when a profile's 'onlyif' expression is true.
   */
  @Test
  public void testMessageNeededByProfile() throws Exception {

    // setup
    ProfileSplitterBolt bolt = createBolt(onlyIfTrue);

    // execute
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1)).emit(refEq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }

  /**
   * What happens when a message is received that is NOT needed by a profile?
   *
   * This occurs when a profile's 'onlyif' expression is false
   */
  @Test
  public void testMessageNotNeededByProfile() throws Exception {

    // setup
    ProfileSplitterBolt bolt = createBolt(onlyIfFalse);

    // execute
    bolt.execute(tuple);

    // a tuple should NOT be emitted for the downstream profile builder
    verify(outputCollector, times(0)).emit(any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }

  /**
   * The entity associated with a ProfileMeasurement can be defined using a variable that is resolved
   * via Stella.  In this case the entity is defined as 'ip_src_addr' which is resolved to
   * '10.0.0.1' based on the data contained within the message.
   */
  @Test
  public void testResolveEntityName() throws Exception {

    // setup
    ProfileSplitterBolt bolt = createBolt(onlyIfTrue);

    // execute
    bolt.execute(tuple);

    // verify - the entity name comes from variable resolution in stella
    String expectedEntity = "10.0.0.1";
    verify(outputCollector, times(1)).emit(any(Tuple.class), refEq(new Values(expectedEntity, onlyIfTrue, message)));
  }

  /**
   * What happens when invalid Stella code is used for 'onlyif'?
   */
  @Test(expected = org.apache.metron.common.dsl.ParseException.class)
  public void testInvalidIf() throws Exception {

    // setup
    ProfileSplitterBolt bolt = createBolt(invalidIf);

    // execute
    bolt.execute(tuple);
  }

  /**
   * If the 'onlyif' is not defined, this should be interpreted as 'true'.
   */
  @Test
  public void testMissingIf() throws Exception {

    // setup
    ProfileSplitterBolt bolt = createBolt(missingIf);

    // execute
    bolt.execute(tuple);

    // a tuple should be emitted for the downstream profile builder
    verify(outputCollector, times(1)).emit(refEq(tuple), any(Values.class));

    // the original tuple should be ack'd
    verify(outputCollector, times(1)).ack(tuple);
  }
}
