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
package org.apache.metron.enrichment.bolt;

import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ThreatIntelSplitterBoltTest extends BaseEnrichmentBoltTest {

  private static final String enrichmentConfigPath = "../" + sampleSensorEnrichmentConfigPath;

  @Test
  public void test() throws IOException {
    String threatIntelType = "hbaseThreatIntel";
    ThreatIntelSplitterBolt threatIntelSplitterBolt = new ThreatIntelSplitterBolt("zookeeperUrl");
    threatIntelSplitterBolt.setCuratorFramework(client);
    threatIntelSplitterBolt.setZKCache(cache);
    threatIntelSplitterBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(enrichmentConfigPath));
    threatIntelSplitterBolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    Map<String, Object> fieldMap = threatIntelSplitterBolt.getFieldMap(sensorType);
    Assert.assertTrue(fieldMap.containsKey(threatIntelType));
    String fieldName = threatIntelSplitterBolt.getKeyName(threatIntelType, "field");
    Assert.assertEquals("threatintels.hbaseThreatIntel.field", fieldName);
  }
}
