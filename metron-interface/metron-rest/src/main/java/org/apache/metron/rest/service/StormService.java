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

import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.model.TopologySummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StormService {

    public static final String STORM_UI_SPRING_PROPERTY = "storm.ui.url";
    public static final String TOPOLOGY_SUMMARY_URL = "/api/v1/topology/summary";
    public static final String TOPOLOGY_URL = "/api/v1/topology";
    public static final String ENRICHMENT_TOPOLOGY_NAME = "enrichment";
    public static final String INDEXING_TOPOLOGY_NAME = "indexing";

    @Autowired
    private Environment environment;

    private RestTemplate restTemplate;

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private StormCLIWrapper stormCLIClientWrapper;

    @Autowired
    public void setStormCLIClientWrapper(StormCLIWrapper stormCLIClientWrapper) {
        this.stormCLIClientWrapper = stormCLIClientWrapper;
    }

    @Autowired
    private GlobalConfigService globalConfigService;

    @Autowired
    private SensorParserConfigService sensorParserConfigService;

    public TopologySummary getTopologySummary() {
        return restTemplate.getForObject("http://" + environment.getProperty(STORM_UI_SPRING_PROPERTY) + TOPOLOGY_SUMMARY_URL, TopologySummary.class);
    }

    public TopologyStatus getTopologyStatus(String name) {
        TopologyStatus topologyResponse = null;
        String id = null;
        for (TopologyStatus topology : getTopologySummary().getTopologies()) {
            if (name.equals(topology.getName())) {
                id = topology.getId();
                break;
            }
        }
        if (id != null) {
            topologyResponse = restTemplate.getForObject("http://" + environment.getProperty(STORM_UI_SPRING_PROPERTY) + TOPOLOGY_URL + "/" + id, TopologyStatus.class);
        }
        return topologyResponse;
    }

    public List<TopologyStatus> getAllTopologyStatus() {
        List<TopologyStatus> topologyStatus = new ArrayList<>();
        for (TopologyStatus topology : getTopologySummary().getTopologies()) {
            topologyStatus.add(restTemplate.getForObject("http://" + environment.getProperty(STORM_UI_SPRING_PROPERTY) + TOPOLOGY_URL + "/" + topology.getId(), TopologyStatus.class));
        }
        return topologyStatus;
    }

    public TopologyResponse activateTopology(String name) {
        TopologyResponse topologyResponse = new TopologyResponse();
        String id = null;
        for (TopologyStatus topology : getTopologySummary().getTopologies()) {
            if (name.equals(topology.getName())) {
                id = topology.getId();
                break;
            }
        }
        if (id != null) {
            Map result = restTemplate.postForObject("http://" + environment.getProperty(STORM_UI_SPRING_PROPERTY) + TOPOLOGY_URL + "/" + id + "/activate", null, Map.class);
            if("success".equals(result.get("status"))) {
                topologyResponse.setSuccessMessage(TopologyStatusCode.ACTIVE.toString());
            } else {
                topologyResponse.setErrorMessage((String) result.get("status"));
            }
        } else {
            topologyResponse.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
        }
        return topologyResponse;
    }

    public TopologyResponse deactivateTopology(String name) {
        TopologyResponse topologyResponse = new TopologyResponse();
        String id = null;
        for (TopologyStatus topology : getTopologySummary().getTopologies()) {
            if (name.equals(topology.getName())) {
                id = topology.getId();
                break;
            }
        }
        if (id != null) {
            Map result = restTemplate.postForObject("http://" + environment.getProperty(STORM_UI_SPRING_PROPERTY) + TOPOLOGY_URL + "/" + id + "/deactivate", null, Map.class);
            if("success".equals(result.get("status"))) {
                topologyResponse.setSuccessMessage(TopologyStatusCode.INACTIVE.toString());
            } else {
                topologyResponse.setErrorMessage((String) result.get("status"));
            }
        } else {
            topologyResponse.setErrorMessage(TopologyStatusCode.TOPOLOGY_NOT_FOUND.toString());
        }
        return topologyResponse;
    }

    public TopologyResponse startParserTopology(String name) throws Exception {
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

    public TopologyResponse stopParserTopology(String name, boolean stopNow) throws IOException, InterruptedException {
        return createResponse(stormCLIClientWrapper.stopParserTopology(name, stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    public TopologyResponse startEnrichmentTopology() throws IOException, InterruptedException {
        return createResponse(stormCLIClientWrapper.startEnrichmentTopology(), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
    }

    public TopologyResponse stopEnrichmentTopology(boolean stopNow) throws IOException, InterruptedException {
        return createResponse(stormCLIClientWrapper.stopEnrichmentTopology(stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    public TopologyResponse startIndexingTopology() throws IOException, InterruptedException {
        return createResponse(stormCLIClientWrapper.startIndexingTopology(), TopologyStatusCode.STARTED, TopologyStatusCode.START_ERROR);
    }

    public TopologyResponse stopIndexingTopology(boolean stopNow) throws IOException, InterruptedException {
        return createResponse(stormCLIClientWrapper.stopIndexingTopology(stopNow), TopologyStatusCode.STOPPED, TopologyStatusCode.STOP_ERROR);
    }

    protected TopologyResponse createResponse(int responseCode, TopologyStatusCode successMessage, TopologyStatusCode errorMessage) {
        TopologyResponse topologyResponse = new TopologyResponse();
        if (responseCode == 0) {
            topologyResponse.setSuccessMessage(successMessage.toString());
        } else {
            topologyResponse.setErrorMessage(errorMessage.toString());
        }
        return topologyResponse;
    }

    public Map<String, String> getStormClientStatus() throws IOException {
        return stormCLIClientWrapper.getStormClientStatus();
    }

}
