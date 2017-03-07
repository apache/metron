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
import org.apache.metron.common.field.transformation.FieldTransformations;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.StellarFunctionDescription;
import org.apache.metron.rest.model.SensorParserContext;
import org.apache.metron.rest.service.StellarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stellar")
public class StellarController {

    @Autowired
    private StellarService stellarService;

  @ApiOperation(value = "Tests Stellar statements to ensure they are well-formed")
  @ApiResponse(message = "Returns validation results", code = 200)
    @RequestMapping(value = "/validate/rules", method = RequestMethod.POST)
    ResponseEntity<Map<String, Boolean>> validateRules(@ApiParam(name="statements", value="List of statements to validate", required=true)@RequestBody List<String> statements) throws RestException {
        return new ResponseEntity<>(stellarService.validateRules(statements), HttpStatus.OK);
    }

  @ApiOperation(value = "Executes transformations against a sample message")
  @ApiResponse(message = "Returns transformation results", code = 200)
    @RequestMapping(value = "/apply/transformations", method = RequestMethod.POST)
    ResponseEntity<Map<String, Object>> applyTransformations(@ApiParam(name="transformationValidation", value="Object containing SensorParserConfig and sample message", required=true)@RequestBody SensorParserContext sensorParserContext) throws RestException {
        return new ResponseEntity<>(stellarService.applyTransformations(sensorParserContext), HttpStatus.OK);
    }

  @ApiOperation(value = "Retrieves field transformations")
  @ApiResponse(message = "Returns a list field transformations", code = 200)
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<FieldTransformations[]> list() throws RestException {
        return new ResponseEntity<>(stellarService.getTransformations(), HttpStatus.OK);
    }

  @ApiOperation(value = "Lists the Stellar functions that can be found on the classpath")
  @ApiResponse(message = "Returns a list of Stellar functions", code = 200)
    @RequestMapping(value = "/list/functions", method = RequestMethod.GET)
    ResponseEntity<List<StellarFunctionDescription>> listFunctions() throws RestException {
        return new ResponseEntity<>(stellarService.getStellarFunctions(), HttpStatus.OK);
    }

  @ApiOperation(value = "Lists the simple Stellar functions (functions with only 1 input) that can be found on the classpath")
  @ApiResponse(message = "Returns a list of simple Stellar functions", code = 200)
    @RequestMapping(value = "/list/simple/functions", method = RequestMethod.GET)
    ResponseEntity<List<StellarFunctionDescription>> listSimpleFunctions() throws RestException {
        return new ResponseEntity<>(stellarService.getSimpleStellarFunctions(), HttpStatus.OK);
    }
}
