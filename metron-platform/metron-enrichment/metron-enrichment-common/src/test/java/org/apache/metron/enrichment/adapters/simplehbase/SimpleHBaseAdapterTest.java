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


import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookup;
import org.apache.metron.enrichment.lookup.FakeEnrichmentLookupFactory;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SimpleHBaseAdapterTest {
  private SimpleHBaseConfig config;
  private FakeEnrichmentLookup lookup;
  private static final String PLAYFUL_CLASSIFICATION_TYPE = "playful_classification";
  private static final String CF1_CLASSIFICATION_TYPE = "cf1";
  private static final Map<String, Object> CF1_ENRICHMENT = new HashMap<String, Object>() {{
    put("key", "value");
  }};
  private static final Map<String, Object> PLAYFUL_ENRICHMENT = new HashMap<String, Object>() {{
    put("orientation", "north");
  }};

  /**
    {
    "playful_classification.orientation":"north"
    }
   */
  @Multiline
  private String expectedMessageString;

  /**
    {
      "enrichment": {
        "fieldMap": {
           "hbaseEnrichment" : [ "ip_dst_addr" ]
        },
      "fieldToTypeMap": {
        "ip_dst_addr" : [ "playful_classification", "cf1" ]
        }
      }
   }
   */
  @Multiline
  private String sourceConfigStr;
  /**
    {
      "enrichment": {
        "fieldMap": {
           "hbaseEnrichment" : [ "ip_dst_addr" ]
        },
      "fieldToTypeMap": {
        "ip_dst_addr" : [ "playful_classification", "cf1" ]
        },
      "config" : {
          "typeToColumnFamily" : {
                        "cf1" : "cf1"
                                 }
                }
      }
   }
   */
  @Multiline
  private String sourceConfigWithCFStr;
  private JSONObject expectedMessage;




  @Before
  public void setup() throws Exception {
    // the enrichments are retrieved from memory, rather than HBase for these tests
    lookup = new FakeEnrichmentLookup();
    lookup.deleteAll();
    lookup.withEnrichment(
            new EnrichmentKey(PLAYFUL_CLASSIFICATION_TYPE, "10.0.2.3"),
            new EnrichmentValue(PLAYFUL_ENRICHMENT));
    lookup.withEnrichment(
            new EnrichmentKey(CF1_CLASSIFICATION_TYPE, "10.0.2.4"),
            new EnrichmentValue(CF1_ENRICHMENT));

    config = new SimpleHBaseConfig()
            .withHBaseTable("enrichment")
            .withHBaseCF("cf")
            .withEnrichmentLookupFactory(new FakeEnrichmentLookupFactory(lookup));

    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
  }

  @Test
  public void testEnrich() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.lookup = lookup;
    sha.initializeAdapter(new HashMap<>());
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testEnrichNonStringValue() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.lookup = lookup;
    sha.initializeAdapter(new HashMap<>());
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", 10L, broSc));
    Assert.assertEquals(actualMessage,new JSONObject());
  }

  @Test
  public void testMultiColumnFamilies() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.lookup = lookup;
    sha.initializeAdapter(new HashMap<>());
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigWithCFStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", "10.0.2.4", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(new JSONObject(ImmutableMap.of("cf1.key", "value")), actualMessage);
  }

  @Test(expected = Exception.class)
  public void testInitializeAdapter() {
    SimpleHBaseConfig config = new SimpleHBaseConfig();
    config.withConnectionFactoryImpl(FakeHBaseConnectionFactory.class.getName());
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.initializeAdapter(null);
  }

  @Test
  public void testCleanup() throws Exception {
    EnrichmentLookup mockLookup = mock(EnrichmentLookup.class);
    config.withEnrichmentLookupFactory((v,w,x,y,z) -> mockLookup);

    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.initializeAdapter(new HashMap<>());
    sha.cleanup();

    // the adapter should close the EnrichmentLookup that it is using to free any resources
    verify(mockLookup, times(1)).close();
  }

}
