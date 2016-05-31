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
import org.apache.metron.enrichment.bolt.CacheKey;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SimpleHBaseAdapterTest {

  private String cf = "cf";
  private String cf1 = "cf1";
  private String atTableName = "tracker";
  private final String hbaseTableName = "enrichments";
  private EnrichmentLookup lookup;
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
      "index": "bro",
      "batchSize": 5,
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
      "index": "bro",
      "batchSize": 5,
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
    final MockHTable trackerTable = (MockHTable) MockHTable.Provider.addToCache(atTableName, cf);
    final MockHTable hbaseTable = (MockHTable) MockHTable.Provider.addToCache(hbaseTableName, cf);
    EnrichmentHelper.INSTANCE.load(hbaseTable, cf, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>() {{
      add(new LookupKV<>(new EnrichmentKey(PLAYFUL_CLASSIFICATION_TYPE, "10.0.2.3")
                      , new EnrichmentValue(PLAYFUL_ENRICHMENT)
              )
      );
    }});
    EnrichmentHelper.INSTANCE.load(hbaseTable, cf1, new ArrayList<LookupKV<EnrichmentKey, EnrichmentValue>>() {{
      add(new LookupKV<>(new EnrichmentKey(CF1_CLASSIFICATION_TYPE, "10.0.2.4")
                      , new EnrichmentValue(CF1_ENRICHMENT)
              )
      );
    }});
    BloomAccessTracker bat = new BloomAccessTracker(hbaseTableName, 100, 0.03);
    PersistentAccessTracker pat = new PersistentAccessTracker(hbaseTableName, "0", trackerTable, cf, bat, 0L);
    lookup = new EnrichmentLookup(hbaseTable, cf, pat);
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
  }

  @Test
  public void testEnrich() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter();
    sha.lookup = lookup;
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", "10.0.2.3", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testMultiColumnFamilies() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter();
    sha.lookup = lookup;
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigWithCFStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", "10.0.2.4", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(new JSONObject(ImmutableMap.of("cf1.key", "value")), actualMessage);
  }

  @Test
  public void testMultiColumnFamiliesWrongCF() throws Exception {
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter();
    sha.lookup = lookup;
    SensorEnrichmentConfig broSc = JSONUtils.INSTANCE.load(sourceConfigStr, SensorEnrichmentConfig.class);
    JSONObject actualMessage = sha.enrich(new CacheKey("test", "test", broSc));
    Assert.assertEquals(actualMessage, new JSONObject());
    actualMessage = sha.enrich(new CacheKey("ip_dst_addr", "10.0.2.4", broSc));
    Assert.assertNotNull(actualMessage);
    Assert.assertEquals(new JSONObject(new HashMap<String, Object>()), actualMessage);
  }
  @Test(expected = Exception.class)
  public void testInitializeAdapter() {
    SimpleHBaseConfig config = new SimpleHBaseConfig();
    SimpleHBaseAdapter sha = new SimpleHBaseAdapter(config);
    sha.initializeAdapter();
  }

}
