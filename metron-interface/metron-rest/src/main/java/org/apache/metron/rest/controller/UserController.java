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
import org.apache.metron.rest.model.UserSettings;
import org.apache.metron.rest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.SECURITY_ROLE_ADMIN;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

  /**
   * Service used to interact with users.
   */
  @Autowired
  private UserService userService;

  @ApiOperation(value = "Retrieves the current user")
  @ApiResponse(message = "Current user", code = 200)
  @RequestMapping(method = RequestMethod.GET)
    public String user(Principal user) {
        return user.getName();
    }

  @ApiOperation(value = "Retrieves the current user's settings")
  @ApiResponses(value = {@ApiResponse(message = "User settings", code = 200),
          @ApiResponse(message = "The current user does not have settings", code = 404)})
  @RequestMapping(value = "/settings", method = RequestMethod.GET)
  ResponseEntity<UserSettings> get() throws RestException {
    UserSettings userSettings = userService.getUserSettings();
    if (userSettings != null) {
      return new ResponseEntity<>(userSettings, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @Secured({"ROLE_" + SECURITY_ROLE_ADMIN})
  @ApiOperation(value = "Retrieves all users' settings.  Only users that are part of "
          + "the \"ROLE_ADMIN\" role are allowed to get all user settings.")
  @ApiResponses(value = {@ApiResponse(message = "List of all user settings", code = 200),
          @ApiResponse(message =
                  "The current user does not have permission to get all user settings", code = 403)})
  @RequestMapping(value = "/settings/all", method = RequestMethod.GET)
  ResponseEntity<Map<String, UserSettings>> findAll() throws RestException {
    return new ResponseEntity<>(userService.findAllUserSettings(), HttpStatus.OK);
  }

  @ApiOperation(value = "Creates or updates the current user's settings")
  @ApiResponses(value = {
          @ApiResponse(message = "User settings updated. Returns saved settings.", code = 200),
          @ApiResponse(message = "User settings created. Returns saved settings.", code = 201)})
  @RequestMapping(value = "/settings", method = RequestMethod.POST)
  ResponseEntity<UserSettings> save(@ApiParam(name = "userSettings", value =
          "The user settings to be saved", required = true) @RequestBody UserSettings userSettings)
          throws RestException {
    if (userService.getUserSettings() == null) {
      return new ResponseEntity<>(userService.saveUserSettings(userSettings), HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(userService.saveUserSettings(userSettings), HttpStatus.OK);
    }
  }

  @Secured({"ROLE_" + SECURITY_ROLE_ADMIN})
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
    if (userService.deleteUserSettings(user)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
