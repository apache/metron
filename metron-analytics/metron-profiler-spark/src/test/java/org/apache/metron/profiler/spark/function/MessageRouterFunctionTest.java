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
package org.apache.metron.profiler.spark.function;

import com.google.common.collect.Lists;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.profiler.MessageRoute;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tests the {@link MessageRouterFunction}.
 */
public class MessageRouterFunctionTest {

  private Long messageTimestamp = 1537468508360L;

  /**
   * { "ip_src_addr": "192.168.1.22", "timestamp": 1537468508360 }
   */
  @Multiline
  private String goodMessage;

  /**
   * { "ip_src_addr": "192.168.1.22"
   */
  private String badMessage;

  /**
   * { "ip_src_addr": "192.168.1.22" }
   */
  @Multiline
  private String messageNoTimestamp;

  @Test
  public void testFindRoutes() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals());
    Iterator<MessageRoute> iter = function.call(goodMessage);

    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(1, routes.size());
    Assert.assertEquals("profile1", routes.get(0).getProfileDefinition().getProfile());
  }

  @Test(expected = IllegalStateException.class)
  public void testWithSystemTime() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profileWithSystemTime(), getGlobals());
    Iterator<MessageRoute> iter = function.call(goodMessage);

    Assert.fail("Exception expected as system time is not supported.");
  }

  @Test
  public void testWithBadMessage() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals());
    Iterator<MessageRoute> iter = function.call(badMessage);

    // an invalid message should return no routes
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(0, routes.size());
  }

  @Test
  public void testFindMultipleRoutes() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(twoProfiles(), getGlobals());
    Iterator<MessageRoute> iter = function.call(goodMessage);

    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(2, routes.size());
    Assert.assertEquals("profile1", routes.get(0).getProfileDefinition().getProfile());
    Assert.assertEquals("profile2", routes.get(1).getProfileDefinition().getProfile());
  }

  @Test
  public void testWithNoTimestampInMessage() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals());
    Iterator<MessageRoute> iter = function.call(messageNoTimestamp);

    // with no timestamp, the message should be ignored
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(0, routes.size());
  }

  @Test
  public void testMessageFilteredByBegin() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withBegin(messageTimestamp + 1000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should be filtered because it is before `beginAt`
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(0, routes.size());
  }

  @Test
  public void testMessageNotFilteredByBegin() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withBegin(messageTimestamp - 1000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should NOT be filtered because it is after 'beginAt'
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(1, routes.size());
  }

  @Test
  public void testMessageFilteredByEnd() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withEnd(messageTimestamp - 1000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should be filtered because it is after 'endAt'
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(0, routes.size());
  }

  @Test
  public void testMessageNotFilteredByEnd() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withEnd(messageTimestamp + 1000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should NOT be filtered because it is before 'endAt'
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(1, routes.size());
  }

  @Test
  public void testMessageFilteredByBeginAndEnd() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withBegin(messageTimestamp + 1000)
            .withEnd(messageTimestamp + 2000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should be filtered because it is outside of [beginAt, endAt]
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(0, routes.size());
  }

  @Test
  public void testMessageNotFilteredByBeginAndEnd() throws Exception {
    MessageRouterFunction function = new MessageRouterFunction(profile(), getGlobals())
            .withBegin(messageTimestamp - 1000)
            .withEnd(messageTimestamp + 1000);
    Iterator<MessageRoute> iter = function.call(goodMessage);

    // the message should NOT be filtered because it is after 'endAt'
    List<MessageRoute> routes = Lists.newArrayList(iter);
    Assert.assertEquals(1, routes.size());
  }

  /**
   * Creates a profiler definition, using event time, and containing one profile.
   */
  private ProfilerConfig profile() {
    ProfileConfig profile = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");

    return new ProfilerConfig()
            .withProfile(profile)
            .withTimestampField(Optional.of("timestamp"));
  }

  /**
   * Creates a profiler definition, using event time, and containing two profiles.
   */
  private ProfilerConfig twoProfiles() {
    ProfileConfig profile1 = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");
    ProfileConfig profile2 = new ProfileConfig()
            .withProfile("profile2")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");
    return new ProfilerConfig()
            .withProfile(profile1)
            .withProfile(profile2)
            .withTimestampField(Optional.of("timestamp"));
  }

  /**
   * Creates a profiler definition using system time.
   */
  private ProfilerConfig profileWithSystemTime() {
    ProfileConfig profile = new ProfileConfig()
            .withProfile("profile1")
            .withForeach("ip_src_addr")
            .withUpdate("count", "count + 1")
            .withResult("count");

    return new ProfilerConfig()
            .withProfile(profile);
  }

  private Map<String, String> getGlobals() {
    return Collections.emptyMap();
  }
}
