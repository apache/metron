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
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class SensorIndexingConfigServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  ObjectMapper objectMapper;
  CuratorFramework curatorFramework;
  SensorIndexingConfigService sensorIndexingConfigService;

  /**
   {
   "hdfs" : {
   "index": "bro",
   "batchSize": 5,
   "enabled" : true
   }
   }
   */
  @Multiline
  public static String broJson;

  @Before
  public void setUp() throws Exception {
    objectMapper = mock(ObjectMapper.class);
    curatorFramework = mock(CuratorFramework.class);
    sensorIndexingConfigService = new SensorIndexingConfigServiceImpl(objectMapper, curatorFramework);
  }


  @Test
  public void deleteShouldProperlyCatchNoNodeExceptionAndReturnFalse() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenThrow(KeeperException.NoNodeException.class);

    assertFalse(sensorIndexingConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldProperlyCatchNonNoNodeExceptionAndThrowRestException() throws Exception {
    exception.expect(RestException.class);

    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    assertFalse(sensorIndexingConfigService.delete("bro"));
  }

  @Test
  public void deleteShouldReturnTrueWhenClientSuccessfullyCallsDelete() throws Exception {
    DeleteBuilder builder = mock(DeleteBuilder.class);

    when(curatorFramework.delete()).thenReturn(builder);
    when(builder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenReturn(null);

    assertTrue(sensorIndexingConfigService.delete("bro"));

    verify(curatorFramework).delete();
  }

  @Test
  public void findOneShouldProperlyReturnSensorEnrichmentConfig() throws Exception {
    final Map<String, Object> sensorIndexingConfig = getTestSensorIndexingConfig();

    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenReturn(broJson.getBytes());
    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    assertEquals(getTestSensorIndexingConfig(), sensorIndexingConfigService.findOne("bro"));
  }

  @Test
  public void findOneShouldReturnNullWhenNoNodeExceptionIsThrown() throws Exception {
    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenThrow(KeeperException.NoNodeException.class);

    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    assertNull(sensorIndexingConfigService.findOne("bro"));
  }

  @Test
  public void findOneShouldWrapNonNoNodeExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenThrow(Exception.class);

    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    sensorIndexingConfigService.findOne("bro");
  }

  @Test
  public void getAllTypesShouldProperlyReturnTypes() throws Exception {
    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot()))
            .thenReturn(new ArrayList() {{
              add("bro");
              add("squid");
            }});
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    assertEquals(new ArrayList() {{
      add("bro");
      add("squid");
    }}, sensorIndexingConfigService.getAllTypes());
  }

  @Test
  public void getAllTypesShouldReturnNullWhenNoNodeExceptionIsThrown() throws Exception {
    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot())).thenThrow(KeeperException.NoNodeException.class);
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    assertEquals(new ArrayList<>(), sensorIndexingConfigService.getAllTypes());
  }

  @Test
  public void getAllTypesShouldWrapNonNoNodeExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot())).thenThrow(Exception.class);
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);

    sensorIndexingConfigService.getAllTypes();
  }

  @Test
  public void getAllShouldProperlyReturnSensorEnrichmentConfigs() throws Exception {
    final Map<String, Object> sensorIndexingConfig = getTestSensorIndexingConfig();

    GetChildrenBuilder getChildrenBuilder = mock(GetChildrenBuilder.class);
    when(getChildrenBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot()))
            .thenReturn(new ArrayList() {{
              add("bro");
            }});
    when(curatorFramework.getChildren()).thenReturn(getChildrenBuilder);
    GetDataBuilder getDataBuilder = mock(GetDataBuilder.class);
    when(getDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro")).thenReturn(broJson.getBytes());
    when(curatorFramework.getData()).thenReturn(getDataBuilder);

    assertEquals(new HashMap() {{ put("bro", sensorIndexingConfig);}}, sensorIndexingConfigService.getAll());
  }

  @Test
  public void saveShouldWrapExceptionInRestException() throws Exception {
    exception.expect(RestException.class);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro", broJson.getBytes())).thenThrow(Exception.class);

    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    sensorIndexingConfigService.save("bro", new HashMap<>());
  }

  @Test
  public void saveShouldReturnSameConfigThatIsPassedOnSuccessfulSave() throws Exception {
    final Map<String, Object> sensorIndexingConfig = getTestSensorIndexingConfig();

    when(objectMapper.writeValueAsString(sensorIndexingConfig)).thenReturn(broJson);

    SetDataBuilder setDataBuilder = mock(SetDataBuilder.class);
    when(setDataBuilder.forPath(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro", broJson.getBytes())).thenReturn(new Stat());
    when(curatorFramework.setData()).thenReturn(setDataBuilder);

    assertEquals(sensorIndexingConfig, sensorIndexingConfigService.save("bro", sensorIndexingConfig));
    verify(setDataBuilder).forPath(eq(ConfigurationType.INDEXING.getZookeeperRoot() + "/bro"), eq(broJson.getBytes()));
  }

  private Map<String, Object> getTestSensorIndexingConfig() {
    Map<String, Object> sensorIndexingConfig = new HashMap<>();
    sensorIndexingConfig.put("hdfs", new HashMap(){{
      put("index", "bro");
      put("batchSize", 5);
      put("enabled", true);
    }});
    return sensorIndexingConfig;
  }
 }
