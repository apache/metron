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
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultMessageRouterTest {

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
   *   "profiles": [ ]
   * }
   */
  @Multiline
  private String noProfiles;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile-one",
   *        "foreach": "ip_src_addr",
   *        "init":   { "x": "0" },
   *        "update": { "x": "x + 1" },
   *        "result": "x"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String oneProfile;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile-one",
   *        "foreach": "ip_src_addr",
   *        "init":   { "x": "0" },
   *        "update": { "x": "x + 1" },
   *        "result": "x"
   *      },
   *      {
   *        "profile": "profile-two",
   *        "foreach": "ip_src_addr",
   *        "init":   { "x": "0" },
   *        "update": { "x": "x + 1" },
   *        "result": "x"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String twoProfiles;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile-one",
   *        "onlyif": "false",
   *        "foreach": "ip_src_addr",
   *        "init":   { "x": "0" },
   *        "update": { "x": "x + 1" },
   *        "result": "x"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String exclusiveProfile;

  private DefaultMessageRouter router;
  private Context context;

  /**
   * Creates a profile definition based on a string of JSON.
   * @param json The string of JSON.
   */
  private ProfilerConfig createConfig(String json) throws IOException {
    return JSONUtils.INSTANCE.load(json, ProfilerConfig.class);
  }

  @Before
  public void setup() throws Exception {
    this.router = new DefaultMessageRouter(Context.EMPTY_CONTEXT());
    this.context = Context.EMPTY_CONTEXT();
    JSONParser parser = new JSONParser();
    this.messageOne = (JSONObject) parser.parse(inputOne);
    this.messageTwo = (JSONObject) parser.parse(inputTwo);
  }

  @Test
  public void testWithOneRoute() throws Exception {
    List<MessageRoute> routes = router.route(messageOne, createConfig(oneProfile), context);

    assertEquals(1, routes.size());
    MessageRoute route1 = routes.get(0);
    assertEquals(messageOne.get("ip_src_addr"), route1.getEntity());
    assertEquals("profile-one", route1.getProfileDefinition().getProfile());
  }

  @Test
  public void testWithNoRoutes() throws Exception {
    List<MessageRoute> routes = router.route(messageOne, createConfig(noProfiles), context);
    assertEquals(0, routes.size());
  }

  @Test
  public void testWithTwoRoutes() throws Exception {
    List<MessageRoute> routes = router.route(messageOne, createConfig(twoProfiles), context);

    assertEquals(2, routes.size());
    {
      MessageRoute route1 = routes.get(0);
      assertEquals(messageOne.get("ip_src_addr"), route1.getEntity());
      assertEquals("profile-one", route1.getProfileDefinition().getProfile());
    }
    {
      MessageRoute route2 = routes.get(1);
      assertEquals(messageOne.get("ip_src_addr"), route2.getEntity());
      assertEquals("profile-two", route2.getProfileDefinition().getProfile());
    }
  }

  /**
   * The 'onlyif' condition should exclude some messages from being routed to a profile.
   */
  @Test
  public void testExclusiveProfile() throws Exception {
    List<MessageRoute> routes = router.route(messageOne, createConfig(exclusiveProfile), context);
    assertEquals(0, routes.size());
  }
}
