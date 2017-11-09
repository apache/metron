/*
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
import org.apache.metron.indexing.dao.metaalert.MetaAlertStatus;
import org.apache.metron.indexing.dao.metaalert.MetaAlertAddRemoveRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateRequest;
import org.apache.metron.indexing.dao.metaalert.MetaAlertCreateResponse;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.MetaAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metaalert")
public class MetaAlertController {

  @Autowired
  private MetaAlertService metaAlertService;

  @ApiOperation(value = "Get all meta alerts for alert")
  @ApiResponse(message = "Search results", code = 200)
  @RequestMapping(value = "/searchByAlert", method = RequestMethod.POST)
  ResponseEntity<SearchResponse> searchByAlert(
      @ApiParam(name = "guid", value = "GUID", required = true)
      @RequestBody final String guid
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.getAllMetaAlertsForAlert(guid), HttpStatus.OK);
  }

  @ApiOperation(value = "Create a meta alert")
  @ApiResponse(message = "Created meta alert", code = 200)
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  ResponseEntity<MetaAlertCreateResponse> create(
      @ApiParam(name = "request", value = "Meta Alert Create Request", required = true)
      @RequestBody  final MetaAlertCreateRequest createRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.create(createRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Create a meta alert")
  @ApiResponse(message = "Created meta alert", code = 200)
  @RequestMapping(value = "/add/alert", method = RequestMethod.POST)
  ResponseEntity<Boolean> addAlertsToMetaAlert(
      @ApiParam(name = "request", value = "Meta Alert Create Request", required = true)
      @RequestBody  final MetaAlertAddRemoveRequest metaAlertAddRemoveRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.addAlertsToMetaAlert(metaAlertAddRemoveRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Create a meta alert")
  @ApiResponse(message = "Created meta alert", code = 200)
  @RequestMapping(value = "/remove/alert", method = RequestMethod.POST)
  ResponseEntity<Boolean> removeAlertsFromMetaAlert(
      @ApiParam(name = "request", value = "Meta Alert Create Request", required = true)
      @RequestBody  final MetaAlertAddRemoveRequest metaAlertAddRemoveRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.removeAlertsFromMetaAlert(metaAlertAddRemoveRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Create a meta alert")
  @ApiResponse(message = "Created meta alert", code = 200)
  @RequestMapping(value = "/update/status/{guid}/{status}", method = RequestMethod.POST)
  ResponseEntity<Boolean> updateMetaAlertStatus(
      final @ApiParam(name = "guid", value = "Kafka topic name", required = true)
      @PathVariable String guid,
      final @ApiParam(name = "status", value = "Kafka topic name", required = true)
      @PathVariable String status) throws RestException {
    return new ResponseEntity<>(metaAlertService.updateMetaAlertStatus(guid,
        MetaAlertStatus.valueOf(status.toUpperCase())), HttpStatus.OK);
  }
}

