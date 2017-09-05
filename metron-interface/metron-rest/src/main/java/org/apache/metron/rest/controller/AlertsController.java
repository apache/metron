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
import java.util.List;
import java.util.Map;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API resource that is used for alert-related operations.
 */
@RestController
@RequestMapping("/api/v1/alert")
public class AlertsController {

  /**
   * Service used to interact with alerts.
   */
  @Autowired
  private AlertService alertService;

  @ApiOperation(value = "Escalates a list of alerts by producing it to the Kafka escalate topic")
  @ApiResponse(message = "Alerts were escalated", code = 200)
  @RequestMapping(value = "/escalate", method = RequestMethod.POST)
  ResponseEntity<Void> escalate(final @ApiParam(name = "alerts", value = "The alerts to be escalated", required = true) @RequestBody List<Map<String, Object>> alerts) throws RestException {
    alertService.escalateAlerts(alerts);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
