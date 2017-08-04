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
  @RequestMapping(value = "/all", method = RequestMethod.GET)
  ResponseEntity<Iterable<AlertsProfile>> findAll() throws RestException {
    return new ResponseEntity<>(alertsProfileService.findAll(), HttpStatus.OK);
  }

  @RequestMapping(method = RequestMethod.POST)
  ResponseEntity<AlertsProfile> save(@RequestBody AlertsProfile alertsProfile)
      throws RestException {
    AlertsProfile savedAlertsProfile = alertsProfileService.save(alertsProfile);
    return new ResponseEntity<>(savedAlertsProfile, HttpStatus.OK);
  }

  @Secured({"ROLE_" + SECURITY_ROLE_ADMIN})
  @RequestMapping(value = "/{user}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(@PathVariable String user) throws RestException {
    if (alertsProfileService.delete(user)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
