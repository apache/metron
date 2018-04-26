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
  private String profile;

  /**
   * Tests deserializing the Profiler configuration using the fromJSON(...) method.
   */
  @Test
  public void testFromJSON() throws IOException {
    ProfilerConfig conf = ProfilerConfig.fromJSON(profile);

    assertFalse(conf.getTimestampField().isPresent());
    assertEquals(1, conf.getProfiles().size());
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
   *   ]
   * }
   */
  @Multiline
  private String noTimestampField;

  /**
   * If no 'timestampField' is defined, it should not be present by default.
   */
  @Test
  public void testFromJSONWithNoTimestampField() throws IOException {
    ProfilerConfig conf = ProfilerConfig.fromJSON(noTimestampField);
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
   * Tests deserializing the Profiler configuration when the timestamp field is defined.
   */
  @Test
  public void testFromJSONWithTimestampField() throws IOException {
    ProfilerConfig conf = ProfilerConfig.fromJSON(timestampField);

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

  @Test
  public void testFromJSONTwoProfiles() throws IOException {
    ProfilerConfig conf = ProfilerConfig.fromJSON(twoProfiles);

    assertEquals(2, conf.getProfiles().size());
    assertFalse(conf.getTimestampField().isPresent());
  }

  /**
   * Tests serializing the Profiler configuration to JSON.
   */
  @Test
  public void testToJSON() throws Exception {

    // setup a profiler config to serialize
    ProfilerConfig expected = ProfilerConfig.fromJSON(profile);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfilerConfig actual = ProfilerConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

  /**
   * {
   *   "profiles": [
   *      {
   *        "profile": "profile1",
   *        "foreach": "ip_src_addr",
   *        "init":   { "count": "0" },
   *        "update": { "count": "count + 1" },
   *        "result": {
   *          "profile": "count",
   *          "triage" : { "count": "count" }
   *        }
   *      }
   *   ]
   * }
   */
  @Multiline
  private String profileWithTriageExpression;

  @Test
  public void testToJSONWithTriageExpression() throws Exception {

    // setup a profiler config to serialize
    ProfilerConfig expected = ProfilerConfig.fromJSON(profileWithTriageExpression);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfilerConfig actual = ProfilerConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

  @Test
  public void testToJSONWithTwoProfiles() throws Exception {

    // setup a profiler config to serialize
    ProfilerConfig expected = ProfilerConfig.fromJSON(twoProfiles);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfilerConfig actual = ProfilerConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }
}
