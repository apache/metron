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

import com.fasterxml.jackson.databind.JsonMappingException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the {@link ProfileConfig} class.
 *
 * Ensures that profile definitions have the expected defaults
 * and can be (de)serialized to and from JSON.
 */
public class ProfileConfigTest {

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String onlyIfDefault;

  /**
   * The 'onlyif' field should default to 'true' when it is not specified.
   */
  @Test
  public void testFromJSONWithOnlyIfDefault() throws IOException {
    ProfileConfig profile = ProfileConfig.fromJSON(onlyIfDefault);
    assertEquals("true", profile.getOnlyif());
  }

  /**
   * Tests serializing the Profiler configuration to JSON.
   */
  @Test
  public void testToJSONWithOnlyIfDefault() throws Exception {

    // setup a profiler config to serialize
    ProfileConfig expected = ProfileConfig.fromJSON(onlyIfDefault);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfileConfig actual = ProfileConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

  /**
   * {
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String nameMissing;

  /**
   * The 'name' of the profile must be defined.
   */
  @Test
  public void testFromJSONWithNameMissing() {
    assertThrows(JsonMappingException.class, () -> ProfileConfig.fromJSON(nameMissing));
  }

  /**
   * {
   *    "profile": "test",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String foreachMissing;

  /**
   * The 'foreach' field must be defined.
   */
  @Test
  public void testFromJSONWithForeachMissing() {
    assertThrows(JsonMappingException.class, () -> ProfileConfig.fromJSON(foreachMissing));
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {}
   * }
   */
  @Multiline
  private String resultMissing;

  /**
   * The 'result' field must be defined.
   */
  @Test
  public void testFromJSONWithResultMissing() {
    assertThrows(JsonMappingException.class, () -> ProfileConfig.fromJSON(resultMissing));
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {}
   * }
   */
  @Multiline
  private String resultMissingProfileExpression;

  /**
   * The 'result' field must contain the 'profile' expression used to store the profile measurement.
   */
  @Test
  public void testFromJSONWithResultMissingProfileExpression() {
    assertThrows(JsonMappingException.class, () -> ProfileConfig.fromJSON(resultMissingProfileExpression));
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String resultWithExpression;

  /**
   * If the 'result' field has only a single expression, it should be treated as
   * the 'profile' expression used to store the profile measurement.
   */
  @Test
  public void testFromJSONWithResultWithExpression() throws IOException {
    ProfileConfig profile = ProfileConfig.fromJSON(resultWithExpression);
    assertEquals("2 + 2", profile.getResult().getProfileExpressions().getExpression());

    // no triage expressions expected
    assertEquals(0, profile.getResult().getTriageExpressions().getExpressions().size());
  }

  /**
   * Tests serializing the Profiler configuration to JSON.
   */
  @Test
  public void testToJSONWithResultWithExpression() throws Exception {

    // setup a profiler config to serialize
    ProfileConfig expected = ProfileConfig.fromJSON(resultWithExpression);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfileConfig actual = ProfileConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {
   *      "profile": "2 + 2"
   *    }
   * }
   */
  @Multiline
  private String resultWithProfileOnly;

  /**
   * The result's 'triage' field is optional.
   */
  @Test
  public void testFromJSONWithResultWithProfileOnly() throws IOException {
    ProfileConfig profile = ProfileConfig.fromJSON(resultWithProfileOnly);
    assertEquals("2 + 2", profile.getResult().getProfileExpressions().getExpression());

    // no triage expressions expected
    assertEquals(0, profile.getResult().getTriageExpressions().getExpressions().size());
  }

  /**
   * Tests serializing the Profiler configuration to JSON.
   */
  @Test
  public void testToJSONWithProfileOnly() throws Exception {

    // setup a profiler config to serialize
    ProfileConfig expected = ProfileConfig.fromJSON(resultWithProfileOnly);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfileConfig actual = ProfileConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {
   *      "profile": "2 + 2",
   *      "triage": {
   *        "eight": "4 + 4",
   *        "sixteen": "8 + 8"
   *      }
   *    }
   * }
   */
  @Multiline
  private String resultWithTriage;

  /**
   * The result's 'triage' field can contain many named expressions.
   */
  @Test
  public void testFromJSONWithResultWithTriage() throws IOException {
    ProfileConfig profile = ProfileConfig.fromJSON(resultWithTriage);

    assertEquals("4 + 4", profile.getResult().getTriageExpressions().getExpression("eight"));
    assertEquals("8 + 8", profile.getResult().getTriageExpressions().getExpression("sixteen"));
  }

  /**
   * Tests serializing the Profiler configuration to JSON.
   */
  @Test
  public void testToJSONWithResultWithTriage() throws Exception {

    // setup a profiler config to serialize
    ProfileConfig expected = ProfileConfig.fromJSON(resultWithTriage);

    // execute the test - serialize the config
    String asJson = expected.toJSON();

    // validate - deserialize to validate
    ProfileConfig actual = ProfileConfig.fromJSON(asJson);
    assertEquals(expected, actual);
  }

}
