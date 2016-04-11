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
package org.apache.metron.domain;

import junit.framework.Assert;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.metron.utils.ConfigurationsUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class SensorEnrichmentConfigTest {

  public String sampleConfigRoot = "../Metron-Testing/src/main/resources/sample/config/";

  @Test
  public void test() throws IOException {
    EqualsVerifier.forClass(SensorEnrichmentConfig.class).suppress(Warning.NONFINAL_FIELDS).usingGetClass().verify();
    Map<String, byte[]> testSensorConfigMap = ConfigurationsUtils.readSensorEnrichmentConfigsFromFile(sampleConfigRoot);
    byte[] sensorConfigBytes = testSensorConfigMap.get("yaf");
    SensorEnrichmentConfig sensorEnrichmentConfig = SensorEnrichmentConfig.fromBytes(sensorConfigBytes);
    Assert.assertNotNull(sensorEnrichmentConfig);
    Assert.assertTrue(sensorEnrichmentConfig.toString() != null && sensorEnrichmentConfig.toString().length() > 0);
  }
}
