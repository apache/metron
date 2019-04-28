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
package org.apache.metron.enrichment.adapters.stellar;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.StellarEnrichmentTest;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class StellarAdapterTest extends StellarEnrichmentTest {
  StellarProcessor processor = new StellarProcessor();

  private JSONObject enrich(JSONObject message, String field, ConfigHandler handler) {
    VariableResolver resolver = new MapVariableResolver(message);
    return StellarAdapter.process( message
                                 , handler
                                 , field
                                 , 1000L
                                 , processor
                                 , resolver
                                 , Context.EMPTY_CONTEXT()
                                 );
  }

  @Test
  public void test_default() throws Exception {
    for(String c : DEFAULT_CONFIGS) {
      JSONObject message = getMessage();
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      JSONObject enriched = enrich(message, "", handler);
      Assert.assertEquals("STELLAR_TEST", enriched.get("stmt1"));
      Assert.assertEquals("stellar_test", enriched.get("stmt2"));
      Assert.assertEquals("foo", enriched.get("stmt3"));
      Assert.assertEquals(3, enriched.size());
    }
  }

  @Test
  public void test_grouped() throws Exception {
    for(String c : GROUPED_CONFIGS) {
      JSONObject message = getMessage();
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      {
        JSONObject enriched = enrich(message, "group1", handler);
        Assert.assertEquals("STELLAR_TEST", enriched.get("stmt1"));
        Assert.assertEquals("stellar_test", enriched.get("stmt2"));
        Assert.assertEquals(2, enriched.size());
      }
      {
        JSONObject enriched = enrich(message, "group2", handler);
        Assert.assertEquals("foo", enriched.get("stmt3"));
        Assert.assertEquals(1, enriched.size());
      }
    }
  }

  @Test
  public void test_mixed() throws Exception {
    for(String c : MIXED_CONFIGS) {
      JSONObject message = getMessage();
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      {
        JSONObject enriched = enrich(message, "group1", handler);
        Assert.assertEquals("STELLAR_TEST", enriched.get("stmt1"));
        Assert.assertEquals("stellar_test", enriched.get("stmt2"));
        Assert.assertEquals(2, enriched.size());
      }
      {
        JSONObject enriched = enrich(message, "group2", handler);
        Assert.assertEquals("foo", enriched.get("stmt3"));
        Assert.assertEquals(1, enriched.size());
      }
      {
        JSONObject enriched = enrich(message, "", handler);
        Assert.assertEquals(2, enriched.get("stmt4"));
        Assert.assertEquals("stellar_test", enriched.get("stmt5"));
        Assert.assertEquals(2, enriched.size());
      }
    }
  }

  @Test
  public void test_tempVariable() throws Exception {
    JSONObject message = getMessage();
    EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(tempVarStellarConfig_list, EnrichmentConfig.class);
    Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
    ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
    {
      JSONObject enriched = enrich(message, "group1", handler);
      Assert.assertEquals("stellar_test", enriched.get("stmt2"));
      Assert.assertEquals(1, enriched.size());
    }
    {
      JSONObject enriched = enrich(message, "group2", handler);
      Assert.assertEquals("foo", enriched.get("stmt3"));
      Assert.assertEquals(1, enriched.size());
    }
    {
      JSONObject enriched = enrich(message, "", handler);
      Assert.assertEquals(2, enriched.get("stmt4"));
      Assert.assertEquals("stellar_test", enriched.get("stmt5"));
      Assert.assertEquals(2, enriched.size());
    }
  }

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "group1" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := { 'foo' : source.type }"
          ]
        }
      }
    }
  }
   */
  @Multiline
  public static String mapConfig_subgroup;
  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : [
            "stmt1 := TO_UPPER(source.type)",
            "stmt2 := { 'foo' : source.type }"
        ]
      }
    }
  }
   */
  @Multiline
  public static String mapConfig_default;


  private void testMapEnrichment(String config, String field) throws Exception {
    JSONObject message = getMessage();
    EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(config, EnrichmentConfig.class);
    Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
    ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
    JSONObject enriched = enrich(message, field, handler);
    Assert.assertEquals(2, enriched.size());
    Assert.assertEquals("stellar_test", enriched.get("stmt2.foo"));
    Assert.assertEquals("stellar_test".toUpperCase(), enriched.get("stmt1"));
  }

  @Test
  public void testMapEnrichment_subgroup() throws Exception {
    testMapEnrichment(mapConfig_subgroup, "group1");
  }

  @Test
  public void testMapEnrichment_default() throws Exception {
    testMapEnrichment(mapConfig_default, "");
  }

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : [
            "stmt1 := MAP_GET('source.type', _)"
        ]
      }
    }
  }
   */
  @Multiline
  public static String allVariableConfig;

  @Test
  public void testAllVariableUsage() throws Exception {
    JSONObject message = getMessage();
    EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(allVariableConfig, EnrichmentConfig.class);
    Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
    ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
    JSONObject enriched = enrich(message, "", handler);
    Assert.assertEquals("stellar_test", enriched.get("stmt1"));
  }

}
