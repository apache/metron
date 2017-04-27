/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentConfig;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.HdfsService;
import org.apache.metron.rest.service.KafkaService;
import org.apache.metron.rest.service.SensorEnrichmentConfigService;
import org.apache.metron.rest.service.SensorIndexingConfigService;
import org.apache.metron.rest.service.SensorParserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/api/v1/ext/parsers")
public class ParserExtensionController {
  @Autowired
  private HdfsService hdfsService;
  @Autowired
  private KafkaService kafkaService;
  @Autowired
  private SensorEnrichmentConfigService sensorEnrichmentConfigService;
  @Autowired
  private SensorIndexingConfigService sensorIndexingConfigService;
  @Autowired
  private SensorParserConfigService sensorParserConfigService;

  @ApiOperation(value = "Install a Metron Parser Extension into the system")
  @ApiResponses(value = { @ApiResponse(message = "Parser Extension Installed", code = 201)})
  @RequestMapping(method = RequestMethod.POST)
  DeferredResult<ResponseEntity<Void>> install(@ApiParam(name="extensionTgz", value="Metron Parser Extension tar.gz", required=true)@RequestParam("extensionTgz") MultipartFile extensionTgz) throws RestException {
    DeferredResult<ResponseEntity<Void>> result = new DeferredResult<>();

    /*
      Operation need
      1. get a tar.gz stream for the file
      2. verify the structure and the contents
         a. must have a config dir
         b. MAY have a .bundle file
      3. send the configurations to the proper configuration services
      4. save the bundle to hdfs
     */
    return result;
  }

}
