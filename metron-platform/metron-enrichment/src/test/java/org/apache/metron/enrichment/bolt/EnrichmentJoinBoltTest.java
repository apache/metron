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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnrichmentJoinBoltTest extends BaseEnrichmentBoltTest {

  /**
   * {
   * "enrichedField": "enrichedValue",
   * "emptyEnrichedField": ""
   * }
   */
  @Multiline
  private String enrichedMessageString;

  /**
   * {
   * "ip_src_addr": "ip1",
   * "ip_dst_addr": "ip2",
   * "source.type": "test",
   * "enrichedField": "enrichedValue"
   * }
   */
  @Multiline
  private String expectedJoinedMessageString;

  private JSONObject enrichedMessage;
  private JSONObject expectedJoinedMessage;

  @Before
  public void parseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    enrichedMessage = (JSONObject) parser.parse(enrichedMessageString);
    expectedJoinedMessage = (JSONObject) parser.parse(expectedJoinedMessageString);
  }

  @Test
  public void test() throws IOException {
    EnrichmentJoinBolt enrichmentJoinBolt = new EnrichmentJoinBolt("zookeeperUrl");
    enrichmentJoinBolt.setCuratorFramework(client);
    enrichmentJoinBolt.setTreeCache(cache);
    enrichmentJoinBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(sampleSensorEnrichmentConfigPath));
    enrichmentJoinBolt.withMaxCacheSize(100);
    enrichmentJoinBolt.withMaxTimeRetain(10000);
    enrichmentJoinBolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    Set<String> actualStreamIds = enrichmentJoinBolt.getStreamIds(sampleMessage);
    streamIds.add("message");
    Assert.assertEquals(streamIds, actualStreamIds);
    Map<String, JSONObject> streamMessageMap = new HashMap<>();
    streamMessageMap.put("message", sampleMessage);
    streamMessageMap.put("enriched", enrichedMessage);
    JSONObject joinedMessage = enrichmentJoinBolt.joinMessages(streamMessageMap);
    removeTimingFields(joinedMessage);
    Assert.assertEquals(expectedJoinedMessage, joinedMessage);
  }
}
