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
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.configuration.SensorParserGroup;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.apache.metron.rest.service.SensorParserGroupService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sensor/parser/group")
public class SensorParserGroupController {

  @Autowired
  private SensorParserGroupService sensorParserGroupService;

  @ApiOperation(value = "Updates or creates a SensorParserGroup in Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "SensorParserGroup updated. Returns saved SensorParserGroup", code = 200),
          @ApiResponse(message = "SensorParserGroup created. Returns saved SensorParserGroup", code = 201) })
  @RequestMapping(method = RequestMethod.POST)
  ResponseEntity<SensorParserGroup> save(@ApiParam(name="sensorParserGroup", value="SensorParserGroup", required=true)@RequestBody SensorParserGroup sensorParserGroup) throws RestException {
    if (sensorParserGroupService.findOne(sensorParserGroup.getName()) == null) {
      return new ResponseEntity<>(sensorParserGroupService.save(sensorParserGroup), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(sensorParserGroupService.save(sensorParserGroup), HttpStatus.OK);
    }
  }

  @ApiOperation(value = "Retrieves a SensorParserGroup from Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "Returns SensorParserGroup", code = 200),
          @ApiResponse(message = "SensorParserGroup is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<SensorParserGroup> findOne(@ApiParam(name="name", value="SensorParserGroup name", required=true)@PathVariable String name) throws RestException {
    SensorParserGroup sensorParserGroup = sensorParserGroupService.findOne(name);
    if (sensorParserGroup != null) {
      return new ResponseEntity<>(sensorParserGroup, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @ApiOperation(value = "Retrieves all SensorParserGroups from Zookeeper")
  @ApiResponse(message = "Returns all SensorParserGroups", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<Map<String, SensorParserGroup>> findAll() throws RestException {
    return new ResponseEntity<>(sensorParserGroupService.getAll(), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes a SensorParserGroup from Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "SensorParserGroup was deleted", code = 200),
          @ApiResponse(message = "SensorParserGroup is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(@ApiParam(name="name", value="SensorParserGroup name", required=true)@PathVariable String name) throws RestException {
    if (sensorParserGroupService.delete(name)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
