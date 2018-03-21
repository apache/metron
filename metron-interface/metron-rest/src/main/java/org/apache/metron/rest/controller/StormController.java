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
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.SupervisorSummary;
import org.apache.metron.rest.model.TopologyResponse;
import org.apache.metron.rest.model.TopologyStatus;
import org.apache.metron.rest.service.StormAdminService;
import org.apache.metron.rest.service.StormStatusService;
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
  private StormStatusService stormStatusService;

  @Autowired
  private StormAdminService stormAdminService;

  @ApiOperation(value = "Retrieves the status of all Storm Supervisors")
  @ApiResponse(message = "Returns a list of supervisors with status information", code = 200)
  @RequestMapping(value = "/supervisors", method = RequestMethod.GET)
  ResponseEntity<SupervisorSummary> getSupervisorSummary() throws RestException{
    return new ResponseEntity<>(stormStatusService.getSupervisorSummary(),HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of all Storm topologies")
  @ApiResponse(message = "Returns a list of topologies with status information", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<List<TopologyStatus>> getAll() throws RestException {
    return new ResponseEntity<>(stormStatusService.getAllTopologyStatus(), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of a Storm topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> get(@ApiParam(name="name", value="Topology name", required=true)@PathVariable String name) throws RestException {
    TopologyStatus topologyStatus = stormStatusService.getTopologyStatus(name);
    if (topologyStatus != null) {
      return new ResponseEntity<>(topologyStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm parser topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/parser/start/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> start(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws RestException {
    return new ResponseEntity<>(stormAdminService.startParserTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm parser topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/parser/stop/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stop(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name,
                                        @ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws RestException {
    return new ResponseEntity<>(stormAdminService.stopParserTopology(name, stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm parser topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/parser/activate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activate(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws RestException {
    return new ResponseEntity<>(stormStatusService.activateTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm parser topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/parser/deactivate/{name}", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivate(@ApiParam(name="name", value="Parser name", required=true)@PathVariable String name) throws RestException {
    return new ResponseEntity<>(stormStatusService.deactivateTopology(name), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of the Storm enrichment topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/enrichment", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getEnrichment() throws RestException {
    TopologyStatus sensorParserStatus = stormStatusService.getTopologyStatus(MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME);
    if (sensorParserStatus != null) {
      return new ResponseEntity<>(sensorParserStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm enrichment topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/enrichment/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startEnrichment() throws RestException {
    return new ResponseEntity<>(stormAdminService.startEnrichmentTopology(), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm enrichment topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/enrichment/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopEnrichment(@ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws RestException {
    return new ResponseEntity<>(stormAdminService.stopEnrichmentTopology(stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm enrichment topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/enrichment/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateEnrichment() throws RestException {
    return new ResponseEntity<>(stormStatusService.activateTopology(MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm enrichment topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/enrichment/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateEnrichment() throws RestException {
    return new ResponseEntity<>(stormStatusService.deactivateTopology(MetronRestConstants.ENRICHMENT_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of the Storm random access indexing topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/indexing/randomaccess", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getRandomAccessIndexing() throws RestException {
    TopologyStatus topologyStatus = stormStatusService.getTopologyStatus(MetronRestConstants.RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME);
    if (topologyStatus != null) {
      return new ResponseEntity<>(topologyStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm random access indexing topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/indexing/randomaccess/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startRandomAccessIndexing() throws RestException {
    return new ResponseEntity<>(stormAdminService.startIndexingTopology(MetronRestConstants.RANDOM_ACCESS_INDEXING_SCRIPT_PATH_SPRING_PROPERTY), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm random access indexing topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/indexing/randomaccess/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopRandomAccessIndexing(@ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws RestException {
    return new ResponseEntity<>(stormAdminService.stopIndexingTopology(MetronRestConstants.RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME ,stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm random access indexing topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/indexing/randomaccess/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateRandomAccessIndexing() throws RestException {
    return new ResponseEntity<>(stormStatusService.activateTopology(MetronRestConstants.RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm random access indexing topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/indexing/randomaccess/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateRandomAccessIndexing() throws RestException {
    return new ResponseEntity<>(stormStatusService.deactivateTopology(MetronRestConstants.RANDOM_ACCESS_INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves information about the Storm command line client")
  @ApiResponse(message = "Returns storm command line client information", code = 200)
  @RequestMapping(value = "/client/status", method = RequestMethod.GET)
  ResponseEntity<Map<String, String>> clientStatus() throws RestException {
    return new ResponseEntity<>(stormAdminService.getStormClientStatus(), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the status of the Storm batch indexing topology")
  @ApiResponses(value = { @ApiResponse(message = "Returns topology status information", code = 200),
          @ApiResponse(message = "Topology is missing", code = 404) })
  @RequestMapping(value = "/indexing/batch", method = RequestMethod.GET)
  ResponseEntity<TopologyStatus> getBatchIndexing() throws RestException {
    TopologyStatus topologyStatus = stormStatusService.getTopologyStatus(MetronRestConstants.BATCH_INDEXING_TOPOLOGY_NAME);
    if (topologyStatus != null) {
      return new ResponseEntity<>(topologyStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Starts a Storm batch indexing topology")
  @ApiResponse(message = "Returns start response message", code = 200)
  @RequestMapping(value = "/indexing/batch/start", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> startBatchIndexing() throws RestException {
    return new ResponseEntity<>(stormAdminService.startIndexingTopology(MetronRestConstants.BATCH_INDEXING_SCRIPT_PATH_SPRING_PROPERTY), HttpStatus.OK);
  }

  @ApiOperation(value = "Stops a Storm batch indexing topology")
  @ApiResponse(message = "Returns stop response message", code = 200)
  @RequestMapping(value = "/indexing/batch/stop", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> stopBatchIndexing(@ApiParam(name="stopNow", value="Stop the topology immediately")@RequestParam(required = false, defaultValue = "false") boolean stopNow) throws RestException {
    return new ResponseEntity<>(stormAdminService.stopIndexingTopology(MetronRestConstants.BATCH_INDEXING_TOPOLOGY_NAME, stopNow), HttpStatus.OK);
  }

  @ApiOperation(value = "Activates a Storm batch indexing topology")
  @ApiResponse(message = "Returns activate response message", code = 200)
  @RequestMapping(value = "/indexing/batch/activate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> activateBatchIndexing() throws RestException {
    return new ResponseEntity<>(stormStatusService.activateTopology(MetronRestConstants.BATCH_INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

  @ApiOperation(value = "Deactivates a Storm batch indexing topology")
  @ApiResponse(message = "Returns deactivate response message", code = 200)
  @RequestMapping(value = "/indexing/batch/deactivate", method = RequestMethod.GET)
  ResponseEntity<TopologyResponse> deactivateBatchIndexing() throws RestException {
    return new ResponseEntity<>(stormStatusService.deactivateTopology(MetronRestConstants.BATCH_INDEXING_TOPOLOGY_NAME), HttpStatus.OK);
  }

}
