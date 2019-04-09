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
package org.apache.metron.profiler.spark.function;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.profiler.MessageRoute;
import org.apache.metron.profiler.ProfilePeriod;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class GroupByPeriodFunctionTest {

  /**
   * {
   *    "profile": "my-profile-name",
   *    "foreach": "'total'",
   *    "init": { "count": 0 },
   *    "update": { "count": "count + 1" },
   *    "result": "count"
   * }
   */
  @Multiline
  private String profileJSON;

  @Test
  public void shouldDecodeGroupKey() throws Exception {
    final ProfileConfig profile = ProfileConfig.fromJSON(profileJSON);
    final Long timestamp = System.currentTimeMillis();
    final String entity = "192.168.1.1";
    final JSONObject message = new JSONObject();
    final String periodId = new Long(ProfilePeriod.fromTimestamp(timestamp, 15, TimeUnit.MINUTES).getPeriod()).toString();

    MessageRoute route = new MessageRoute(profile, entity, message, timestamp);
    String groupKey = new GroupByPeriodFunction(new Properties()).call(route);

    // should be able to extract the profile, entity and period from the group key
    Assert.assertEquals("my-profile-name", GroupByPeriodFunction.profileFromKey(groupKey));
    Assert.assertEquals(entity, GroupByPeriodFunction.entityFromKey(groupKey));
    Assert.assertEquals(periodId, GroupByPeriodFunction.periodFromKey(groupKey));
  }

}
