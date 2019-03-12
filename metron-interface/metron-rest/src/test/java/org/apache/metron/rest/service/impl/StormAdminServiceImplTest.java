/*
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

import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.apache.metron.rest.service.StormAdminService;
import org.apache.metron.rest.service.StormStatusService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class StormAdminServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  StormCLIWrapper stormCLIClientWrapper;
  StormAdminService stormAdminService;
  GlobalConfigService globalConfigService;
  SensorParserConfigService sensorParserConfigService;
  SensorParserGroupService sensorParserGroupService;
  StormStatusService stormStatusService;

  @Before
  public void setUp() throws Exception {
    stormCLIClientWrapper = mock(StormCLIWrapper.class);
    globalConfigService = mock(GlobalConfigService.class);
    sensorParserConfigService = mock(SensorParserConfigService.class);
    sensorParserGroupService = mock(SensorParserGroupService.class);
    stormStatusService = mock(StormStatusService.class);
    stormAdminService = new StormAdminServiceImpl(stormCLIClientWrapper, globalConfigService, sensorParserConfigService, sensorParserGroupService, stormStatusService);
  }

  @Test
  public void startParserTopologyShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    when(stormCLIClientWrapper.startParserTopology("bro")).thenReturn(0);
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>());
    when(sensorParserConfigService.findOne("bro")).thenReturn(new SensorParserConfig());

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STARTED.toString());
    TopologyResponse actual = stormAdminService.startParserTopology("bro");

    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void startParserTopologyByGroupShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    SensorParserGroup group = new SensorParserGroup();
    group.setName("group");
    group.setSensors(new HashSet<String>() {{
      add("bro");
      add("snort");
    }});
    when(sensorParserGroupService.findOne("group")).thenReturn(group);
    when(stormCLIClientWrapper.startParserTopology("bro,snort")).thenReturn(0);
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>());
    when(sensorParserConfigService.findOne("bro")).thenReturn(new SensorParserConfig());
    when(sensorParserConfigService.findOne("snort")).thenReturn(new SensorParserConfig());

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STARTED.toString());
    TopologyResponse actual = stormAdminService.startParserTopology("group");

    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void startParserTopologyShouldReturnGlobalConfigMissingError() throws Exception {
    when(globalConfigService.get()).thenReturn(null);

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage(TopologyStatusCode.GLOBAL_CONFIG_MISSING.toString());

    assertEquals(expected, stormAdminService.startParserTopology("bro"));
  }

  @Test
  public void startParserTopologyShouldReturnSensorParserConfigMissingError() throws Exception {
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>());
    when(sensorParserConfigService.findOne("bro")).thenReturn(null);

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage(TopologyStatusCode.SENSOR_PARSER_CONFIG_MISSING.toString());

    assertEquals(expected, stormAdminService.startParserTopology("bro"));
  }

  @Test
  public void stopParserTopologyShouldProperlyReturnErrorTopologyResponse() throws Exception {
    TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName("bro");
    when(stormCLIClientWrapper.stopParserTopology("bro", false)).thenReturn(1);
    when(stormStatusService.getTopologyStatus("bro")).thenReturn(topologyStatus);

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage(TopologyStatusCode.STOP_ERROR.toString());

    assertEquals(expected, stormAdminService.stopParserTopology("bro", false));
  }

  @Test
  public void startEnrichmentTopologyShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    when(stormCLIClientWrapper.startEnrichmentTopology()).thenReturn(0);

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STARTED.toString());

    assertEquals(expected, stormAdminService.startEnrichmentTopology());
  }

  @Test
  public void stopEnrichmentTopologyShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    when(stormCLIClientWrapper.stopEnrichmentTopology(false)).thenReturn(0);

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STOPPED.toString());

    assertEquals(expected, stormAdminService.stopEnrichmentTopology(false));
  }

  @Test
  public void startIndexingTopologyShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    when(stormCLIClientWrapper.startIndexingTopology("random_access_indexing_script_path")).thenReturn(0);

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STARTED.toString());

    assertEquals(expected, stormAdminService.startIndexingTopology("random_access_indexing_script_path"));
  }

  @Test
  public void stopIndexingTopologyShouldProperlyReturnSuccessTopologyResponse() throws Exception {
    when(stormCLIClientWrapper.stopIndexingTopology("random_access_indexing", false)).thenReturn(0);

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.STOPPED.toString());

    assertEquals(expected, stormAdminService.stopIndexingTopology("random_access_indexing",false));
  }

  @Test
  public void getStormClientStatusShouldProperlyReturnStatus() throws Exception {
    final Map<String, String> status = new HashMap() {{
      put("status", "statusValue");
    }};
    when(stormCLIClientWrapper.getStormClientStatus()).thenReturn(status);

    assertEquals(new HashMap() {{
      put("status", "statusValue");
    }}, stormAdminService.getStormClientStatus());
  }


}