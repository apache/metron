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
package org.apache.metron.enrichment.adapters.threatintel;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.metron.common.configuration.SensorEnrichmentConfig;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.converter.EnrichmentHelper;
import org.apache.metron.test.mock.MockHTable;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.PersistentAccessTracker;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class ThreatIntelAdapterTest {

  public static class ExceptionProvider implements TableProvider {

    public ExceptionProvider() {};

    @Override
    public HTableInterface getTable(Configuration config, String tableName) throws IOException {
      throw new IOException();
    }
  }

  private String cf = "cf";
  private String atTableName = "tracker";
  private static final String MALICIOUS_IP_TYPE = "malicious_ip";
  private final String threatIntelTableName = "threat_intel";
  private EnrichmentLookup lookup;

  /**
   * {
   * "10.0.2.3":"alert"
   * }
   */
  @Multiline
  private String expectedMessageString;

  /**
   * {
   * "index": "bro",
   * "batchSize": 5,
   * "enrichmentFieldMap": {
   * "geo": ["ip_dst_addr", "ip_src_addr"],
   * "host": ["host"]
   * },
   * "threatIntelFieldMap": {
   * "hbaseThreatIntel": ["ip_dst_addr", "ip_src_addr"]
   * },
   * "fieldToThreatIntelTypeMap": {
   * "ip_dst_addr" : [ "10.0.2.3" ],
   * "ip_src_addr" : [ "malicious_ip" ]
   * }
   * }
   */
  @Multiline
  private static String sourceConfigStr;

  private JSONObject expectedMessage;

  @Before
  public void setup() throws Exception {

    final MockHTable trackerTable = (MockHTable) MockHTable.Provider.addToCache(atTableName, cf);
    final MockHTable threatIntelTable = (MockHTable) MockHTable.Provider.addToCache(threatIntelTableName, cf);
    EnrichmentHelper.INSTANCE.load(threatIntelTable, cf, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>() {{
      add(new LookupKV<>(new EnrichmentKey("10.0.2.3", "10.0.2.3"), new EnrichmentValue(new HashMap<String, String>())));
    }});

    BloomAccessTracker bat = new BloomAccessTracker(threatIntelTableName, 100, 0.03);
    PersistentAccessTracker pat = new PersistentAccessTracker(threatIntelTableName, "0", trackerTable, cf, bat, 0L);
    lookup = new EnrichmentLookup(threatIntelTable, cf, pat);
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
  }


  @Test
  public void testEnrich() throws Exception {
    ThreatIntelAdapter tia = new ThreatIntelAdapter();
    tia.lookup = lookup;
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = tia.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(expectedMessage, actualMessage);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializeAdapter() {

    String cf = "cf";
    String table = "threatintel";
    String trackCf = "cf";
    String trackTable = "Track";
    double falsePositive = 0.03;
    int expectedInsertion = 1;
    long millionseconds = (long) 0.1;

    ThreatIntelConfig config = new ThreatIntelConfig();
    config.withHBaseCF(cf);
    config.withHBaseTable(table);
    config.withExpectedInsertions(expectedInsertion);
    config.withFalsePositiveRate(falsePositive);
    config.withMillisecondsBetweenPersists(millionseconds);
    config.withTrackerHBaseCF(trackCf);
    config.withTrackerHBaseTable(trackTable);
    config.withProviderImpl(ExceptionProvider.class.getName());

    ThreatIntelAdapter tia = new ThreatIntelAdapter(config);
    tia.initializeAdapter();

  }


}
