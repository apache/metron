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

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_ADMIN;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.AlertsProfile;
import org.apache.metron.rest.service.AlertsProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts/profile")
public class AlertsProfileController {

  @Autowired
  private AlertsProfileService alertsProfileService;

  @ApiOperation(value = "Retrieves the current user's alerts profile")
  @ApiResponses(value = {@ApiResponse(message = "Alerts profile", code = 200),
      @ApiResponse(message = "The current user does not have an alerts profile", code = 404)})
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<AlertsProfile> get() throws RestException {
    AlertsProfile alertsProfile = alertsProfileService.get();
    if (alertsProfile != null) {
      return new ResponseEntity<>(alertsProfile, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @Secured({"ROLE_" + SECURITY_ROLE_ADMIN})
  @ApiOperation(value = "Retrieves all users' alerts profiles.  Only users that are part of "
      + "the \"ROLE_ADMIN\" role are allowed to get all alerts profiles.")
  @ApiResponses(value = {@ApiResponse(message = "List of all alerts profiles", code = 200),
      @ApiResponse(message =
          "The current user does not have permission to get all alerts profiles", code = 403)})
  @RequestMapping(value = "/all", method = RequestMethod.GET)
  ResponseEntity<Iterable<AlertsProfile>> findAll() throws RestException {
    return new ResponseEntity<>(alertsProfileService.findAll(), HttpStatus.OK);
  }

  @RequestMapping(method = RequestMethod.POST)
  @ApiOperation(value = "Creates or updates the current user's alerts profile")
  @ApiResponses(value = {
      @ApiResponse(message = "Alerts profile updated. Returns saved alerts profile.", code = 200),
      @ApiResponse(message = "Alerts profile created. Returns saved alerts profile.", code = 201)})
  ResponseEntity<AlertsProfile> save(@ApiParam(name = "alertsProfile", value =
      "The alerts profile to be saved", required = true) @RequestBody AlertsProfile alertsProfile)
      throws RestException {
    if (alertsProfileService.get() == null) {
      return new ResponseEntity<>(alertsProfileService.save(alertsProfile), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(alertsProfileService.save(alertsProfile), HttpStatus.OK);
    }
  }

  @Secured({"ROLE_" + SECURITY_ROLE_ADMIN})
  @ApiOperation(value = "Deletes a user's alerts profile.  Only users that are part of "
      + "the \"ROLE_ADMIN\" role are allowed to delete user alerts profiles.")
  @ApiResponses(value = {@ApiResponse(message = "Alerts profile was deleted", code = 200),
      @ApiResponse(message = "The current user does not have permission to delete alerts profiles",
          code = 403),
      @ApiResponse(message = "Alerts profile could not be found", code = 404)})
  @RequestMapping(value = "/{user}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(
      @ApiParam(name = "user", value = "The user whose prolife will be deleted", required = true)
      @PathVariable String user)
      throws RestException {
    if (alertsProfileService.delete(user)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
