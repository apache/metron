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
package org.apache.metron.profiler.spark.cli;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.profiler.ProfilerConfig;
import org.apache.metron.integration.TestZKServer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BatchProfilerZKIntegrationTest {
  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile1",
   *        "foreach": "ip_src_addr",
   *        "init":   { "count": "0" },
   *        "update": { "count": "count + 1" },
   *        "result":   "count"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profile;

  @Test
  public void testProfilerZookeeperIntegration() throws Exception {
    final byte[] profileExpectedByte = profile.getBytes(StandardCharsets.UTF_8);
    final ProfilerConfig expectedProfileConfig = ProfilerConfig.fromBytes(profileExpectedByte);

    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      // write bytes to zookeeper
      ConfigurationsUtils.writeProfilerConfigToZookeeper(profileExpectedByte, zkClient);

      // read bytes from zookeeper utilizing Batch Profiler functions
      final ProfilerConfig profiles = BatchProfilerCLI.readProfileFromZK(zkClient);

      // compare expected values
      assertEquals(expectedProfileConfig, profiles, "Profile read from zookeeper has changes");
    });
  }

  @Test
  public void testProfileZookeeperIntegrationFails() throws Exception {
    final byte[] profileExpectedByte = profile.getBytes(StandardCharsets.UTF_8);
    final ProfilerConfig expectedProfileConfig = ProfilerConfig.fromBytes(profileExpectedByte);
    expectedProfileConfig.setTimestampField("foobar");

    TestZKServer.runWithZK( (zkServer, zkClient) -> {
      // write bytes to zookeeper
      ConfigurationsUtils.writeProfilerConfigToZookeeper(profileExpectedByte, zkClient);

      // read bytes from zookeeper utilizing Batch Profiler functions
      final ProfilerConfig profiles = BatchProfilerCLI.readProfileFromZK(zkClient);

      // compare expected values
      assertNotEquals(expectedProfileConfig, profiles, "Profile zookeeper integration test fails to detect change");
    });
  }
}
