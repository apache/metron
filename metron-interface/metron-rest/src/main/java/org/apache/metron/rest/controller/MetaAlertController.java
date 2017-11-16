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

  @ApiOperation(value = "Get all meta alerts that contain an alert.")
  @ApiResponse(message = "Search results", code = 200)
  @RequestMapping(value = "/searchByAlert", method = RequestMethod.POST)
  ResponseEntity<SearchResponse> searchByAlert(
      @ApiParam(name = "guid", value = "Alert GUID", required = true)
      @RequestBody final String guid
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.getAllMetaAlertsForAlert(guid), HttpStatus.OK);
  }

  @ApiOperation(value = "Creates a new meta alert from a list of existing alerts.  "
      + "The meta alert status will initially be set to 'ACTIVE' and summary statistics "
      + "will be computed from the list of alerts.  A list of groups included in the request are also added to the meta alert.")
  @ApiResponse(message = "The GUID of the new meta alert", code = 200)
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  ResponseEntity<MetaAlertCreateResponse> create(
      @ApiParam(name = "createRequest", value = "Meta alert create request which includes a list of alert "
          + "get requests and a list of custom groups used to annotate a meta alert", required = true)
      @RequestBody  final MetaAlertCreateRequest createRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.create(createRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Adds an alert to an existing meta alert.  An alert will not be added if it is already contained in a meta alert.")
  @ApiResponse(message = "Returns 'true' if the alert was added and 'false' if the meta alert did not change.", code = 200)
  @RequestMapping(value = "/add/alert", method = RequestMethod.POST)
  ResponseEntity<Boolean> addAlertsToMetaAlert(
      @ApiParam(name = "metaAlertAddRemoveRequest", value = "Meta alert add request which includes a meta alert GUID and list of alert get requests", required = true)
      @RequestBody  final MetaAlertAddRemoveRequest metaAlertAddRemoveRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.addAlertsToMetaAlert(metaAlertAddRemoveRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Removes an alert from an existing meta alert.  If the alert to be removed is not in a meta alert, 'false' will be returned.")
  @ApiResponse(message = "Returns 'true' if the alert was removed and 'false' if the meta alert did not change.", code = 200)
  @RequestMapping(value = "/remove/alert", method = RequestMethod.POST)
  ResponseEntity<Boolean> removeAlertsFromMetaAlert(
      @ApiParam(name = "metaAlertAddRemoveRequest", value = "Meta alert remove request which includes a meta alert GUID and list of alert get requests", required = true)
      @RequestBody  final MetaAlertAddRemoveRequest metaAlertAddRemoveRequest
  ) throws RestException {
    return new ResponseEntity<>(metaAlertService.removeAlertsFromMetaAlert(metaAlertAddRemoveRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Updates the status of a meta alert to either 'ACTIVE' or 'INACTIVE'.")
  @ApiResponse(message = "Returns 'true' if the status changed and 'false' if it did not.", code = 200)
  @RequestMapping(value = "/update/status/{guid}/{status}", method = RequestMethod.POST)
  ResponseEntity<Boolean> updateMetaAlertStatus(
      final @ApiParam(name = "guid", value = "Meta alert GUID", required = true)
      @PathVariable String guid,
      final @ApiParam(name = "status", value = "Meta alert status with a value of either 'ACTIVE' or 'INACTIVE'", required = true)
      @PathVariable String status) throws RestException {
    return new ResponseEntity<>(metaAlertService.updateMetaAlertStatus(guid,
        MetaAlertStatus.valueOf(status.toUpperCase())), HttpStatus.OK);
  }
}

