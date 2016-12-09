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

import org.apache.metron.rest.model.SensorParserConfigHistory;
import org.apache.metron.rest.service.SensorParserConfigHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensorParserConfigHistory")
public class SensorParserConfigHistoryController {

    @Autowired
    private SensorParserConfigHistoryService sensorParserHistoryService;

    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    ResponseEntity<SensorParserConfigHistory> findOne(@PathVariable String name) throws Exception {
        SensorParserConfigHistory sensorParserConfigHistory = sensorParserHistoryService.findOne(name);
        if (sensorParserConfigHistory != null) {
            return new ResponseEntity<>(sensorParserHistoryService.findOne(name), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<SensorParserConfigHistory>> getall() throws Exception {
        return new ResponseEntity<>(sensorParserHistoryService.getAll(), HttpStatus.OK);
    }

    @RequestMapping(value = "/history/{name}", method = RequestMethod.GET)
    ResponseEntity<List<SensorParserConfigHistory>> history(@PathVariable String name) throws Exception {
        return new ResponseEntity<>(sensorParserHistoryService.history(name), HttpStatus.OK);
    }
}
