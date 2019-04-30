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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatTriageConfig;
import org.apache.metron.storm.common.message.MessageGetStrategy;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.adapters.maxmind.asn.GeoLiteAsnDatabase;
import org.apache.metron.enrichment.adapters.maxmind.geo.GeoLiteCityDatabase;
import org.apache.metron.test.bolt.BaseEnrichmentBoltTest;
import org.apache.metron.test.utils.UnitTestHelper;
import org.apache.storm.tuple.Tuple;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ThreatIntelJoinBoltTest extends BaseEnrichmentBoltTest {

  private static final String enrichmentConfigPath = "../" + sampleSensorEnrichmentConfigPath;

  /**
   * {
   * "field1": "value1",
   * "enrichedField1": "enrichedValue1",
   * "source.type": "test"
   * }
   */
  @Multiline
  private String messageString;

  /**
   * {
   * "field1": "value1",
   * "enrichedField1": "enrichedValue1",
   * "source.type": "test",
   * "threatintels.field.end.ts": "timing"
   * }
   */
  @Multiline
  private String messageWithTimingString;

  /**
   * {
   * "field1": "value1",
   * "enrichedField1": "enrichedValue1",
   * "source.type": "test",
   * "threatintels.field": "threatIntelValue"
   * }
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

  /**
   * {
   *  "riskLevelRules" : [
   *   {
   *    "rule" : "enrichedField1 == 'enrichedValue1'",
   *    "score" : 10
   *   }
   *  ],
   *  "aggregator" : "MAX"
   * }
   */
  @Multiline
  private static String testWithTriageConfig;

  @Test
  public void testWithTriage() throws IOException {
    test(testWithTriageConfig, false);
  }

  /**
   * {
   *  "riskLevelRules" : [
   *  {
   *    "rule" : "enrichedField1 == 'enrichedValue1",
   *    "score" : 10
   *  }
   *  ],
   *  "aggregator" : "MAX"
   * }
   */
  @Multiline
  private static String testWithBadTriageRuleConfig;

  @Test
  public void testWithBadTriageRule() throws IOException {
    test(testWithBadTriageRuleConfig, true);
  }

  @Test
  public void testWithoutTriage() throws IOException {
    test(null, false);
  }

  /**
   * {
   *   "riskLevelRules": [
   *   {
   *      "rule" : "not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))",
   *      "score" : 10
   *   }
   *   ],
   *   "aggregator": "MAX"
   * }
   */
  @Multiline
  private static String testWithStellarFunctionConfig;

  @Test
  public void testWithStellarFunction() throws IOException {
    test(testWithStellarFunctionConfig, false);
  }

  public void test(String threatTriageConfig, boolean badConfig) throws IOException {

    ThreatIntelJoinBolt threatIntelJoinBolt = new ThreatIntelJoinBolt("zookeeperUrl");
    threatIntelJoinBolt.setCuratorFramework(client);
    threatIntelJoinBolt.setZKCache(cache);

    SensorEnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(
            new FileInputStream(enrichmentConfigPath), SensorEnrichmentConfig.class);
    boolean withThreatTriage = threatTriageConfig != null;
    if (withThreatTriage) {
      try {
        enrichmentConfig.getThreatIntel().setTriageConfig(JSONUtils.INSTANCE.load(threatTriageConfig, ThreatTriageConfig.class));
        if (badConfig) {
          Assert.fail(threatTriageConfig + "\nThis should not parse!");
        }
      } catch (JsonMappingException pe) {
        if (!badConfig) {
          throw pe;
        }
      }
    }
    threatIntelJoinBolt.getConfigurations().updateSensorEnrichmentConfig(sensorType, enrichmentConfig);
    HashMap<String, Object> globalConfig = new HashMap<>();
    String baseDir = UnitTestHelper.findDir(new File("../metron-enrichment-common"), "GeoLite");
    File geoHdfsFile = new File(new File(baseDir), "GeoLite2-City.mmdb.gz");
    globalConfig.put(GeoLiteCityDatabase.GEO_HDFS_FILE, geoHdfsFile.getAbsolutePath());
    File asnHdfsFile = new File(new File(baseDir), "GeoLite2-ASN.tar.gz");
    globalConfig.put(GeoLiteAsnDatabase.ASN_HDFS_FILE, asnHdfsFile.getAbsolutePath());
    threatIntelJoinBolt.getConfigurations().updateGlobalConfig(globalConfig);
    threatIntelJoinBolt.withMaxCacheSize(100);
    threatIntelJoinBolt.withMaxTimeRetain(10000);
    threatIntelJoinBolt.prepare(new HashMap<>(), topologyContext, outputCollector);

    Map<String, Object> fieldMap = threatIntelJoinBolt.getFieldMap("incorrectSourceType");
    Assert.assertNull(fieldMap);

    fieldMap = threatIntelJoinBolt.getFieldMap(sensorType);
    Assert.assertTrue(fieldMap.containsKey("hbaseThreatIntel"));

    MessageGetStrategy messageGetStrategy = mock(MessageGetStrategy.class);
    Tuple messageTuple = mock(Tuple.class);
    when(messageGetStrategy.get(messageTuple)).thenReturn(message);
    Map<String, Tuple> streamMessageMap = new HashMap<>();
    streamMessageMap.put("message", messageTuple);
    JSONObject joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap, messageGetStrategy);
    assertFalse(joinedMessage.containsKey("is_alert"));

    when(messageGetStrategy.get(messageTuple)).thenReturn(messageWithTiming);
    joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap, messageGetStrategy);
    assertFalse(joinedMessage.containsKey("is_alert"));

    when(messageGetStrategy.get(messageTuple)).thenReturn(alertMessage);
    joinedMessage = threatIntelJoinBolt.joinMessages(streamMessageMap, messageGetStrategy);
    assertTrue(joinedMessage.containsKey("is_alert") && "true".equals(joinedMessage.get("is_alert")));

    if(withThreatTriage && !badConfig) {
      assertTrue(joinedMessage.containsKey("threat.triage.score"));
      Double score = (Double) joinedMessage.get("threat.triage.score");
      assertTrue(Math.abs(10d - score) < 1e-10);
    }
    else {
      assertFalse(joinedMessage.containsKey("threat.triage.score"));
    }
  }
}
