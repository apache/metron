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
package org.apache.metron.common.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.handler.Configs;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StellarEnrichmentConfigTest extends StellarEnrichmentTest {

  /**
   {
    "fieldMap": {
      "stellar" : {
        "config" : [
   "dga_model_endpoint := MAAS_GET_ENDPOINT('dga')",
   "dga_result_map := MAAS_MODEL_APPLY( dga_model_endpoint, { 'host' : domain_without_subdomains } )",
   "dga_result := MAP_GET('is_malicious', dga_result_map)",
   "is_dga := dga_result != null && dga_result == 'dga'",
   "dga_model_version := MAP_GET('version', dga_model_endpoint)",
   "dga_model_endpoint := null",
   "dga_result_map := null",
   "dga_result := null"
        ]
      }
    }
  }
   */
  @Multiline
  public static String conf;

  @Test
  public void testSplitter_listWithTemporaryVariables() throws IOException {
    JSONObject message = new JSONObject(ImmutableMap.of("domain_without_subdomains", "yahoo.com"));
    EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(conf, EnrichmentConfig.class);
    assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
    ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
    List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
    assertEquals(1, splits.size());
    Map<String, Object> split = (Map<String, Object>)(splits.get(0)).get("");
    assertEquals("yahoo.com", split.get("domain_without_subdomains"));
    assertTrue(split.containsKey("dga_result"));
    assertTrue(split.containsKey("dga_model_endpoint"));
    assertTrue(split.containsKey("dga_result_map"));
  }

  @Test
  public void testSplitter_default() throws IOException {
    JSONObject message = getMessage();
    for(String c : DEFAULT_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      assertEquals(1, splits.size());
      Map<String, Object> split = (Map<String, Object>) splits.get(0).get("");
      assertEquals(3, split.size());
      assertEquals("stellar_test", split.get("source.type"));
      assertEquals("foo", split.get("string"));
      assertNull(split.get("stmt1"));
    }
  }

  @Test
  public void testGetSubgroups_default() throws IOException {
    for(String c : DEFAULT_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      assertEquals("", subgroups.get(0));
      assertEquals(1, subgroups.size());
    }
  }

  @Test
  public void testSplitter_grouped() throws IOException {
    JSONObject message = getMessage();
    for(String c : GROUPED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      assertEquals(2, splits.size());
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(0).get("group1");
        assertEquals(2, split.size());
        assertEquals("stellar_test", split.get("source.type"));
        assertNull(split.get("stmt1"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(1).get("group2");
        assertEquals(1, split.size());
        assertEquals("foo", split.get("string"));
      }
    }
  }

  @Test
  public void testGetSubgroups_grouped() throws IOException {
    for(String c : GROUPED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      assertEquals("group1", subgroups.get(0));
      assertEquals("group2", subgroups.get(1));
      assertEquals(2, subgroups.size());
    }
  }


  @Test
  public void testSplitter_mixed() throws IOException {
    JSONObject message = getMessage();
    for(String c : Iterables.concat(MIXED_CONFIGS, ImmutableList.of(tempVarStellarConfig_list))) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      assertEquals(3, splits.size());
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(0).get("group1");
        assertEquals(2, split.size());
        assertEquals("stellar_test", split.get("source.type"));
        assertNull(split.get("stmt1"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(1).get("group2");
        assertEquals(1, split.size());
        assertEquals("foo", split.get("string"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(2).get("");
        assertEquals(1, split.size());
        assertEquals("stellar_test", split.get("source.type"));
      }
    }
  }

  @Test
  public void testGetSubgroups_mixed() throws IOException {
    for(String c : MIXED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      assertEquals("group1", subgroups.get(0));
      assertEquals("group2", subgroups.get(1));
      assertEquals("", subgroups.get(2));
      assertEquals(3, subgroups.size());
    }
  }
}
