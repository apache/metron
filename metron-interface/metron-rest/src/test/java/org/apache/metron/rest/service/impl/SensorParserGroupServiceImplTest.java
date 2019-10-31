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

import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.metron.common.configuration.ParserConfigurations.PARSER_GROUPS_CONF;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SensorParserGroupServiceImplTest {
  private ConfigurationsCache cache;
  private GlobalConfigService globalConfigService;
  private SensorParserConfigService sensorParserConfigService;
  private SensorParserGroupService sensorParserGroupService;

  @BeforeEach
  public void setUp() {
    cache = mock(ConfigurationsCache.class);
    globalConfigService = mock(GlobalConfigService.class);
    sensorParserConfigService = mock(SensorParserConfigService.class);
    sensorParserGroupService = new SensorParserGroupServiceImpl(cache, globalConfigService, sensorParserConfigService);
  }

  @Test
  public void shouldSaveNewGroup() throws Exception {
    when(cache.get(ParserConfigurations.class)).thenReturn(new ParserConfigurations());
    when(sensorParserConfigService.findOne("bro")).thenReturn(new SensorParserConfig());

    SensorParserGroup sensorParserGroup = new SensorParserGroup();
    sensorParserGroup.setName("group1");
    sensorParserGroup.setDescription("description 1");
    sensorParserGroup.setSensors(Collections.singleton("bro"));

    Map<String, Object> expectedGlobalConfig = new HashMap<>();
    Collection<SensorParserGroup> expectedGroup = Collections.singleton(sensorParserGroup);
    expectedGlobalConfig.put(PARSER_GROUPS_CONF, expectedGroup);

    assertEquals(sensorParserGroup, sensorParserGroupService.save(sensorParserGroup));
    verify(globalConfigService, times(1)).save(expectedGlobalConfig);

    verifyNoMoreInteractions(globalConfigService);
  }

  @Test
  public void shouldSaveExistingGroup() throws Exception {
    SensorParserGroup oldGroup = new SensorParserGroup();
    oldGroup.setName("oldGroup");
    oldGroup.setDescription("old description");
    oldGroup.setSensors(Collections.singleton("oldSensor"));

    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);
    when(cache.get(ParserConfigurations.class)).thenReturn(new ParserConfigurations());
    when(parserConfigurations.getSensorParserGroups()).thenReturn(new HashMap<String, SensorParserGroup>() {{
      put("newSensor", oldGroup);
    }});
    when(sensorParserConfigService.findOne("newSensor")).thenReturn(new SensorParserConfig());

    SensorParserGroup newGroup = new SensorParserGroup();
    newGroup.setName("newGroup");
    newGroup.setDescription("new description");
    newGroup.setSensors(Collections.singleton("newSensor"));

    Map<String, Object> expectedGlobalConfig = new HashMap<>();
    Collection<SensorParserGroup> expectedGroup = Collections.singleton(newGroup);
    expectedGlobalConfig.put(PARSER_GROUPS_CONF, expectedGroup);

    assertEquals(newGroup, sensorParserGroupService.save(newGroup));
    verify(globalConfigService, times(1)).save(expectedGlobalConfig);

    verifyNoMoreInteractions(globalConfigService);
  }

  @Test
  public void saveShouldThrowExceptionOnMissingSensor() {
    when(cache.get(ParserConfigurations.class)).thenReturn(new ParserConfigurations());

    RestException e = assertThrows(RestException.class, () -> sensorParserGroupService.save(new SensorParserGroup()));
    assertEquals("A parser group must contain sensors", e.getMessage());
  }

  @Test
  public void saveShouldThrowExceptionOnMissingConfig() {
    when(cache.get(ParserConfigurations.class)).thenReturn(new ParserConfigurations());

    SensorParserGroup sensorParserGroup = new SensorParserGroup();
    sensorParserGroup.setSensors(Collections.singleton("bro"));

    RestException e = assertThrows(RestException.class, () -> sensorParserGroupService.save(sensorParserGroup));
    assertEquals("Could not find config for sensor bro", e.getMessage());
  }

  @Test
  public void saveShouldThrowExceptionOnSensorInAnotherGroup() throws Exception {
    SensorParserGroup existingGroup = new SensorParserGroup();
    existingGroup.setName("existingGroup");
    existingGroup.setSensors(Collections.singleton("bro"));
    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);
    when(parserConfigurations.getSensorParserGroups()).thenReturn(new HashMap<String, SensorParserGroup>() {{
      put("existingGroup", existingGroup);
    }});
    when(cache.get(ParserConfigurations.class)).thenReturn(parserConfigurations);
    when(sensorParserConfigService.findOne("bro")).thenReturn(new SensorParserConfig());

    SensorParserGroup newGroup = new SensorParserGroup();
    newGroup.setName("newGroup");
    newGroup.setSensors(Collections.singleton("bro"));

    RestException e = assertThrows(RestException.class, () -> sensorParserGroupService.save(newGroup));
    assertEquals("Sensor bro is already in group existingGroup", e.getMessage());
  }

  @Test
  public void shouldFindSensorParserGroup() {
    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);
    SensorParserGroup group1 = new SensorParserGroup();
    group1.setName("group1");
    group1.setDescription("group1 description");
    group1.setSensors(Collections.singleton("group1Sensor"));
    SensorParserGroup group2 = new SensorParserGroup();
    group2.setName("group2");
    group2.setDescription("group2 description");
    group2.setSensors(Collections.singleton("group2Sensor"));
    when(parserConfigurations.getSensorParserGroups()).thenReturn(new HashMap<String, SensorParserGroup>() {{
      put("group1", group1);
      put("group2", group2);
    }});
    when(cache.get(ParserConfigurations.class)).thenReturn(parserConfigurations);

    assertEquals(group2, sensorParserGroupService.findOne("group2"));
  }


  @Test
  public void shouldDeleteSensorParserGroup() throws Exception {
    ParserConfigurations parserConfigurations = mock(ParserConfigurations.class);
    SensorParserGroup group1 = new SensorParserGroup();
    group1.setName("group1");
    group1.setDescription("group1 description");
    group1.setSensors(Collections.singleton("group1Sensor"));
    when(parserConfigurations.getSensorParserGroups()).thenReturn(new HashMap<String, SensorParserGroup>() {{
      put("group1", group1);
    }});
    when(cache.get(ParserConfigurations.class)).thenReturn(parserConfigurations);

    Map<String, Object> expectedGlobalConfig = new HashMap<>();
    expectedGlobalConfig.put(PARSER_GROUPS_CONF, new HashSet<>());

    assertTrue(sensorParserGroupService.delete("group1"));
    assertFalse(sensorParserGroupService.delete("group2"));

    verify(globalConfigService, times(1)).save(expectedGlobalConfig);
    verifyNoMoreInteractions(globalConfigService);
  }


}
