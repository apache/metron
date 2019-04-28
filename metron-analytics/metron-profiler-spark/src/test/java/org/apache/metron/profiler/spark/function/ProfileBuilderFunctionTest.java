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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfilePeriod;
import org.apache.metron.profiler.spark.ProfileMeasurementAdapter;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION;
import static org.apache.metron.profiler.spark.BatchProfilerConfig.PERIOD_DURATION_UNITS;

public class ProfileBuilderFunctionTest {

  /**
   * {
   *    "profile": "total-count",
   *    "foreach": "'total'",
   *    "init": { "count": 0 },
   *    "update": { "count": "count + 1" },
   *    "result": "count"
   * }
   */
  @Multiline
  private String profileJSON;

  @Test
  public void shouldBuildProfileMeasurement() throws Exception {
    // setup the message and profile
    JSONObject message = getMessage();
    String entity = "192.168.1.1";
    long timestamp = (Long) message.get("timestamp");
    ProfileConfig profile = ProfileConfig.fromJSON(profileJSON);

    // setup the route
    MessageRoute route = new MessageRoute(profile, entity, message, timestamp);
    List<MessageRoute> routes = new ArrayList();
    routes.add(route);
    routes.add(route);
    routes.add(route);
    Properties profilerProperties = getProfilerProperties();

    // setup the period
    int periodDuration = PERIOD_DURATION.get(profilerProperties, Integer.class);
    TimeUnit periodDurationUnits = TimeUnit.valueOf(PERIOD_DURATION_UNITS.get(profilerProperties, String.class));
    ProfilePeriod expectedPeriod = ProfilePeriod.fromTimestamp(timestamp, periodDuration, periodDurationUnits);

    // build the profile
    ProfileBuilderFunction function = new ProfileBuilderFunction(profilerProperties, getGlobals());
    ProfileMeasurementAdapter measurement = function.call("profile1-192.168.1.1-0", routes.iterator());

    // validate the measurement
    Assert.assertEquals(entity, measurement.getEntity());
    Assert.assertEquals(profile.getProfile(), measurement.getProfileName());
    Assert.assertEquals(routes.size(), measurement.toProfileMeasurement().getProfileValue());
    Assert.assertEquals(expectedPeriod.getPeriod(), (long) measurement.getPeriodId());
  }

  /**
   * {
   *    "profile": "total-count",
   *    "foreach": "'total'",
   *    "init": { "count": 0 },
   *    "update": { "count": "count + 1" },
   *    "result": "INVALID_FUNCTION(count)"
   * }
   */
  @Multiline
  private static String invalidProfileJson;

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfInvalidProfile() throws Exception {
    // setup the message and profile
    JSONObject message = getMessage();
    String entity = "192.168.1.1";
    long timestamp = (Long) message.get("timestamp");
    ProfileConfig profile = ProfileConfig.fromJSON(invalidProfileJson);

    // setup the route
    MessageRoute route = new MessageRoute(profile, entity, message, timestamp);
    List<MessageRoute> routes = new ArrayList();
    routes.add(route);
    routes.add(route);
    routes.add(route);
    Properties profilerProperties = getProfilerProperties();

    // an exception should be thrown, if there is a bug in the profile definition
    ProfileBuilderFunction function = new ProfileBuilderFunction(profilerProperties, getGlobals());
    ProfileMeasurementAdapter measurement = function.call("profile1-192.168.1.1-0", routes.iterator());
  }

  private JSONObject getMessage() {
    JSONObject message = new JSONObject();
    message.put("ip_src_addr", "192.168.1.1");
    message.put("status", "red");
    message.put("timestamp", System.currentTimeMillis());
    return message;
  }

  private Properties getProfilerProperties() {
    return new Properties();
  }

  private Map<String, String> getGlobals() {
    return Collections.emptyMap();
  }
}
