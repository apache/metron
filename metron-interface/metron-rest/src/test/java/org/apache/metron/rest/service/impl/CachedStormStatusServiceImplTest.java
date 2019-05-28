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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.metron.rest.model.SupervisorStatus;
import org.apache.metron.rest.model.SupervisorSummary;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologySummary;
import org.apache.metron.rest.service.StormStatusService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CachedStormStatusServiceImplTest {

  private SupervisorStatus supervisorStatus1;
  private SupervisorStatus supervisorStatus2;
  private SupervisorSummary supervisorSummary;
  private TopologyStatus topologyStatus1;
  private TopologyStatus topologyStatus2;
  private TopologySummary topologySummary;
  @Mock
  private StormStatusService stormService;
  private CachedStormStatusServiceImpl cachedStormStatusService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    supervisorStatus1 = new SupervisorStatus();
    supervisorStatus2 = new SupervisorStatus();
    supervisorSummary = new SupervisorSummary(
        new SupervisorStatus[]{supervisorStatus1, supervisorStatus2});
    topologyStatus1 = new TopologyStatus();
    topologyStatus2 = new TopologyStatus();
    topologySummary = new TopologySummary(new TopologyStatus[]{topologyStatus1, topologyStatus2});
    when(stormService.getSupervisorSummary()).thenReturn(supervisorSummary);
    when(stormService.getTopologySummary()).thenReturn(topologySummary);
    cachedStormStatusService = new CachedStormStatusServiceImpl(stormService);
  }

  @Test
  public void caches_supervisor_summary() {
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
  public void caches_topology_status() {
    fail("not implemented");
  }

  @Test
  public void caches_all_topology_status() {
    fail("not implemented");
  }
}
