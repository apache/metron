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

import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.parsers.topology.ParserTopologyCLI;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.apache.metron.rest.service.StormAdminService;
import org.apache.metron.rest.service.StormStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class StormAdminServiceImpl implements StormAdminService {

    private StormCLIWrapper stormCLIClientWrapper;

    private GlobalConfigService globalConfigService;

    private SensorParserConfigService sensorParserConfigService;

    private SensorParserGroupService sensorParserGroupService;

    private StormStatusService stormStatusService;

    @Autowired
    public StormAdminServiceImpl(StormCLIWrapper stormCLIClientWrapper,
                                 GlobalConfigService globalConfigService,
                                 SensorParserConfigService sensorParserConfigService,
                                 SensorParserGroupService sensorParserGroupService,
                                 StormStatusService stormStatusService) {
        this.stormCLIClientWrapper = stormCLIClientWrapper;
        this.globalConfigService = globalConfigService;
        this.sensorParserConfigService = sensorParserConfigService;
        this.sensorParserGroupService = sensorParserGroupService;
        this.stormStatusService = stormStatusService;
    }

    /**
     * Starts a parser topology.  The name should either be a sensor name or group name in the case of aggregate parser topologies.
     * @param name SensorParserConfig or SensorParserGroup name
     * @return ToplogyResponse
     * @throws RestException Global Config or SensorParserConfigs not found or starting the topology resulted in an error.
     */
    @Override
    public TopologyResponse startParserTopology(String name) throws RestException {
        TopologyResponse topologyResponse = new TopologyResponse();
        if (globalConfigService.get() == null) {
            topologyResponse.setErrorMessage(TopologyStatusCode.GLOBAL_CONFIG_MISSING.toString());
            return topologyResponse;
        }

        List<String> sensorTypes = Collections.singletonList(name);
        // If name is a group then look up sensors to build the actual topology name
        SensorParserGroup sensorParserGroup = sensorParserGroupService.findOne(name);
        if (sensorParserGroup != null) {
          sensorTypes = new ArrayList<>(sensorParserGroup.getSensors());
        }
        for (String sensorType : sensorTypes) {
            if (sensorParserConfigService.findOne(sensorType.trim()) == null) {
                topologyResponse
                    .setErrorMessage(TopologyStatusCode.SENSOR_PARSER_CONFIG_MISSING.toString());
                return topologyResponse;
            }
        }

        // sort the sensor types so the topology name is consistent
        Collections.sort(sensorTypes);
        return createResponse(
            stormCLIClientWrapper.startParserTopology(String.join(ParserTopologyCLI.TOPOLOGY_OPTION_SEPARATOR, sensorTypes)),
                TopologyStatusCode.STARTED,
                TopologyStatusCode.START_ERROR
        );
    }

    /**
     * Stops a parser topology.  The name should either be a sensor name or group name in the case of aggregate parser topologies.
     * @param name SensorParserConfig or SensorParserGroup name
     * @param stopNow Stop the topology immediately
     * @return ToplogyResponse
     * @throws RestException Stopping the topology resulted in an error.
     */
    @Override
    public TopologyResponse stopParserTopology(String name, boolean stopNow) throws RestException {
        // Supplied name could be a group so get the actual job name from Storm
        TopologyStatus topologyStatus = stormStatusService.getTopologyStatus(name);
        String jobName = topologyStatus != null ? topologyStatus.getName() : name;
        return createResponse(stormCLIClientWrapper.stopParserTopology(jobName, stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    @Override
    public TopologyResponse startEnrichmentTopology() throws RestException {
        return createResponse(stormCLIClientWrapper.startEnrichmentTopology(), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
    }

    @Override
    public TopologyResponse stopEnrichmentTopology(boolean stopNow) throws RestException {
        return createResponse(stormCLIClientWrapper.stopEnrichmentTopology(stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    @Override
    public TopologyResponse startIndexingTopology(String scriptPath) throws RestException {
        return createResponse(stormCLIClientWrapper.startIndexingTopology(scriptPath), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
    }

    @Override
    public TopologyResponse stopIndexingTopology(String name, boolean stopNow) throws RestException {
        return createResponse(stormCLIClientWrapper.stopIndexingTopology(name, stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    private TopologyResponse createResponse(int responseCode, TopologyStatusCode successMessage, TopologyStatusCode errorMessage) {
        TopologyResponse topologyResponse = new TopologyResponse();
        if (responseCode == 0) {
            topologyResponse.setSuccessMessage(successMessage.toString());
        } else {
            topologyResponse.setErrorMessage(errorMessage.toString());
        }
        return topologyResponse;
    }

    @Override
    public Map<String, String> getStormClientStatus() throws RestException {
        return stormCLIClientWrapper.getStormClientStatus();
    }

}
