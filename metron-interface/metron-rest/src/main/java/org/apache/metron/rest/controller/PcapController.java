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
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.model.pcap.PcapStatus;
import org.apache.metron.rest.security.SecurityUtils;
import org.apache.metron.rest.service.PcapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pcap")
public class PcapController {

  @Autowired
  private PcapService pcapQueryService;

  @ApiOperation(value = "Executes a Fixed Pcap Query.")
  @ApiResponses(value = { @ApiResponse(message = "Returns a job status with job ID.", code = 200)})
  @RequestMapping(value = "/fixed", method = RequestMethod.POST)
  ResponseEntity<PcapStatus> fixed(@ApiParam(name="fixedPcapRequest", value="A Fixed Pcap Request"
          + " which includes fixed filter fields like ip source address and protocol.", required=true)@RequestBody FixedPcapRequest fixedPcapRequest) throws RestException {
    PcapStatus pcapStatus = pcapQueryService.fixed(SecurityUtils.getCurrentUser(), fixedPcapRequest);
    return new ResponseEntity<>(pcapStatus, HttpStatus.OK);
  }

  @ApiOperation(value = "Gets job status for running job.")
  @ApiResponses(value = { @ApiResponse(message = "Returns a job status for the passed job.", code = 200)})
  @RequestMapping(value = "/{jobId}", method = RequestMethod.GET)
  ResponseEntity<PcapStatus> getStatus(@ApiParam(name="jobId", value="Job ID of submitted job"
      + " which includes fixed filter fields like ip source address and protocol.", required=true)@PathVariable String jobId) throws RestException {
    PcapStatus jobStatus = pcapQueryService.getJobStatus(SecurityUtils.getCurrentUser(), jobId);
    if (jobStatus != null) {
      return new ResponseEntity<>(jobStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Kills running job.")
  @ApiResponses(value = { @ApiResponse(message = "Kills passed job.", code = 200)})
  @RequestMapping(value = "/kill/{jobId}", method = RequestMethod.DELETE)
  ResponseEntity<PcapStatus> killJob(
      @ApiParam(name = "jobId", value = "Job ID of submitted job", required = true) @PathVariable String jobId)
      throws RestException {
    PcapStatus jobStatus = pcapQueryService.killJob(SecurityUtils.getCurrentUser(), jobId);
    if (jobStatus != null) {
      return new ResponseEntity<>(jobStatus, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

}
