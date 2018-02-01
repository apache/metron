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
package org.apache.metron.test.bolt;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.TestConstants;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;

import java.util.HashSet;
import java.util.Set;

public class BaseEnrichmentBoltTest extends BaseBoltTest {

  public static final String sampleSensorEnrichmentConfigPath = TestConstants.SAMPLE_CONFIG_PATH + "enrichments/test.json";
  public static final String sampleSensorIndexingConfigPath = TestConstants.SAMPLE_CONFIG_PATH + "indexing/test.json";
  protected Set<String> streamIds = new HashSet<>();
  protected Set<String> joinStreamIds = new HashSet<>();
  protected String key = "someKey";
  public static final String sensorType = "test";

  /**
   * {
   * "ip_src_addr": "ip1",
   * "ip_dst_addr": "ip2",
   * "source.type": "test"
   * }
   */
  @Multiline
  protected String sampleMessageString;

  /**
   * {
   * "enrichments.geo.ip_src_addr": "ip1",
   * "enrichments.geo.ip_dst_addr": "ip2",
   * "source.type": "test"
   * }
   */
  @Multiline
  protected String geoMessageString;

  /**
   * {
   * "enrichments.host.ip_src_addr": "ip1",
   * "enrichments.host.ip_dst_addr": "ip2",
   * "source.type": "test"
   * }
   */
  @Multiline
  protected String hostMessageString;

  /**
   * {
   * "enrichments.hbaseEnrichment.ip_src_addr": "ip1",
   * "enrichments.hbaseEnrichment.ip_dst_addr": "ip2",
   * "source.type": "test"
   * }
   */
  @Multiline
  protected String hbaseEnrichmentMessageString;

  protected JSONObject sampleMessage;
  protected JSONObject geoMessage;
  protected JSONObject hostMessage;
  protected JSONObject hbaseEnrichmentMessage;

  @Before
  public void parseBaseMessages() throws ParseException {
    JSONParser parser = new JSONParser();
    sampleMessage = (JSONObject) parser.parse(sampleMessageString);
    geoMessage = (JSONObject) parser.parse(geoMessageString);
    hostMessage = (JSONObject) parser.parse(hostMessageString);
    hbaseEnrichmentMessage = (JSONObject) parser.parse(hbaseEnrichmentMessageString);
    streamIds.add("geo");
    streamIds.add("stellar");
    streamIds.add("host");
    streamIds.add("hbaseEnrichment");
    joinStreamIds.add("geo:");
    joinStreamIds.add("stellar:");
    joinStreamIds.add("stellar:numeric");
    joinStreamIds.add("stellar:dst_enrichment");
    joinStreamIds.add("stellar:src_enrichment");
    joinStreamIds.add("stellar:error_test");
    joinStreamIds.add("host:");
    joinStreamIds.add("hbaseEnrichment:");
    joinStreamIds.add("message:");
  }
}
