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
package org.apache.metron.enrichment.parallel;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.enrichment.adapters.stellar.StellarAdapter;
import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelEnricherTest {
  /**
   * {
  "enrichment": {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : {
                      "map" : "{ 'blah' : 1}"
                      ,"one" : "MAP_GET('blah', map)"
                      ,"foo": "1 + 1"
                      }
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
        }
      }
    }
  ,"fieldToTypeMap": { }
  },
  "threatIntel": { }
}
   */
  @Multiline
  public static String goodConfig;

  private static ParallelEnricher enricher;
  private static Context stellarContext;
  private static AtomicInteger numAccesses = new AtomicInteger(0);
  @BeforeClass
  public static void setup() {
    ConcurrencyContext infrastructure = new ConcurrencyContext();
    infrastructure.initialize(5, 100, 10, null, null, false);
    stellarContext = new Context.Builder()
                         .build();
    StellarFunctions.initialize(stellarContext);
    StellarAdapter adapter = new StellarAdapter(){
      @Override
      public void logAccess(CacheKey value) {
        numAccesses.incrementAndGet();
      }
    }.ofType("ENRICHMENT");
    adapter.initializeAdapter(new HashMap<>());
    enricher = new ParallelEnricher(ImmutableMap.of("stellar", adapter), infrastructure, false);
  }

  @Test
  public void testCacheHit() throws Exception {
    numAccesses.set(0);
    JSONObject message = new JSONObject() {{
      put(Constants.SENSOR_TYPE, "test");
    }};
    for(int i = 0;i < 10;++i) {
      SensorEnrichmentConfig config = JSONUtils.INSTANCE.load(goodConfig, SensorEnrichmentConfig.class);
      config.getConfiguration().putIfAbsent("stellarContext", stellarContext);
      ParallelEnricher.EnrichmentResult result = enricher.apply(message, EnrichmentStrategies.ENRICHMENT, config, null);
    }
    //we only want 2 actual instances of the adapter.enrich being run due to the cache.
    Assert.assertTrue(2 >= numAccesses.get());
  }

  @Test
  public void testGoodConfig() throws Exception {
    SensorEnrichmentConfig config = JSONUtils.INSTANCE.load(goodConfig, SensorEnrichmentConfig.class);
    config.getConfiguration().putIfAbsent("stellarContext", stellarContext);
    JSONObject message = new JSONObject() {{
      put(Constants.SENSOR_TYPE, "test");
    }};
    ParallelEnricher.EnrichmentResult result = enricher.apply(message, EnrichmentStrategies.ENRICHMENT, config, null);
    JSONObject ret = result.getResult();
    Assert.assertEquals("Got the wrong result count: " + ret, 8, ret.size());
    Assert.assertEquals(1, ret.get("map.blah"));
    Assert.assertEquals("test", ret.get("source.type"));
    Assert.assertEquals(1, ret.get("one"));
    Assert.assertEquals(2, ret.get("foo"));
    Assert.assertEquals("TEST", ret.get("ALL_CAPS"));
    Assert.assertEquals(0, result.getEnrichmentErrors().size());
  }

  /**
   * {
  "enrichment": {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "numeric" : [
                      "map := { 'blah' : 1}"
                      ,"one := MAP_GET('blah', map)"
                      ,"foo := 1 + 1"
                      ]
          ,"ALL_CAPS" : "TO_UPPER(source.type)"
          ,"errors" : [
            "error := 1/0"
          ]
        }
      }
    }
  ,"fieldToTypeMap": { }
  },
  "threatIntel": { }
}
   */
  @Multiline
  public static String badConfig;

  @Test
  public void testBadConfig() throws Exception {
    SensorEnrichmentConfig config = JSONUtils.INSTANCE.load(badConfig, SensorEnrichmentConfig.class);
    config.getConfiguration().putIfAbsent("stellarContext", stellarContext);
    JSONObject message = new JSONObject() {{
      put(Constants.SENSOR_TYPE, "test");
    }};
    ParallelEnricher.EnrichmentResult result = enricher.apply(message, EnrichmentStrategies.ENRICHMENT, config, null);
    JSONObject ret = result.getResult();
    Assert.assertEquals(ret + " is not what I expected", 8, ret.size());
    Assert.assertEquals(1, ret.get("map.blah"));
    Assert.assertEquals("test", ret.get("source.type"));
    Assert.assertEquals(1, ret.get("one"));
    Assert.assertEquals(2, ret.get("foo"));
    Assert.assertEquals("TEST", ret.get("ALL_CAPS"));
    Assert.assertEquals(1, result.getEnrichmentErrors().size());
  }
}
