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
package org.apache.metron.common.configuration.profiler;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link ProfilerConfig} class.
 */
public class ProfilerConfigTest {

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
  private String noTimestampField;

  /**
   * If no 'timestampField' is defined, it should not be present by default.
   */
  @Test
  public void testNoTimestampField() throws IOException {
    ProfilerConfig conf = JSONUtils.INSTANCE.load(noTimestampField, ProfilerConfig.class);
    assertFalse(conf.getTimestampField().isPresent());
  }

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
   *   ],
   *   "timestampField": "timestamp"
   * }
   */
  @Multiline
  private String timestampField;

  /**
   * If no 'timestampField' is defined, it should not be present by default.
   */
  @Test
  public void testTimestampField() throws IOException {
    ProfilerConfig conf = JSONUtils.INSTANCE.load(timestampField, ProfilerConfig.class);
    assertTrue(conf.getTimestampField().isPresent());
  }

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile1",
   *        "foreach": "ip_src_addr",
   *        "init":   { "count": "0" },
   *        "update": { "count": "count + 1" },
   *        "result":   "count"
   *      },
   *      {
   *        "profile": "profile2",
   *        "foreach": "ip_dst_addr",
   *        "init":   { "count": "0" },
   *        "update": { "count": "count + 1" },
   *        "result":   "count"
   *      }
   *   ]
   * }
   */
  @Multiline
  private String twoProfiles;

  /**
   * The 'onlyif' field should default to 'true' when it is not specified.
   */
  @Test
  public void testTwoProfiles() throws IOException {
    ProfilerConfig conf = JSONUtils.INSTANCE.load(twoProfiles, ProfilerConfig.class);
    assertEquals(2, conf.getProfiles().size());
  }

}
