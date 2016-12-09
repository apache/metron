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

import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.rest.service.SensorEnrichmentConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensorEnrichmentConfig")
public class SensorEnrichmentConfigController {

  @Autowired
  private SensorEnrichmentConfigService sensorEnrichmentConfigService;

  @RequestMapping(value = "/{name}", method = RequestMethod.POST)
  ResponseEntity<SensorEnrichmentConfig> save(@PathVariable String name, @RequestBody SensorEnrichmentConfig sensorEnrichmentConfig) throws Exception {
    return new ResponseEntity<>(sensorEnrichmentConfigService.save(name, sensorEnrichmentConfig), HttpStatus.CREATED);
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<SensorEnrichmentConfig> findOne(@PathVariable String name) throws Exception {
    SensorEnrichmentConfig sensorEnrichmentConfig = sensorEnrichmentConfigService.findOne(name);
    if (sensorEnrichmentConfig != null) {
      return new ResponseEntity<>(sensorEnrichmentConfig, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }


  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<List<SensorEnrichmentConfig>> getAll() throws Exception {
    return new ResponseEntity<>(sensorEnrichmentConfigService.getAll(), HttpStatus.OK);
  }

  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(@PathVariable String name) throws Exception {
    if (sensorEnrichmentConfigService.delete(name)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/list/available", method = RequestMethod.GET)
  ResponseEntity<List<String>> getAvailable() throws Exception {
    return new ResponseEntity<>(sensorEnrichmentConfigService.getAvailableEnrichments(), HttpStatus.OK);
  }
}
