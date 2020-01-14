/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.metron.profiler.repl;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.profiler.ProfileMeasurement;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the StandAloneProfiler class.
 */
public class StandAloneProfilerTest {

  /**
   * {
   *   "profiles": [
   *   ]
   * }
   */
  @Multiline
  private String noProfiles;

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile1",
   *        "foreach": "'global'",
   *        "init": { "count": 0 },
   *        "update": { "count": "count + 1" },
   *        "result": "count"
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
   *        "profile": "profile1",
   *        "foreach": "'global1'",
   *        "result": "'result'"
   *      },
   *      {
   *        "profile": "profile2",
   *        "foreach": "'global2'",
   *        "result": "'result'"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String twoProfiles;

  /**
   * {
   *   "ip_src_addr": "10.0.0.1",
   *   "ip_dst_addr": "10.0.0.20",
   *   "protocol": "HTTP",
   *   "timestamp": 2222222222222,
   * }
   */
  @Multiline
  private String messageJson;

  private JSONObject message;

  private long periodDurationMillis = TimeUnit.MINUTES.toMillis(15);

  private Context context = Context.EMPTY_CONTEXT();

  @BeforeEach
  public void setup() throws Exception {

    // parse the input message
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(messageJson);
  }

  @Test
  public void testWithOneProfile() throws Exception {

    StandAloneProfiler profiler = createProfiler(oneProfile);
    profiler.apply(message);
    profiler.apply(message);
    profiler.apply(message);

    List<ProfileMeasurement> measurements = profiler.flush();
    assertEquals(1, measurements.size());

    // expect 1 measurement for the 1 profile that has been defined
    ProfileMeasurement m = measurements.get(0);
    assertEquals("profile1", m.getProfileName());
    assertEquals(3, m.getProfileValue());
  }


  @Test
  public void testWithTwoProfiles() throws Exception {

    StandAloneProfiler profiler = createProfiler(twoProfiles);
    profiler.apply(message);
    profiler.apply(message);
    profiler.apply(message);

    List<ProfileMeasurement> measurements = profiler.flush();
    assertEquals(2, measurements.size());

    // expect 2 measurements, 1 for each profile
    List<String> expected = Arrays.asList(new String[] { "profile1", "profile2" });
    {
      ProfileMeasurement m = measurements.get(0);
      assertTrue(expected.contains(m.getProfileName()));
      assertEquals("result", m.getProfileValue());
    }
    {
      ProfileMeasurement m = measurements.get(1);
      assertTrue(expected.contains(m.getProfileName()));
      assertEquals("result", m.getProfileValue());
    }
  }

  /**
   * The message count and route count will always be equal, if there is only one
   * profile defined.  The message count and route count can be different when there
   * are multiple profiles defined that each use the same message.
   */
  @Test
  public void testRouteAndMessageCounters() throws Exception {
    {
      StandAloneProfiler profiler = createProfiler(noProfiles);

      profiler.apply(message);
      assertEquals(1, profiler.getMessageCount());
      assertEquals(0, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(2, profiler.getMessageCount());
      assertEquals(0, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(3, profiler.getMessageCount());
      assertEquals(0, profiler.getRouteCount());
    }
    {
      StandAloneProfiler profiler = createProfiler(oneProfile);

      profiler.apply(message);
      assertEquals(1, profiler.getMessageCount());
      assertEquals(1, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(2, profiler.getMessageCount());
      assertEquals(2, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(3, profiler.getMessageCount());
      assertEquals(3, profiler.getRouteCount());
    }
    {
      StandAloneProfiler profiler = createProfiler(twoProfiles);

      profiler.apply(message);
      assertEquals(1, profiler.getMessageCount());
      assertEquals(2, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(2, profiler.getMessageCount());
      assertEquals(4, profiler.getRouteCount());

      profiler.apply(message);
      assertEquals(3, profiler.getMessageCount());
      assertEquals(6, profiler.getRouteCount());
    }
  }

  @Test
  public void testProfileCount() throws Exception {
    {
      StandAloneProfiler profiler = createProfiler(noProfiles);
      assertEquals(0, profiler.getProfileCount());
    }
    {
      StandAloneProfiler profiler = createProfiler(oneProfile);
      assertEquals(1, profiler.getProfileCount());
    }
    {
      StandAloneProfiler profiler = createProfiler(twoProfiles);
      assertEquals(2, profiler.getProfileCount());
    }
  }

  /**
   * Creates a ProfilerConfig based on a string containing JSON.
   *
   * @param configAsJSON The config as JSON.
   * @return The ProfilerConfig.
   * @throws Exception
   */
  private ProfilerConfig toProfilerConfig(String configAsJSON) throws Exception {

    InputStream in = new ByteArrayInputStream(configAsJSON.getBytes(StandardCharsets.UTF_8));
    return JSONUtils.INSTANCE.load(in, ProfilerConfig.class);
  }

  /**
   * Creates the StandAloneProfiler
   *
   * @param profileJson The Profiler configuration to use as a String containing JSON.
   * @throws Exception
   */
  private StandAloneProfiler createProfiler(String profileJson) throws Exception {

    // the TTL and max routes need not be bounded
    long profileTimeToLiveMillis = Long.MAX_VALUE;
    long maxNumberOfRoutes = Long.MAX_VALUE;

    ProfilerConfig config = toProfilerConfig(profileJson);
    return new StandAloneProfiler(config, periodDurationMillis, profileTimeToLiveMillis, maxNumberOfRoutes, context);
  }
}
