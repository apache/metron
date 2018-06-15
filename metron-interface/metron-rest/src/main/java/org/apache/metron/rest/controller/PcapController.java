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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.metron.rest.model.pcap.FixedPcapRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.PcapsResponse;
import org.apache.metron.rest.service.PcapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/pcap")
public class PcapController {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PcapService pcapQueryService;

  @RequestMapping(value = "/fixed", method = RequestMethod.POST)
  ResponseEntity<PcapsResponse> fixed(@RequestBody FixedPcapRequest fixedPcapRequest) throws RestException, IOException {
    PcapsResponse pcapsResponse = pcapQueryService.fixed(fixedPcapRequest);
    if (pcapsResponse != null) {
      return new ResponseEntity<>(pcapsResponse, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
