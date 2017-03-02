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
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.enrichment.EnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SensorEnrichmentConfigService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Before
  public void setUp() throws Exception {
    objectMapper = mock(ObjectMapper.class);
    curatorFramework = mock(CuratorFramework.class);
    sensorEnrichmentConfigService = new SensorEnrichmentConfigServiceImpl(objectMapper, curatorFramework);
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

    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenReturn(broJson.getBytes());
    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    assertEquals(getTestSensorEnrichmentConfig(), sensorEnrichmentConfigService.findOne("bro"));
  }

  @Test
  public void findOneShouldReturnNullWhenNoNodeExceptionIsThrown() throws Exception {
    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenThrow(KeeperException.NoNodeException.class);

    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    assertNull(sensorEnrichmentConfigService.findOne("bro"));
  }

  @Test
  public void findOneShouldWrapNonNoNodeExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    sensorEnrichmentConfigService.findOne("bro");
  }

  @Test
  public void getAllTypesShouldProperlyReturnTypes() throws Exception {
    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot()))
            .thenReturn(new ArrayList() {{
              add("bro");
              add("squid");
            }});
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    assertEquals(new ArrayList() {{
      add("bro");
      add("squid");
    }}, sensorEnrichmentConfigService.getAllTypes());
  }

  @Test
  public void getAllTypesShouldReturnNullWhenNoNodeExceptionIsThrown() throws Exception {
    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot())).thenThrow(KeeperException.NoNodeException.class);
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    assertEquals(new ArrayList<>(), sensorEnrichmentConfigService.getAllTypes());
  }

  @Test
  public void getAllTypesShouldWrapNonNoNodeExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot())).thenThrow(Exception.class);
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    sensorEnrichmentConfigService.getAllTypes();
  }

  @Test
  public void getAllShouldProperlyReturnSensorEnrichmentConfigs() throws Exception {
    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot()))
            .thenReturn(new ArrayList() {{
              add("bro");
            }});
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    final SensorEnrichmentConfig sensorEnrichmentConfig = getTestSensorEnrichmentConfig();
    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.ENRICHMENT.getZookeeperRoot() + "/bro")).thenReturn(broJson.getBytes());
    when(curatorFramework.getData()).thenReturn(getDataBuilder);

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
  public void getAvailableEnrichmentsShouldReturnEnrichments() throws Exception {
    assertEquals(new ArrayList<String>() {{
      add("geo");
      add("host");
      add("whois");
    }}, sensorEnrichmentConfigService.getAvailableEnrichments());
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
