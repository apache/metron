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
import org.apache.metron.rest.service.GlobalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/global/config")
public class GlobalConfigController {

    @Autowired
    private GlobalConfigService globalConfigService;

    @ApiOperation(value = "Creates or updates the Global Config in Zookeeper")
    @ApiResponses(value = { @ApiResponse(message = "Global Config updated. Returns saved Global Config JSON", code = 200),
            @ApiResponse(message = "Global Config created. Returns saved Global Config JSON", code = 201) })
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Map<String, Object>> save(@ApiParam(name="globalConfig", value="The Global Config JSON to be saved", required=true)@RequestBody Map<String, Object> globalConfig) throws RestException {
        if (globalConfigService.get() == null) {
            return new ResponseEntity<>(globalConfigService.save(globalConfig), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(globalConfigService.save(globalConfig), HttpStatus.OK);
        }
    }

    @ApiOperation(value = "Retrieves the current Global Config from Zookeeper")
    @ApiResponses(value = { @ApiResponse(message = "Returns current Global Config JSON in Zookeeper", code = 200),
            @ApiResponse(message = "Global Config JSON was not found in Zookeeper", code = 404) })
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<Map<String, Object>> get() throws RestException {
        Map<String, Object> globalConfig = globalConfigService.get();
        if (globalConfig != null) {
            return new ResponseEntity<>(globalConfig, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation(value = "Deletes the current Global Config from Zookeeper")
    @ApiResponses(value = { @ApiResponse(message = "Global Config JSON was deleted", code = 200),
            @ApiResponse(message = "Global Config JSON was not found in Zookeeper", code = 404) })
    @RequestMapping(method = RequestMethod.DELETE)
    ResponseEntity<Void> delete() throws RestException {
        if (globalConfigService.delete()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
