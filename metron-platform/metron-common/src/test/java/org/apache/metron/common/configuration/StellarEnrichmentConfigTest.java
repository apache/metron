package org.apache.metron.common.configuration;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.common.configuration.enrichment.handler.Configs;
import org.apache.metron.common.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StellarEnrichmentConfigTest extends StellarEnrichmentTest {


  @Test
  public void testSplitter_default() throws IOException {
    JSONObject message = getMessage();
    for(String c : DEFAULT_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      Assert.assertEquals(1, splits.size());
      Map<String, Object> split = (Map<String, Object>) splits.get(0).get("");
      Assert.assertEquals(3, split.size());
      Assert.assertEquals("stellar_test", split.get("source.type"));
      Assert.assertEquals("foo", split.get("string"));
      Assert.assertNull(split.get("stmt1"));
    }
  }

  @Test
  public void testGetSubgroups_default() throws IOException {
    for(String c : DEFAULT_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      Assert.assertEquals("", subgroups.get(0));
      Assert.assertEquals(1, subgroups.size());
    }
  }

  @Test
  public void testSplitter_grouped() throws IOException {
    JSONObject message = getMessage();
    for(String c : GROUPED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      Assert.assertEquals(2, splits.size());
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(0).get("group1");
        Assert.assertEquals(2, split.size());
        Assert.assertEquals("stellar_test", split.get("source.type"));
        Assert.assertNull(split.get("stmt1"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(1).get("group2");
        Assert.assertEquals(1, split.size());
        Assert.assertEquals("foo", split.get("string"));
      }
    }
  }

  @Test
  public void testGetSubgroups_grouped() throws IOException {
    for(String c : GROUPED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      Assert.assertEquals("group1", subgroups.get(0));
      Assert.assertEquals("group2", subgroups.get(1));
      Assert.assertEquals(2, subgroups.size());
    }
  }


  @Test
  public void testSplitter_mixed() throws IOException {
    JSONObject message = getMessage();
    for(String c : Iterables.concat(MIXED_CONFIGS, ImmutableList.of(tempVarStellarConfig_list))) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<JSONObject> splits = Configs.STELLAR.splitByFields(message, null, x -> null, handler );
      Assert.assertEquals(3, splits.size());
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(0).get("group1");
        Assert.assertEquals(2, split.size());
        Assert.assertEquals("stellar_test", split.get("source.type"));
        Assert.assertNull(split.get("stmt1"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(1).get("group2");
        Assert.assertEquals(1, split.size());
        Assert.assertEquals("foo", split.get("string"));
      }
      {
        Map<String, Object> split = (Map<String, Object>) splits.get(2).get("");
        Assert.assertEquals(1, split.size());
        Assert.assertEquals("stellar_test", split.get("source.type"));
      }
    }
  }

  @Test
  public void testGetSubgroups_mixed() throws IOException {
    for(String c : MIXED_CONFIGS) {
      EnrichmentConfig enrichmentConfig = JSONUtils.INSTANCE.load(c, EnrichmentConfig.class);
      Assert.assertNotNull(enrichmentConfig.getEnrichmentConfigs().get("stellar"));
      ConfigHandler handler = enrichmentConfig.getEnrichmentConfigs().get("stellar");
      List<String> subgroups = Configs.STELLAR.getSubgroups(handler);
      Assert.assertEquals("group1", subgroups.get(0));
      Assert.assertEquals("group2", subgroups.get(1));
      Assert.assertEquals("", subgroups.get(2));
      Assert.assertEquals(3, subgroups.size());
    }
  }
}
