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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SensorEnrichmentConfigServiceImplTest {
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

  @BeforeEach
  public void setUp() {
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
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    assertThrows(RestException.class, () -> sensorEnrichmentConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldReturnTrueWhenClientSuccessfullyCallsDelete() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    doNothing().when(builder).forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro");

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

    assertEquals(new ArrayList<String>() {{
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

    assertEquals(Collections.singletonMap("bro", sensorEnrichmentConfig), sensorEnrichmentConfigService.getAll());
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro", broJson.getBytes(
        StandardCharsets.UTF_8))).thenThrow(Exception.class);

    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    assertThrows(RestException.class, () -> sensorEnrichmentConfigService.save("bro", new SensorEnrichmentConfig()));
  }

  @Test
  public void saveShouldReturnSameConfigThatIsPassedOnSuccessfulSave() throws Exception {
    final SensorEnrichmentConfig sensorEnrichmentConfig = getTestSensorEnrichmentConfig();

    when(objectMapper.writeValueAsString(sensorEnrichmentConfig)).thenReturn(broJson);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro", broJson.getBytes(StandardCharsets.UTF_8))).thenReturn(new Stat());
    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    assertEquals(sensorEnrichmentConfig, sensorEnrichmentConfigService.save("bro", sensorEnrichmentConfig));
    verify(setDataBuilder).forPath(eq(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro"), eq(broJson.getBytes(StandardCharsets.UTF_8)));
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
  public void getAvailableThreatTriageAggregatorsShouldReturnAggregators() {
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
    enrichmentConfig.setFieldMap(Collections.singletonMap("geo", Collections.singletonList("ip_dst_addr")));
    sensorEnrichmentConfig.setEnrichment(enrichmentConfig);
    ThreatIntelConfig threatIntelConfig = new ThreatIntelConfig();
    threatIntelConfig.setFieldMap(Collections.singletonMap("hbaseThreatIntel", Collections.singletonList("ip_src_addr")));
    threatIntelConfig.setFieldToTypeMap(Collections.singletonMap("ip_src_addr", Collections.singletonList("malicious_ip")));
    sensorEnrichmentConfig.setThreatIntel(threatIntelConfig);
    return sensorEnrichmentConfig;
  }
 }
