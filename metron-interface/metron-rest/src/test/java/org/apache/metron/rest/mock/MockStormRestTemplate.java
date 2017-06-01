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
package org.apache.metron.rest.mock;

import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.model.TopologyStatusCode;
import org.apache.metron.rest.model.TopologySummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockStormRestTemplate extends RestTemplate {

  @Autowired
  private Environment environment;

  private MockStormCLIClientWrapper mockStormCLIClientWrapper;

  public void setMockStormCLIClientWrapper(MockStormCLIClientWrapper mockStormCLIClientWrapper) {
    this.mockStormCLIClientWrapper = mockStormCLIClientWrapper;
  }

  @Override
  public Object getForObject(String url, Class responseType, Object... urlVariables) throws RestClientException {
    Object response = null;
    if (url.equals(getStormUiProperty() + MetronRestConstants.TOPOLOGY_SUMMARY_URL)) {
      TopologySummary topologySummary = new TopologySummary();
      List<TopologyStatus> topologyStatusList = new ArrayList<>();
      for(String name: mockStormCLIClientWrapper.getParserTopologyNames()) {
        topologyStatusList.add(getTopologyStatus(name));
      }
      TopologyStatusCode enrichmentStatus = mockStormCLIClientWrapper.getEnrichmentStatus();
      if (enrichmentStatus != TopologyStatusCode.TOPOLOGY_NOT_FOUND) {
        topologyStatusList.add(getTopologyStatus("enrichment"));
      }
      TopologyStatusCode indexingStatus = mockStormCLIClientWrapper.getIndexingStatus();
      if (indexingStatus != TopologyStatusCode.TOPOLOGY_NOT_FOUND) {
        topologyStatusList.add(getTopologyStatus("indexing"));
      }
      topologySummary.setTopologies(topologyStatusList.toArray(new TopologyStatus[topologyStatusList.size()]));
      response =  topologySummary;
    } else if (url.startsWith(getStormUiProperty() + MetronRestConstants.TOPOLOGY_URL + "/")){
      String name = url.substring(url.lastIndexOf('/') + 1, url.length()).replaceFirst("-id", "");
      response = getTopologyStatus(name);
    }
    return response;
  }

  private TopologyStatus getTopologyStatus(String name) {
    TopologyStatus topologyStatus = new TopologyStatus();
    topologyStatus.setName(name);
    topologyStatus.setId(name + "-id");
    if ("enrichment".equals(name)) {
      topologyStatus.setStatus(mockStormCLIClientWrapper.getEnrichmentStatus());
    } else if ("indexing".equals(name)) {
      topologyStatus.setStatus(mockStormCLIClientWrapper.getIndexingStatus());
    } else {
      topologyStatus.setStatus(mockStormCLIClientWrapper.getParserStatus(name));
    }
    return topologyStatus;
  }

  @Override
  public Object postForObject(String url, Object request, Class responseType, Object... uriVariables) throws RestClientException {
    Map<String, String> result = new HashMap<>();
    String[] urlParts = url.split("/");
    String name = urlParts[urlParts.length - 2].replaceFirst("-id", "");
    String action = urlParts[urlParts.length - 1];
    int returnCode = 0;
    if (action.equals("activate")) {
      if (name.equals("enrichment")) {
        returnCode = mockStormCLIClientWrapper.activateEnrichmentTopology();
      } else if (name.equals("indexing")) {
        returnCode = mockStormCLIClientWrapper.activateIndexingTopology();
      } else {
        returnCode = mockStormCLIClientWrapper.activateParserTopology(name);
      }
    } else if (action.equals("deactivate")){
      if (name.equals("enrichment")) {
        returnCode = mockStormCLIClientWrapper.deactivateEnrichmentTopology();
      } else if (name.equals("indexing")) {
        returnCode = mockStormCLIClientWrapper.deactivateIndexingTopology();
      } else {
        returnCode = mockStormCLIClientWrapper.deactivateParserTopology(name);
      }
    }
    if (returnCode == 0) {
      result.put("status", "success");
    } else {
      result.put("status", "error");
    }
    return result;
  }

  // If we don't have a protocol, prepend one
  protected String getStormUiProperty() {
    String baseValue = environment.getProperty(MetronRestConstants.STORM_UI_SPRING_PROPERTY);
    if(!(baseValue.contains("://"))) {
      return "http://" + baseValue;
    }
    return baseValue;
  }
}
