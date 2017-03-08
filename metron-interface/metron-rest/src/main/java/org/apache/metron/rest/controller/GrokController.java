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
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/grok")
public class GrokController {

    @Autowired
    private GrokService grokService;

    @ApiOperation(value = "Applies a Grok statement to a sample message")
    @ApiResponse(message = "JSON results", code = 200)
    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    ResponseEntity<GrokValidation> post(@ApiParam(name = "grokValidation", value = "Object containing Grok statement and sample message", required = true) @RequestBody GrokValidation grokValidation) throws RestException {
        return new ResponseEntity<>(grokService.validateGrokStatement(grokValidation), HttpStatus.OK);
    }

    @ApiOperation(value = "Lists the common Grok statements available in Metron")
    @ApiResponse(message = "JSON object containing pattern label/Grok statements key value pairs", code = 200)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<Map<String, String>> list() throws RestException {
        return new ResponseEntity<>(grokService.getCommonGrokPatterns(), HttpStatus.OK);
    }

    @ApiOperation(value = "Retrieves a Grok statement from the classpath")
    @ApiResponse(message = "Grok statement", code = 200)
    @RequestMapping(value = "/get/statement", method = RequestMethod.GET)
    ResponseEntity<String> get(@ApiParam(name = "path", value = "Path to classpath resource", required = true) @RequestParam String path) throws RestException {
      return new ResponseEntity<>(grokService.getStatementFromClasspath(path), HttpStatus.OK);
    }
}
