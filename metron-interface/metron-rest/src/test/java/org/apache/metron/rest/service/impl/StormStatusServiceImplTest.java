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

import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.model.TopologySummary;
import org.apache.metron.rest.service.StormStatusService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.STORM_UI_SPRING_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.TOPOLOGY_SUMMARY_URL;
import static org.apache.metron.rest.MetronRestConstants.TOPOLOGY_URL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
public class StormStatusServiceImplTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  Environment environment;
  RestTemplate restTemplate;
  StormStatusService stormStatusService;

  @Before
  public void setUp() throws Exception {
    environment = mock(Environment.class);
    restTemplate = mock(RestTemplate.class);
    stormStatusService = new StormStatusServiceImpl(environment, restTemplate);
  }

  @Test
  public void getTopologySummaryShouldReturnTopologySummary() throws Exception {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setStatus(TopologyStatusCode.STARTED);
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);

    TopologyStatus expectedStatus = new TopologyStatus();
    expectedStatus.setStatus(TopologyStatusCode.STARTED);
    expectedStatus.setName("bro");
    expectedStatus.setId("bro_id");
    TopologySummary expected = new TopologySummary();
    expected.setTopologies(new TopologyStatus[]{expectedStatus});

    TopologySummary actual = stormStatusService.getTopologySummary();
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void getTopologyStatusShouldReturnTopologyStatus() throws Exception {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setStatus(TopologyStatusCode.STARTED);
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id", TopologyStatus.class)).thenReturn(topologyStatus);

    TopologyStatus expected = new TopologyStatus();
    expected.setStatus(TopologyStatusCode.STARTED);
    expected.setName("bro");
    expected.setId("bro_id");

    TopologyStatus actual = stormStatusService.getTopologyStatus("bro");
    assertEquals(expected, actual);
    assertEquals(expected.hashCode(), actual.hashCode());
  }

  @Test
  public void getAllTopologyStatusShouldReturnAllTopologyStatus() {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setStatus(TopologyStatusCode.STARTED);
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id", TopologyStatus.class)).thenReturn(topologyStatus);

    TopologyStatus expected = new TopologyStatus();
    expected.setStatus(TopologyStatusCode.STARTED);
    expected.setName("bro");
    expected.setId("bro_id");

    assertEquals(new ArrayList() {{ add(expected); }}, stormStatusService.getAllTopologyStatus());
  }


  @Test
  public void activateTopologyShouldReturnActiveTopologyResponse() {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.postForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id/activate", null, Map.class))
            .thenReturn(new HashMap() {{ put("status", "success"); }});

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.ACTIVE.toString());
    assertEquals(expected, stormStatusService.activateTopology("bro"));
  }

  @Test
  public void activateTopologyShouldReturnErrorTopologyResponse() {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.postForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id/activate", null, Map.class))
            .thenReturn(new HashMap() {{ put("status", "error message"); }});

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage("error message");
    assertEquals(expected, stormStatusService.activateTopology("bro"));
  }

  @Test
  public void activateTopologyShouldReturnTopologyNotFoundTopologyResponse() {
    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(new TopologySummary());

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
    assertEquals(expected, stormStatusService.activateTopology("bro"));
  }

  @Test
  public void deactivateTopologyShouldReturnActiveTopologyResponse() {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.postForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id/deactivate", null, Map.class))
            .thenReturn(new HashMap() {{ put("status", "success"); }});

    TopologyResponse expected = new TopologyResponse();
    expected.setSuccessMessage(TopologyStatusCode.INACTIVE.toString());
    assertEquals(expected, stormStatusService.deactivateTopology("bro"));
  }

  @Test
  public void deactivateTopologyShouldReturnErrorTopologyResponse() {
    final TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName("bro");
    topologyStatus.setId("bro_id");
    final TopologySummary topologySummary = new TopologySummary();
    topologySummary.setTopologies(new TopologyStatus[]{topologyStatus});

    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(topologySummary);
    when(restTemplate.postForObject("http://storm_ui" + TOPOLOGY_URL + "/bro_id/deactivate", null, Map.class))
            .thenReturn(new HashMap() {{ put("status", "error message"); }});

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage("error message");
    assertEquals(expected, stormStatusService.deactivateTopology("bro"));
  }

  @Test
  public void deactivateTopologyShouldReturnTopologyNotFoundTopologyResponse() {
    when(environment.getProperty(STORM_UI_SPRING_PROPERTY)).thenReturn("storm_ui");
    when(restTemplate.getForObject("http://storm_ui" + TOPOLOGY_SUMMARY_URL, TopologySummary.class)).thenReturn(new TopologySummary());

    TopologyResponse expected = new TopologyResponse();
    expected.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
    assertEquals(expected, stormStatusService.deactivateTopology("bro"));
  }


}