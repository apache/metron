/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.configuration;

import org.adrianwalker.multilinestring.Multiline;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IndexingConfigurationsTest {

  /**
   * {
   *  "indexing.writer.elasticsearch.setDocumentId" : "true"
   * }
   */
  @Multiline
  private static String globalConfig;

  /**
   * {
   *  "writer" : {
   *    "setDocumentId": true
   *  }
   * }
   */
  @Multiline
  private static String sensorConfig;

  private IndexingConfigurations configurations;

  @Before
  public void setup() {
    configurations = new IndexingConfigurations();
  }

  @Test
  public void shouldReturnDefaultSetDocumentId() throws Exception {
    // verify false by default
    assertFalse(configurations.isSetDocumentId("sensor", "writer"));
  }

  @Test
  public void shouldReturnGlobalSetDocumentId() throws Exception {
    // verify global config setting applies to any sensor
    configurations.updateGlobalConfig(globalConfig.getBytes(StandardCharsets.UTF_8));

    assertTrue(configurations.isSetDocumentId("sensor", "writer"));
    assertTrue(configurations.isSetDocumentId("anySensor", "writer"));
  }

  @Test
  public void shouldReturnSensorSetDocumentId() throws Exception {
    // verify sensor config only applies to that sensor
    configurations.updateGlobalConfig(new HashMap<>());
    configurations.updateSensorIndexingConfig("sensor", sensorConfig.getBytes(StandardCharsets.UTF_8));

    assertTrue(configurations.isSetDocumentId("sensor", "writer"));
    assertFalse(configurations.isSetDocumentId("anySensor", "writer"));
  }
}
