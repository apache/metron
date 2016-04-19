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

import junit.framework.Assert;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.bolt.BaseEnrichmentBoltTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreatIntelJoinBoltTest extends BaseEnrichmentBoltTest {

  /**
   {
   "field1": "value1",
   "enrichedField1": "enrichedValue1",
   "source.type": "yaf"
   }
   */
  @Multiline
  private String messageString;

  /**
   {
   "field1": "value1",
   "enrichedField1": "enrichedValue1",
   "source.type": "yaf",
   "threatintels.field.end.ts": "timing"
   }
   */
  @Multiline
  private String messageWithTimingString;

  /**
   {
   "field1": "value1",
   "enrichedField1": "enrichedValue1",
   "source.type": "yaf",
   "threatintels.field": "threatIntelValue"
   }
   */
  @Multiline
  private String alertMessageString;

  private JSONObject message;
  private JSONObject messageWithTiming;
  private JSONObject alertMessage;

  @Before
  public void parseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    message = (JSONObject) parser.parse(messageString);
    messageWithTiming = (JSONObject) parser.parse(messageWithTimingString);
    alertMessage = (JSONObject) parser.parse(alertMessageString);
  }

  @Test
  public void test() throws IOException {
    ThreatIntelJoinBolt threatIntelJoinBolt = new ThreatIntelJoinBolt("zookeeperUrl");
    threatIntelJoinBolt.setCuratorFramework(client);
    threatIntelJoinBolt.setTreeCache(cache);
    threatIntelJoinBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, new FileInputStream(sampleSensorEnrichmentConfigPath));
    threatIntelJoinBolt.withMaxCacheSize(100);
    threatIntelJoinBolt.withMaxTimeRetain(10000);
    threatIntelJoinBolt.prepare(new HashMap<>(), topologyContext, outputCollector);
    Map<String, List<String>> fieldMap = threatIntelJoinBolt.getFieldMap("incorrectSourceType");
    Assert.assertNull(fieldMap);
    fieldMap = threatIntelJoinBolt.getFieldMap(sensorType);
    Assert.assertTrue(fieldMap.containsKey("hbaseThreatIntel"));
    Map<String, JSONObject> streamMessageMap = new HashMap<>();
    streamMessageMap.put("message", message);
    JSONObject joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap);
    Assert.assertFalse(joinedMessage.containsKey("is_alert"));
    streamMessageMap.put("message", messageWithTiming);
    joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap);
    Assert.assertFalse(joinedMessage.containsKey("is_alert"));
    streamMessageMap.put("message", alertMessage);
    joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap);
    Assert.assertTrue(joinedMessage.containsKey("is_alert") && "true".equals(joinedMessage.get("is_alert")));
  }
}
