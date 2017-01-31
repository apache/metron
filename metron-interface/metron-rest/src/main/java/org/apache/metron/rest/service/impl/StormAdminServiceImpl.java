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

import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.service.GlobalConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.StormAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StormAdminServiceImpl implements StormAdminService {

    private StormCLIWrapper stormCLIClientWrapper;

    private GlobalConfigService globalConfigService;

    private SensorParserConfigService sensorParserConfigService;

    @Autowired
    public StormAdminServiceImpl(StormCLIWrapper stormCLIClientWrapper, GlobalConfigService globalConfigService, SensorParserConfigService sensorParserConfigService) {
        this.stormCLIClientWrapper = stormCLIClientWrapper;
        this.globalConfigService = globalConfigService;
        this.sensorParserConfigService = sensorParserConfigService;
    }


    @Override
    public TopologyResponse startParserTopology(String name) throws RestException {
        TopologyResponse topologyResponse = new TopologyResponse();
        if (globalConfigService.get() == null) {
            topologyResponse.setErrorMessage(TopologyStatusCode.GLOBAL_CONFIG_MISSING.toString());
        } else if (sensorParserConfigService.findOne(name) == null) {
            topologyResponse.setErrorMessage(TopologyStatusCode.SENSOR_PARSER_CONFIG_MISSING.toString());
        } else {
            topologyResponse = createResponse(stormCLIClientWrapper.startParserTopology(name), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
        }
        return topologyResponse;
    }

    @Override
    public TopologyResponse stopParserTopology(String name, boolean stopNow) throws RestException {
        return createResponse(stormCLIClientWrapper.stopParserTopology(name, stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
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
    public TopologyResponse startIndexingTopology() throws RestException {
        return createResponse(stormCLIClientWrapper.startIndexingTopology(), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
    }

    @Override
    public TopologyResponse stopIndexingTopology(boolean stopNow) throws RestException {
        return createResponse(stormCLIClientWrapper.stopIndexingTopology(stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
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
