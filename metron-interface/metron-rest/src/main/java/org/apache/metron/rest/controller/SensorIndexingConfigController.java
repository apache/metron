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
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sensor/indexing/config")
public class SensorIndexingConfigController {

  @Autowired
  private SensorIndexingConfigService sensorIndexingConfigService;

  @ApiOperation(value = "Updates or creates a SensorIndexingConfig in Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "SensorIndexingConfig updated. Returns saved SensorIndexingConfig", code = 200),
          @ApiResponse(message = "SensorIndexingConfig created. Returns saved SensorIndexingConfig", code = 201) })
  @RequestMapping(value = "/{name}", method = RequestMethod.POST)
  ResponseEntity<Map<String, Object>> save(@ApiParam(name="name", value="SensorIndexingConfig name", required=true)@PathVariable String name,
                                           @ApiParam(name="sensorIndexingConfig", value="SensorIndexingConfig", required=true)@RequestBody Map<String, Object> sensorIndexingConfig) throws RestException {
    if (sensorIndexingConfigService.findOne(name) == null) {
      return new ResponseEntity<>(sensorIndexingConfigService.save(name, sensorIndexingConfig), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(sensorIndexingConfigService.save(name, sensorIndexingConfig), HttpStatus.OK);
    }
  }

  @ApiOperation(value = "Retrieves a SensorIndexingConfig from Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "Returns SensorIndexingConfig", code = 200),
          @ApiResponse(message = "SensorIndexingConfig is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<Map<String, Object>> findOne(@ApiParam(name="name", value="SensorIndexingConfig name", required=true)@PathVariable String name) throws RestException {
    Map<String, Object> sensorIndexingConfig = sensorIndexingConfigService.findOne(name);
    if (sensorIndexingConfig != null) {
      return new ResponseEntity<>(sensorIndexingConfig, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @ApiOperation(value = "Retrieves all SensorIndexingConfigs from Zookeeper")
  @ApiResponse(message = "Returns all SensorIndexingConfigs", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<Map<String, Map<String, Object>>> getAll() throws Exception {
    return new ResponseEntity<>(sensorIndexingConfigService.getAll(), HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves all indices")
  @ApiResponse(message = "Returns all the indices in use", code = 200)
  @RequestMapping(value = "/list/indices/{writerName}", method = RequestMethod.GET)
  ResponseEntity<Iterable<String>> getAllIndices(@ApiParam(name="writerName", value="Writer name.  One of solr, elasticsearch or hdfs", required=true)@PathVariable String writerName) throws Exception {
    return new ResponseEntity<>(sensorIndexingConfigService.getAllIndices(writerName), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes a SensorIndexingConfig from Zookeeper")
  @ApiResponses(value = { @ApiResponse(message = "SensorIndexingConfig was deleted", code = 200),
          @ApiResponse(message = "SensorIndexingConfig is missing", code = 404) })
  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(@ApiParam(name="name", value="SensorIndexingConfig name", required=true)@PathVariable String name) throws RestException {
    if (sensorIndexingConfigService.delete(name)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
