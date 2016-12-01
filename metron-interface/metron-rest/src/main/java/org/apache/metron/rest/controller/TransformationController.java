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

import org.apache.metron.common.field.transformation.FieldTransformations;
import org.apache.metron.rest.model.StellarFunctionDescription;
import org.apache.metron.rest.model.TransformationValidation;
import org.apache.metron.rest.service.TransformationService;
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
@RequestMapping("/api/v1/transformation")
public class TransformationController {

    @Autowired
    private TransformationService transformationService;

    @RequestMapping(value = "/validate/rules", method = RequestMethod.POST)
    ResponseEntity<Map<String, Boolean>> validateRule(@RequestBody List<String> rules) throws Exception {
        return new ResponseEntity<>(transformationService.validateRules(rules), HttpStatus.OK);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    ResponseEntity<Map<String, Object>> validateTransformation(@RequestBody TransformationValidation transformationValidation) throws Exception {
        return new ResponseEntity<>(transformationService.validateTransformation(transformationValidation), HttpStatus.OK);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<FieldTransformations[]> list() throws Exception {
        return new ResponseEntity<>(transformationService.getTransformations(), HttpStatus.OK);
    }

    @RequestMapping(value = "/list/functions", method = RequestMethod.GET)
    ResponseEntity<List<StellarFunctionDescription>> listFunctions() throws Exception {
        return new ResponseEntity<>(transformationService.getStellarFunctions(), HttpStatus.OK);
    }

    @RequestMapping(value = "/list/simple/functions", method = RequestMethod.GET)
    ResponseEntity<List<StellarFunctionDescription>> listSimpleFunctions() throws Exception {
        return new ResponseEntity<>(transformationService.getSimpleStellarFunctions(), HttpStatus.OK);
    }
}
