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
package org.apache.metron.rest.controller;

import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.service.StormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/storm")
public class StormController {

  @Autowired
  private StormService stormService;

  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<List<TopologyStatus>> getAll() throws Exception {
    return new ResponseEntity<>(stormService.getAllTopologyStatus(), HttpStatus.OK);
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> get(@PathVariable String name) throws Exception {
    TopologyStatus sensorParserStatus = stormService.getTopologyStatus(name);
    if (sensorParserStatus != null) {
      return new ResponseEntity<>(sensorParserStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/parser/start/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> start(@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.startParserTopology(name), HttpStatus.OK);
  }

  @RequestMapping(value = "/parser/stop/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stop(@PathVariable String name, @RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopParserTopology(name, stopNow), HttpStatus.OK);
  }

  @RequestMapping(value = "/parser/activate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activate(@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(name), HttpStatus.OK);
  }

  @RequestMapping(value = "/parser/deactivate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivate(@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(name), HttpStatus.OK);
  }

  @RequestMapping(value = "/enrichment", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getEnrichment() throws Exception {
    TopologyStatus sensorParserStatus = stormService.getTopologyStatus(StormService.ENRICHMENT_TOPOLOGY_NAME);
    if (sensorParserStatus != null) {
      return new ResponseEntity<>(sensorParserStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/enrichment/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.startEnrichmentTopology(), HttpStatus.OK);
  }

  @RequestMapping(value = "/enrichment/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopEnrichment(@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopEnrichmentTopology(stopNow), HttpStatus.OK);
  }

  @RequestMapping(value = "/enrichment/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(StormService.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @RequestMapping(value = "/enrichment/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(StormService.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @RequestMapping(value = "/indexing", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getIndexing() throws Exception {
    TopologyStatus sensorParserStatus = stormService.getTopologyStatus(StormService.INDEXING_TOPOLOGY_NAME);
    if (sensorParserStatus != null) {
      return new ResponseEntity<>(sensorParserStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/indexing/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startIndexing() throws Exception {
    return new ResponseEntity<>(stormService.startIndexingTopology(), HttpStatus.OK);
  }

  @RequestMapping(value = "/indexing/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopIndexing(@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopIndexingTopology(stopNow), HttpStatus.OK);
  }

  @RequestMapping(value = "/indexing/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateIndexing() throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(StormService.INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @RequestMapping(value = "/indexing/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateIndexing() throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(StormService.INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @RequestMapping(value = "/client/status", method = RequestMethod.GET)
  ResponseEntity<Map<String, String>> clientStatus() throws Exception {
    return new ResponseEntity<>(stormService.getStormClientStatus(), HttpStatus.OK);
  }

}
