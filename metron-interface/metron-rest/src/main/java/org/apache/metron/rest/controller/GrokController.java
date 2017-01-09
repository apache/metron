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

import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/grok")
public class GrokController {

    @Autowired
    private GrokService grokService;

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    ResponseEntity<GrokValidation> post(@RequestBody GrokValidation grokValidation) throws Exception {
        return new ResponseEntity<>(grokService.validateGrokStatement(grokValidation), HttpStatus.OK);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    ResponseEntity<Map<String, String>> list() throws Exception {
        return new ResponseEntity<>(grokService.getCommonGrokPatterns(), HttpStatus.OK);
    }
}
