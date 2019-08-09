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
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


public class ThreatIntelAdapterTest {

  private FakeEnrichmentLookup lookup;
  private ThreatIntelAdapter adapter;
  private SensorEnrichmentConfig config;

  /**
   * {
   *   "enrichment": {
   *   },
   *   "threatIntel": {
   *     "fieldMap": {
   *       "hbaseThreatIntel": ["ip_dst_addr" ]
   *     },
   *     "fieldToTypeMap": {
   *       "ip_dst_addr": [ "blacklist" ]
   *     }
   *   }
   * }
   */
  @Multiline
  private static String configJson;

  @Before
  public void setup() throws Exception {
    // deserialize the enrichment configuration
    config = JSONUtils.INSTANCE.load(configJson, SensorEnrichmentConfig.class);

    // create a 'blacklist' enrichment where the indicator is the IP address
    lookup = new FakeEnrichmentLookup()
            .withEnrichment(
                    new EnrichmentKey("blacklist", "10.0.2.3"),
                    new EnrichmentValue());

    // initialize the adapter under test
    adapter = new ThreatIntelAdapter()
            .withLookup(lookup);
    adapter.initializeAdapter(new HashMap<>());
  }

  @Test
  public void testBlacklistHit() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", config));

    // expect a hit on the whitelist
    JSONObject expected = new JSONObject();
    expected.put("blacklist", "alert");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMiss() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", "4.4.4.4", config));

    // not a known IP in either the whitelist or the blacklist
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMissWithNonStringValue() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", 10L, config));

    // not a known IP in either the whitelist or the blacklist
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testNoEnrichmentDefined() {
    JSONObject actual = adapter.enrich(new CacheKey("username", "ada_lovelace", config));

    // no enrichment defined for the field 'username'
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }
}
