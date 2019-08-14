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
package org.apache.metron.enrichment.adapters.simplehbase;


import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SimpleHBaseAdapterTest {
  private static final String WHITELIST_ENRICHMENT = "whitelist";
  private static final String BLACKLIST_ENRICHMENT = "blacklist";

  /**
   * {
   *    "whitelist.reason": "known_host"
   * }
   */
  @Multiline
  private String expectedMessageString;

  /**
   * {
   *   "enrichment": {
   *     "fieldMap": {
   *       "hbaseEnrichment": [ "ip_dst_addr" ]
   *     },
   *     "fieldToTypeMap": {
   *       "ip_dst_addr": [ "whitelist", "blacklist" ]
   *     }
   *   }
   * }
   */
  @Multiline
  private String enrichmentJson;

  /**
   * {
   *   "enrichment": {
   *     "fieldMap": {
   *       "hbaseEnrichment": [ "ip_dst_addr" ]
   *     },
   *     "fieldToTypeMap": {
   *       "ip_dst_addr": [ "whitelist", "blacklist" ]
   *     },
   *     "config": {
   *       "typeToColumnFamily": {
   *         "blacklist": "cf1"
   *       }
   *     }
   *   }
   * }
   */
  @Multiline
  private String enrichmentJsonWithColumnFamily;

  private SensorEnrichmentConfig enrichmentConfig;
  private SensorEnrichmentConfig enrichmentConfigWithColumnFamily;
  private SimpleHBaseAdapter adapter;
  private FakeEnrichmentLookup lookup;

  @Before
  public void setup() throws Exception {
    // deserialize the enrichment configuration
    enrichmentConfig = JSONUtils.INSTANCE.load(enrichmentJson, SensorEnrichmentConfig.class);
    enrichmentConfigWithColumnFamily = JSONUtils.INSTANCE.load(enrichmentJsonWithColumnFamily, SensorEnrichmentConfig.class);

    // the enrichments are retrieved from memory, rather than HBase for these tests
    lookup = new FakeEnrichmentLookup();

    // create a 'whitelist' enrichment where the indicator is the IP address
    lookup.withEnrichment(
            new EnrichmentKey(WHITELIST_ENRICHMENT, "10.0.2.3"),
            new EnrichmentValue()
                    .withValue("hit", "true")
                    .withValue("reason", "known-host")
                    .withValue("source", "internal-asset-db")
    );

    // create a 'blacklist' enrichment where the indicator is the IP address
    lookup.withEnrichment(
            new EnrichmentKey(BLACKLIST_ENRICHMENT, "10.0.2.4"),
            new EnrichmentValue()
                    .withValue("hit", "true")
                    .withValue("reason", "known-phisher")
                    .withValue("source", "phish-tank-ip")
    );

    // initialize the adapter under test
    adapter = new SimpleHBaseAdapter()
            .withLookup(lookup);
    adapter.initializeAdapter(new HashMap<>());
  }

  @Test
  public void testWhitelistHit() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", enrichmentConfig));

    // expect a hit on the whitelist
    JSONObject expected = new JSONObject();
    expected.put("whitelist.hit", "true");
    expected.put("whitelist.reason", "known-host");
    expected.put("whitelist.source", "internal-asset-db");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testBlacklistHit() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", "10.0.2.4", enrichmentConfig));

    // expect a hit on the whitelist
    JSONObject expected = new JSONObject();
    expected.put("blacklist.hit", "true");
    expected.put("blacklist.reason", "known-phisher");
    expected.put("blacklist.source", "phish-tank-ip");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMiss() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", "4.4.4.4", enrichmentConfig));

    // not a known IP in either the whitelist or the blacklist
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testMissWithNonStringValue() {
    JSONObject actual = adapter.enrich(new CacheKey("ip_dst_addr", 10L, enrichmentConfig));

    // not a known IP in either the whitelist or the blacklist
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testNoEnrichmentDefined() {
    JSONObject actual = adapter.enrich(new CacheKey("username", "ada_lovelace", enrichmentConfig));

    // no enrichment defined for the field 'username'
    JSONObject expected = new JSONObject();
    Assert.assertEquals(expected, actual);
  }

  @Test(expected = Exception.class)
  public void testInitializeAdapter() {
    // create a configuration that will NOT try to reach out to an actual HBase cluster
    SimpleHBaseConfig config = new SimpleHBaseConfig();
    config.withConnectionFactoryImpl(FakeHBaseConnectionFactory.class.getName());

    SimpleHBaseAdapter sha = new SimpleHBaseAdapter()
            .withConfig(config);
    sha.initializeAdapter(null);
  }

  @Test
  public void testCleanup() throws Exception {
    EnrichmentLookup mockLookup = mock(EnrichmentLookup.class);
    new SimpleHBaseAdapter()
            .withLookup(mockLookup)
            .cleanup();

    // the adapter should close the EnrichmentLookup that it is using to free any resources
    verify(mockLookup, times(1)).close();
  }

}
