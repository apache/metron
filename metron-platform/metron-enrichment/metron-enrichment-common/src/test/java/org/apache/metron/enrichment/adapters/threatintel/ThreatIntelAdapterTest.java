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
import org.apache.log4j.Level;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookupFactory;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.test.utils.UnitTestHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


public class ThreatIntelAdapterTest {

  private SensorEnrichmentConfig config;
  private static final String MALICIOUS_IP_TYPE = "malicious_ip";
  private FakeEnrichmentLookup lookup;
  private ThreatIntelAdapter threatIntelAdapter;
  private ThreatIntelConfig threatIntelConfig;

  /**
    {
    "10.0.2.3":"alert"
    }
   */
  @Multiline
  private String expectedMessageString;

  /**
    {
      "enrichment": {
        "fieldMap": {
          "geo": ["ip_dst_addr", "ip_src_addr"],
          "host": ["host"]
        }
      },
      "threatIntel" : {
        "fieldMap": {
          "hbaseThreatIntel": ["ip_dst_addr", "ip_src_addr"]
        },
        "fieldToTypeMap": {
          "ip_dst_addr" : [ "10.0.2.3" ],
          "ip_src_addr" : [ "malicious_ip" ]
        }
      }
    }
   */
  @Multiline
  private static String sourceConfigStr;

  private JSONObject expectedMessage;

  @Before
  public void setup() throws Exception {
    // deserialize the enrichment configuration
    config = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);

    // create an enrichment where the indicator is the IP address
    lookup = new FakeEnrichmentLookup()
            .withEnrichment(
                    new EnrichmentKey("10.0.2.3", "10.0.2.3"),
                    new EnrichmentValue());

    threatIntelConfig = new ThreatIntelConfig()
            .withHBaseTable("enrichment")
            .withHBaseCF("cf")
            .withTrackerHBaseTable("tracker")
            .withTrackerHBaseCF("cf")
            .withConnectionFactory(new FakeHBaseConnectionFactory())
            .withEnrichmentLookupFactory(new FakeEnrichmentLookupFactory(lookup));

    threatIntelAdapter = new ThreatIntelAdapter(threatIntelConfig);
    threatIntelAdapter.lookup = lookup;
    threatIntelAdapter.initializeAdapter(new HashMap<>());

    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
  }

  @Test
  public void testEnrich() throws Exception {
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = threatIntelAdapter.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testEnrichNonString() throws Exception {
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = threatIntelAdapter.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(expectedMessage, actualMessage);

    actualMessage = threatIntelAdapter.enrich(new CacheKey("ip_dst_addr", 10L, broSc));
    Assert.assertEquals(actualMessage,new JSONObject());
  }

  @Test
  public void testMiss() {
    JSONObject actual = threatIntelAdapter.enrich(new CacheKey("ip_dst_addr", "4.4.4.4", config));

    // not a known IP in either the enrichment data
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMissWithNonStringValue() {
    JSONObject actual = threatIntelAdapter.enrich(new CacheKey("ip_dst_addr", 10L, config));

    // not a known IP in either the enrichment data
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testNoEnrichmentDefined() {
    JSONObject actual = threatIntelAdapter.enrich(new CacheKey("username", "ada_lovelace", config));

    // no enrichment defined for the field 'username'
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
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
    config.withEnrichmentLookupFactory(new FakeEnrichmentLookupFactory(lookup));
    config.withConnectionFactory(new FakeHBaseConnectionFactory());

    ThreatIntelAdapter tia = new ThreatIntelAdapter(config);
    UnitTestHelper.setLog4jLevel(ThreatIntelAdapter.class, Level.FATAL);
    tia.initializeAdapter(null);
    UnitTestHelper.setLog4jLevel(ThreatIntelAdapter.class, Level.ERROR);
    Assert.assertTrue(tia.isInitialized());
  }


}
