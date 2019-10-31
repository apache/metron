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

import com.google.common.collect.ImmutableList;
import org.apache.metron.rest.model.*;
import org.apache.metron.rest.service.StormStatusService;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CachedStormStatusServiceImplTest {

  @Mock
  private StormStatusService stormService;
  private CachedStormStatusServiceImpl cachedStormStatusService;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    cachedStormStatusService = new CachedStormStatusServiceImpl(stormService, 150, 30);
  }

  @Test
  public void caches_supervisor_summary() {
    SupervisorStatus supervisorStatus1 = new SupervisorStatus();
    SupervisorStatus supervisorStatus2 = new SupervisorStatus();
    SupervisorSummary supervisorSummary = new SupervisorSummary(
        new SupervisorStatus[]{supervisorStatus1, supervisorStatus2});
    when(stormService.getSupervisorSummary()).thenReturn(supervisorSummary);
    // should hit the cache
    for (int i = 0; i < 100; i++) {
      cachedStormStatusService.getSupervisorSummary();
    }
    SupervisorSummary summary = cachedStormStatusService.getSupervisorSummary();
    assertThat("Number of supervisors did not match.", summary.getSupervisors().length,
        CoreMatchers.equalTo(2));
    verify(stormService, times(1)).getSupervisorSummary();
    cachedStormStatusService.reset();
    summary = cachedStormStatusService.getSupervisorSummary();
    assertThat("Number of supervisors did not match.", summary.getSupervisors().length,
        CoreMatchers.equalTo(2));
    verify(stormService, times(2)).getSupervisorSummary();
  }

  @Test
  public void caches_topology_summary() {
    TopologyStatus topologyStatus1 = new TopologyStatus();
    TopologyStatus topologyStatus2 = new TopologyStatus();
    TopologySummary topologySummary = new TopologySummary(
        new TopologyStatus[]{topologyStatus1, topologyStatus2});
    when(stormService.getTopologySummary()).thenReturn(topologySummary);
    // should hit the cache
    for (int i = 0; i < 100; i++) {
      cachedStormStatusService.getTopologySummary();
    }
    TopologySummary summary = cachedStormStatusService.getTopologySummary();
    assertThat("Number of topologies did not match.", summary.getTopologies().length,
        CoreMatchers.equalTo(2));
    verify(stormService, times(1)).getTopologySummary();
    cachedStormStatusService.reset();
    summary = cachedStormStatusService.getTopologySummary();
    assertThat("Number of topologies did not match.", summary.getTopologies().length,
        CoreMatchers.equalTo(2));
    verify(stormService, times(2)).getTopologySummary();
  }

  @Test
  public void caches_topology_status_by_name() {
    String topologyName1 = "topology-1";
    String topologyName2 = "topology-2";
    TopologyStatus topologyStatus1 = new TopologyStatus();
    topologyStatus1.setName(topologyName1);
    TopologyStatus topologyStatus2 = new TopologyStatus();
    topologyStatus2.setName(topologyName2);
    when(stormService.getTopologyStatus(topologyName1)).thenReturn(topologyStatus1);
    when(stormService.getTopologyStatus(topologyName2)).thenReturn(topologyStatus2);
    // should hit the cache
    for (int i = 0; i < 100; i++) {
      cachedStormStatusService.getTopologyStatus(topologyName1);
      cachedStormStatusService.getTopologyStatus(topologyName2);
    }
    TopologyStatus status1 = cachedStormStatusService.getTopologyStatus(topologyName1);
    TopologyStatus status2 = cachedStormStatusService.getTopologyStatus(topologyName2);
    assertThat("Name did not match for topology 1.", status1.getName(),
        CoreMatchers.equalTo(topologyName1));
    assertThat("Name did not match for topology 2.", status2.getName(),
        CoreMatchers.equalTo(topologyName2));
    verify(stormService, times(1)).getTopologyStatus(topologyName1);
    verify(stormService, times(1)).getTopologyStatus(topologyName2);
    cachedStormStatusService.reset();
    cachedStormStatusService.getTopologyStatus(topologyName1);
    cachedStormStatusService.getTopologyStatus(topologyName2);
    verify(stormService, times(2)).getTopologyStatus(topologyName1);
    verify(stormService, times(2)).getTopologyStatus(topologyName2);
  }

  @Test
  public void caches_all_topology_status() {
    TopologyStatus topologyStatus1 = new TopologyStatus();
    TopologyStatus topologyStatus2 = new TopologyStatus();
    List<TopologyStatus> allTopologyStatus = ImmutableList.of(topologyStatus1, topologyStatus2);
    when(stormService.getAllTopologyStatus()).thenReturn(allTopologyStatus);
    // should hit the cache
    for (int i = 0; i < 100; i++) {
      cachedStormStatusService.getAllTopologyStatus();
    }
    List<TopologyStatus> allStatus = cachedStormStatusService.getAllTopologyStatus();
    assertThat("Number of topologies returned by all topology status check did not match.",
        allStatus.size(), CoreMatchers.equalTo(2));
    verify(stormService, times(1)).getAllTopologyStatus();
    cachedStormStatusService.reset();
    cachedStormStatusService.getAllTopologyStatus();
    verify(stormService, times(2)).getAllTopologyStatus();
  }

  @Test
  public void admin_functions_act_as_simple_passthroughs_to_storm_service() {
    TopologyResponse topologyResponse = new TopologyResponse();
    when(stormService.activateTopology(anyString())).thenReturn(topologyResponse);
    when(stormService.deactivateTopology(anyString())).thenReturn(topologyResponse);
    for (int i = 0; i < 100; i++) {
      cachedStormStatusService.activateTopology("foo");
      cachedStormStatusService.deactivateTopology("foo");
    }
    verify(stormService, times(100)).activateTopology(anyString());
    verify(stormService, times(100)).deactivateTopology(anyString());
  }
}
