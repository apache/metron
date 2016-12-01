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

import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.rest.model.ParseMessageRequest;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sensorParserConfig")
public class SensorParserConfigController {

  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  @RequestMapping(method = RequestMethod.POST)
  ResponseEntity<SensorParserConfig> save(@RequestBody SensorParserConfig sensorParserConfig) throws Exception {
    return new ResponseEntity<>(sensorParserConfigService.save(sensorParserConfig), HttpStatus.CREATED);
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<SensorParserConfig> findOne(@PathVariable String name) throws Exception {
    SensorParserConfig sensorParserConfig = sensorParserConfigService.findOne(name);
    if (sensorParserConfig != null) {
      return new ResponseEntity<>(sensorParserConfig, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<Iterable<SensorParserConfig>> findAll() throws Exception {
    return new ResponseEntity<>(sensorParserConfigService.getAll(), HttpStatus.OK);
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(@PathVariable String name) throws Exception {
    if (sensorParserConfigService.delete(name)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/list/available", method = RequestMethod.GET)
  ResponseEntity<Map<String, String>> getAvailable() throws Exception {
    return new ResponseEntity<>(sensorParserConfigService.getAvailableParsers(), HttpStatus.OK);
  }

  @RequestMapping(value = "/reload/available", method = RequestMethod.GET)
  ResponseEntity<Map<String, String>> reloadAvailable() throws Exception {
    return new ResponseEntity<>(sensorParserConfigService.reloadAvailableParsers(), HttpStatus.OK);
  }

  @RequestMapping(value = "/parseMessage", method = RequestMethod.POST)
  ResponseEntity<JSONObject> parseMessage(@RequestBody ParseMessageRequest parseMessageRequest) throws Exception {
    return new ResponseEntity<>(sensorParserConfigService.parseMessage(parseMessageRequest), HttpStatus.OK);
  }
}
