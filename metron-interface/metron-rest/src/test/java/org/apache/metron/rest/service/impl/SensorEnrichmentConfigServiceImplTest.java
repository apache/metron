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
package org.apache.metron.rest.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.hbase.client.HBaseClient;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SensorEnrichmentConfigService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SuppressWarnings("ALL")
public class SensorEnrichmentConfigServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  ObjectMapper objectMapper;
  CuratorFramework curatorFramework;
  SensorEnrichmentConfigService sensorEnrichmentConfigService;

  /**
   {
   "enrichment" : {
   "fieldMap": {
   "geo": ["ip_dst_addr"]
   }
   },
   "threatIntel": {
   "fieldMap": {
   "hbaseThreatIntel": ["ip_src_addr"]
   },
   "fieldToTypeMap": {
   "ip_src_addr" : ["malicious_ip"]
   }
   }
   }
   */
  @Multiline
  public static String broJson;

  ConfigurationsCache cache;
  private HBaseClient hBaseClient;

  @Before
  public void setUp() throws Exception {
    objectMapper = mock(ObjectMapper.class);
    curatorFramework = mock(CuratorFramework.class);
    cache = mock(ConfigurationsCache.class);
    hBaseClient = mock(HBaseClient.class);
    sensorEnrichmentConfigService = new SensorEnrichmentConfigServiceImpl(objectMapper, curatorFramework, cache, hBaseClient);
  }


  @Test
  public void deleteShouldProperlyCatchNoNodeExceptionAndReturnFalse() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenThrow(KeeperException.NoNodeException.class);

    assertFalse(sensorEnrichmentConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldProperlyCatchNonNoNodeExceptionAndThrowRestException() throws Exception {
    exception.expect(RestException.class);

    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    assertFalse(sensorEnrichmentConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldReturnTrueWhenClientSuccessfullyCallsDelete() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenReturn(null);

    assertTrue(sensorEnrichmentConfigService.delete("bro"));

    verify(curatorFramework).delete();
  }

  @Test
  public void findOneShouldProperlyReturnSensorEnrichmentConfig() throws Exception {
    final SensorEnrichmentConfig sensorEnrichmentConfig = getTestSensorEnrichmentConfig();

    EnrichmentConfigurations configs = new EnrichmentConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(EnrichmentConfigurations.getKey("bro"), sensorEnrichmentConfig);
      }
    };
    when(cache.get(eq(EnrichmentConfigurations.class)))
            .thenReturn(configs);

    //We only have bro, so we should expect it to be returned
    assertEquals(getTestSensorEnrichmentConfig(), sensorEnrichmentConfigService.findOne("bro"));
    //and blah should be a miss.
    assertNull(sensorEnrichmentConfigService.findOne("blah"));
  }

  @Test
  public void getAllTypesShouldProperlyReturnTypes() throws Exception {

    EnrichmentConfigurations configs = new EnrichmentConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(EnrichmentConfigurations.getKey("bro"), new HashMap<>()
                              ,EnrichmentConfigurations.getKey("squid"), new HashMap<>()
                              );
      }
    };
    when(cache.get(eq(EnrichmentConfigurations.class)))
            .thenReturn(configs);

    assertEquals(new ArrayList() {{
      add("bro");
      add("squid");
    }}, sensorEnrichmentConfigService.getAllTypes());

  }


  @Test
  public void getAllShouldProperlyReturnSensorEnrichmentConfigs() throws Exception {
    final SensorEnrichmentConfig sensorEnrichmentConfig = getTestSensorEnrichmentConfig();
    EnrichmentConfigurations configs = new EnrichmentConfigurations(){
      @Override
      public Map<String, Object> getConfigurations() {
        return ImmutableMap.of(EnrichmentConfigurations.getKey("bro"), sensorEnrichmentConfig);
      }
    };
    when(cache.get( eq(EnrichmentConfigurations.class)))
            .thenReturn(configs);

    assertEquals(new HashMap() {{ put("bro", sensorEnrichmentConfig);}}, sensorEnrichmentConfigService.getAll());
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro", broJson.getBytes())).thenThrow(Exception.class);

    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    sensorEnrichmentConfigService.save("bro", new SensorEnrichmentConfig());
  }

  @Test
  public void saveShouldReturnSameConfigThatIsPassedOnSuccessfulSave() throws Exception {
    final SensorEnrichmentConfig sensorEnrichmentConfig = getTestSensorEnrichmentConfig();

    when(objectMapper.writeValueAsString(sensorEnrichmentConfig)).thenReturn(broJson);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro", broJson.getBytes())).thenReturn(new Stat());
    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    assertEquals(sensorEnrichmentConfig, sensorEnrichmentConfigService.save("bro", sensorEnrichmentConfig));
    verify(setDataBuilder).forPath(eq(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro"), eq(broJson.getBytes()));
  }

  @Test
  public void getAvailableEnrichmentsShouldReturnEnrichmentsSorted() throws Exception {
    when(hBaseClient.readRecords()).thenReturn(new ArrayList<String>() {{
      add("geo");
      add("whois");
      add("host");
      add("a-new-one");
    }});
    assertEquals(new ArrayList<String>() {{
      add("a-new-one");
      add("geo");
      add("host");
      add("whois");
    }}, sensorEnrichmentConfigService.getAvailableEnrichments());
  }

  @Test
  public void getAvailableThreatTriageAggregatorsShouldReturnAggregators() throws Exception {
    assertEquals(new ArrayList<String>() {{
      add("MAX");
      add("MIN");
      add("SUM");
      add("MEAN");
      add("POSITIVE_MEAN");
    }}, sensorEnrichmentConfigService.getAvailableThreatTriageAggregators());
  }

  private SensorEnrichmentConfig getTestSensorEnrichmentConfig() {
    SensorEnrichmentConfig sensorEnrichmentConfig = new SensorEnrichmentConfig();
    EnrichmentConfig enrichmentConfig = new EnrichmentConfig();
    enrichmentConfig.setFieldMap(new HashMap() {{ put("geo", Arrays.asList("ip_dst_addr")); }});
    sensorEnrichmentConfig.setEnrichment(enrichmentConfig);
    ThreatIntelConfig threatIntelConfig = new ThreatIntelConfig();
    threatIntelConfig.setFieldMap(new HashMap() {{ put("hbaseThreatIntel", Arrays.asList("ip_src_addr")); }});
    threatIntelConfig.setFieldToTypeMap(new HashMap() {{ put("ip_src_addr", Arrays.asList("malicious_ip")); }});
    sensorEnrichmentConfig.setThreatIntel(threatIntelConfig);
    return sensorEnrichmentConfig;
  }
 }
