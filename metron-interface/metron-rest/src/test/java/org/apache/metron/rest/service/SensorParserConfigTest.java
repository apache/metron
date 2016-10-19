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
package org.apache.metron.rest.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.ConfigurationsUtils;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigurationsUtils.class})
public class SensorParserConfigTest {

  @Mock
  private GetChildrenBuilder getChildrenBuilder;

  @Mock
  private DeleteBuilder deleteBuilder;

  @Mock
  private CuratorFramework client;

  @InjectMocks
  private SensorParserConfigService sensorParserConfigService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
    Mockito.when(client.delete()).thenReturn(deleteBuilder);
  }

  @Test
  public void test() throws Exception {
    mockStatic(ConfigurationsUtils.class);
    SensorParserConfig broParserConfig = new SensorParserConfig();
    broParserConfig.setParserClassName("org.apache.metron.parsers.bro.BasicBroParser");
    broParserConfig.setSensorTopic("bro");
    sensorParserConfigService.save(broParserConfig);
    verifyStatic(times(1));
    ConfigurationsUtils.writeSensorParserConfigToZookeeper("bro", JSONUtils.INSTANCE.toJSON(broParserConfig), client);

    PowerMockito.when(ConfigurationsUtils.readSensorParserConfigFromZookeeper("bro", client)).thenReturn(broParserConfig);
    assertEquals(broParserConfig, sensorParserConfigService.findOne("bro"));

    SensorParserConfig squidParserConfig = new SensorParserConfig();
    squidParserConfig.setParserClassName("org.apache.metron.parsers.GrokParser");
    squidParserConfig.setSensorTopic("squid");
    PowerMockito.when(ConfigurationsUtils.readSensorParserConfigFromZookeeper("squid", client)).thenReturn(squidParserConfig);

    List<String> allTypes = new ArrayList<String>() {{
      add("bro");
      add("squid");
    }};
    Mockito.when(getChildrenBuilder.forPath(ConfigurationType.PARSER.getZookeeperRoot())).thenReturn(allTypes);
    assertEquals(new ArrayList<SensorParserConfig>() {{ add(broParserConfig); add(squidParserConfig); }}, sensorParserConfigService.findAll());

    Mockito.when(getChildrenBuilder.forPath(ConfigurationType.PARSER.getZookeeperRoot())).thenThrow(new KeeperException.NoNodeException());
    assertEquals(new ArrayList<>(), sensorParserConfigService.findAll());

    assertTrue(sensorParserConfigService.delete("bro"));
    verify(deleteBuilder, times(1)).forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro");
    Mockito.when(deleteBuilder.forPath(ConfigurationType.PARSER.getZookeeperRoot() + "/bro")).thenThrow(new KeeperException.NoNodeException());
    assertFalse(sensorParserConfigService.delete("bro"));
  }
}
