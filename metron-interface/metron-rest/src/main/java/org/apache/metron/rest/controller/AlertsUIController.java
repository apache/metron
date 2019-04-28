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
import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_PREFIX;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.annotations.ApiResponses;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.AlertsUIUserSettings;
import org.apache.metron.rest.service.AlertsUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API resource that is used for alert-related operations.
 */
@RestController
@RequestMapping("/api/v1/alerts/ui")
public class AlertsUIController {

  /**
   * Service used to interact with alerts.
   */
  @Autowired
  private AlertsUIService alertsUIService;

  @ApiOperation(value = "Escalates a list of alerts by producing it to the Kafka escalate topic")
  @ApiResponse(message = "Alerts were escalated", code = 200)
  @RequestMapping(value = "/escalate", method = RequestMethod.POST)
  ResponseEntity<Void> escalate(final @ApiParam(name = "alerts", value = "The alerts to be escalated", required = true) @RequestBody List<Map<String, Object>> alerts) throws RestException {
    alertsUIService.escalateAlerts(alerts);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @ApiOperation(value = "Retrieves the current user's settings")
  @ApiResponses(value = {@ApiResponse(message = "User settings", code = 200),
          @ApiResponse(message = "The current user does not have settings", code = 404)})
  @RequestMapping(value = "/settings", method = RequestMethod.GET)
  ResponseEntity<AlertsUIUserSettings> get() throws RestException {
    Optional<AlertsUIUserSettings> alertUserSettings = alertsUIService.getAlertsUIUserSettings();
    if (alertUserSettings.isPresent()) {
      return new ResponseEntity<>(alertUserSettings.get(), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @Secured({SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN})
  @ApiOperation(value = "Retrieves all users' settings.  Only users that are part of "
          + "the \"ROLE_ADMIN\" role are allowed to get all user settings.")
  @ApiResponses(value = {@ApiResponse(message = "List of all user settings", code = 200),
          @ApiResponse(message =
                  "The current user does not have permission to get all user settings", code = 403)})
  @RequestMapping(value = "/settings/all", method = RequestMethod.GET)
  ResponseEntity<Map<String, AlertsUIUserSettings>> findAll() throws RestException {
    return new ResponseEntity<>(alertsUIService.findAllAlertsUIUserSettings(), HttpStatus.OK);
  }

  @ApiOperation(value = "Creates or updates the current user's settings")
  @ApiResponses(value = {
          @ApiResponse(message = "User settings updated. Returns saved settings.", code = 200),
          @ApiResponse(message = "User settings created. Returns saved settings.", code = 201)})
  @RequestMapping(value = "/settings", method = RequestMethod.POST)
  ResponseEntity<Void> save(@ApiParam(name = "alertsUIUserSettings", value =
          "The user settings to be saved", required = true) @RequestBody AlertsUIUserSettings alertsUIUserSettings)
          throws RestException {
    ResponseEntity<Void> responseEntity;
    if (alertsUIService.getAlertsUIUserSettings().isPresent()) {
      responseEntity = new ResponseEntity<>(HttpStatus.OK);
    } else {
      responseEntity = new ResponseEntity<>(HttpStatus.CREATED);
    }
    alertsUIService.saveAlertsUIUserSettings(alertsUIUserSettings);
    return responseEntity;
  }

  @Secured({SECURITY_ROLE_PREFIX + SECURITY_ROLE_ADMIN})
  @ApiOperation(value = "Deletes a user's settings.  Only users that are part of "
          + "the \"ROLE_ADMIN\" role are allowed to delete user settings.")
  @ApiResponses(value = {@ApiResponse(message = "User settings were deleted", code = 200),
          @ApiResponse(message = "The current user does not have permission to delete user settings",
                  code = 403),
          @ApiResponse(message = "User settings could not be found", code = 404)})
  @RequestMapping(value = "/settings/{user}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(
          @ApiParam(name = "user", value = "The user whose settings will be deleted", required = true)
          @PathVariable String user)
          throws RestException {
    if (alertsUIService.deleteAlertsUIUserSettings(user)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
