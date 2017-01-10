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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

  @ApiOperation(value = "Retrieves the status of all Storm topologies")
  @ApiResponse(message = "Returns a list of topologies with status information", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<List<TopologyStatus>> getAll() throws Exception {
    return new ResponseEntity<>(stormService.getAllTopologyStatus(), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of a Storm topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> get(@ApiParam(name="name", value="Topology name", required=true)@PathVariable String name) throws Exception {
    TopologyStatus topologyStatus = stormService.getTopologyStatus(name);
    if (topologyStatus != null) {
      return new ResponseEntity<>(topologyStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm parser topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/parser/start/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> start(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.startParserTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm parser topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/parser/stop/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stop(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name,
                                        @ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopParserTopology(name, stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm parser topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/parser/activate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activate(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm parser topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/parser/deactivate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivate(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of the Storm enrichment topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/enrichment", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getEnrichment() throws Exception {
    TopologyStatus sensorParserStatus = stormService.getTopologyStatus(StormService.ENRICHMENT_TOPOLOGY_NAME);
    if (sensorParserStatus != null) {
      return new ResponseEntity<>(sensorParserStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm enrichment topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/enrichment/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.startEnrichmentTopology(), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm enrichment topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/enrichment/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopEnrichment(@ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopEnrichmentTopology(stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm enrichment topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/enrichment/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(StormService.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm enrichment topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/enrichment/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateEnrichment() throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(StormService.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of the Storm indexing topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/indexing", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getIndexing() throws Exception {
    TopologyStatus topologyStatus = stormService.getTopologyStatus(StormService.INDEXING_TOPOLOGY_NAME);
    if (topologyStatus != null) {
      return new ResponseEntity<>(topologyStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm indexing topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/indexing/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startIndexing() throws Exception {
    return new ResponseEntity<>(stormService.startIndexingTopology(), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm enrichment topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/indexing/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopIndexing(@ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws Exception {
    return new ResponseEntity<>(stormService.stopIndexingTopology(stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm indexing topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/indexing/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateIndexing() throws Exception {
    return new ResponseEntity<>(stormService.activateTopology(StormService.INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm indexing topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/indexing/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateIndexing() throws Exception {
    return new ResponseEntity<>(stormService.deactivateTopology(StormService.INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves information about the Storm command line client")
  @ApiResponse(message = "Returns storm command line client information", code = 200)
  @RequestMapping(value = "/client/status", method = RequestMethod.GET)
  ResponseEntity<Map<String, String>> clientStatus() throws Exception {
    return new ResponseEntity<>(stormService.getStormClientStatus(), HttpStatus.OK);
  }

}
