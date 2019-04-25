/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.rest.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.parsers.topology.ParserTopologyCLI;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.SupervisorSummary;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.model.TopologySummary;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.apache.metron.rest.service.StormStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static org.apache.metron.rest.MetronRestConstants.STORM_UI_SPRING_PROPERTY;
import static org.apache.metron.rest.MetronRestConstants.SUPERVISOR_SUMMARY_URL;
import static org.apache.metron.rest.MetronRestConstants.TOPOLOGY_SUMMARY_URL;
import static org.apache.metron.rest.MetronRestConstants.TOPOLOGY_URL;

@Service
public class StormStatusServiceImpl implements StormStatusService {

  private Environment environment;
  private RestTemplate restTemplate;
  private SensorParserGroupService sensorParserGroupService;

  @Autowired
  public StormStatusServiceImpl(Environment environment, RestTemplate restTemplate, SensorParserGroupService sensorParserGroupService) {
    this.environment = environment;
    this.restTemplate = restTemplate;
    this.sensorParserGroupService = sensorParserGroupService;
  }

  @Override
  public SupervisorSummary getSupervisorSummary() {
    return restTemplate
        .getForObject(getStormUiProperty() + SUPERVISOR_SUMMARY_URL, SupervisorSummary.class);
  }

  @Override
  public TopologySummary getTopologySummary() {
    return restTemplate
        .getForObject(getStormUiProperty() + TOPOLOGY_SUMMARY_URL, TopologySummary.class);
  }

  @Override
  public TopologyStatus getTopologyStatus(String name) {
    TopologyStatus topologyResponse = null;
    String id = getTopologyId(name);
    if (id != null) {
      topologyResponse = restTemplate
          .getForObject(getStormUiProperty() + TOPOLOGY_URL + "/" + id, TopologyStatus.class);
    }
    return topologyResponse;
  }

  @Override
  public List<TopologyStatus> getAllTopologyStatus() {
    List<TopologyStatus> topologyStatus = new ArrayList<>();
    for (TopologyStatus topology : getTopologySummary().getTopologies()) {
      topologyStatus.add(restTemplate
          .getForObject(getStormUiProperty() + TOPOLOGY_URL + "/" + topology.getId(),
              TopologyStatus.class));
    }
    return topologyStatus;
  }

  @Override
  public TopologyResponse activateTopology(String name) {
    TopologyResponse topologyResponse = new TopologyResponse();
    String id = getTopologyId(name);
    if (id != null) {
      Map result = restTemplate
          .postForObject(getStormUiProperty() + TOPOLOGY_URL + "/" + id + "/activate", null,
              Map.class);
      if ("success".equals(result.get("status"))) {
        topologyResponse.setSuccessMessage(TopologyStatusCode.ACTIVE.toString());
      } else {
        topologyResponse.setErrorMessage((String) result.get("status"));
      }
    } else {
      topologyResponse.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
    }
    return topologyResponse;
  }

  @Override
  public TopologyResponse deactivateTopology(String name) {
    TopologyResponse topologyResponse = new TopologyResponse();
    String id = getTopologyId(name);
    if (id != null) {
      Map result = restTemplate
          .postForObject(getStormUiProperty() + TOPOLOGY_URL + "/" + id + "/deactivate", null,
              Map.class);
      if ("success".equals(result.get("status"))) {
        topologyResponse.setSuccessMessage(TopologyStatusCode.INACTIVE.toString());
      } else {
        topologyResponse.setErrorMessage((String) result.get("status"));
      }
    } else {
      topologyResponse.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
    }
    return topologyResponse;
  }

  // If we don't have a protocol, choose http
  protected String getStormUiProperty() {
    String baseValue = environment.getProperty(STORM_UI_SPRING_PROPERTY);
    if (!(baseValue.contains("://"))) {
      return "http://" + baseValue;
    }
    return baseValue;
  }

  /**
   * Retrieves the Storm topology id from the given topology name.  If a topology name is detected to be an aggregate
   * parser topology, the SensorParserGroups are checked for a match.
   * @param name Topology or SensorParserGroup name
   * @return Topology id
   */
  protected String getTopologyId(String name) {
    String id = null;
    for (TopologyStatus topology : getTopologySummary().getTopologies()) {
      String topologyName = topology.getName();

      // check sensor group
      if (topologyName.contains(ParserTopologyCLI.STORM_JOB_SEPARATOR)) {
        Set<String> sensors = new HashSet<>(Arrays.asList(topologyName.split(ParserTopologyCLI.STORM_JOB_SEPARATOR)));
        SensorParserGroup group = sensorParserGroupService.findOne(name);
        if (group == null) {
          break;
        } else if (sensors.equals(group.getSensors())){
          id = topology.getId();
          break;
        }
      }

      if (topologyName.equals(name)) {
        id = topology.getId();
        break;
      }
    }
    return id;
  }
}
