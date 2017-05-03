/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.metron.common.configuration.extensions.ParserExtensionConfig;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/ext/parsers")
public class ParserExtensionController {
  @Autowired
  private ExtensionService extensionService;

  @ApiOperation(value = "Install a Metron Parser Extension into the system")
  @ApiResponses(value = {@ApiResponse(message = "Parser Extension Installed", code = 201)})
  @PostMapping()
  DeferredResult<ResponseEntity<Void>> install(@ApiParam(name = "extensionTgz", value = "Metron Parser Extension tar.gz", required = true) @RequestParam("extensionTgz") MultipartFile extensionTgz) throws RestException {
    DeferredResult<ResponseEntity<Void>> result = new DeferredResult<>();
    result.setResult(new ResponseEntity<Void>(HttpStatus.CREATED));

    try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
            new GzipCompressorInputStream(
                    new BufferedInputStream(
                            extensionTgz.getInputStream())))) {
      extensionService.install(ExtensionService.ExtensionType.PARSER, extensionTgz.getOriginalFilename(), tarArchiveInputStream);
    } catch (Exception e) {
      throw new RestException(e);
    }
    return result;
  }

  @ApiOperation(value = "Retrieves a ParserExtensionConfig from Zookeeper")
  @ApiResponses(value = {@ApiResponse(message = "Returns ParserExtensionConfig", code = 200),
          @ApiResponse(message = "ParserExtensionConfig is missing", code = 404)})
  @RequestMapping(value = "/{name}", method = RequestMethod.GET)
  ResponseEntity<ParserExtensionConfig> findOne(@ApiParam(name = "name", value = "ParserExtensionConfig name", required = true) @PathVariable String name) throws RestException {
    ParserExtensionConfig parserExtensionConfig = extensionService.findOneParserExtension(name);
    if (parserExtensionConfig != null) {
      return new ResponseEntity<>(parserExtensionConfig, HttpStatus.OK);
    }

    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @ApiOperation(value = "Retrieves all ParserExtensionConfigs from Zookeeper")
  @ApiResponse(message = "Returns all ParserExtensionConfigs", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<Map<String, ParserExtensionConfig>> findAll() throws RestException {
    return new ResponseEntity<Map<String, ParserExtensionConfig>>(extensionService.getAllParserExtensions(), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes a ParserExtensionConfig from Zookeeper")
  @ApiResponses(value = {@ApiResponse(message = "ParserExtensionConfig was deleted", code = 200),
          @ApiResponse(message = "ParserExtensionConfig is missing", code = 404)})
  @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
  DeferredResult<ResponseEntity<Void>> delete(@ApiParam(name = "name", value = "SensorParserConfig name", required = true) @PathVariable String name) throws RestException {
    DeferredResult<ResponseEntity<Void>> result = new DeferredResult<>();
    if (extensionService.deleteParserExtension(name)) {
      result.setResult(new ResponseEntity<Void>(HttpStatus.OK));
    } else {
      result.setResult(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }
    return result;
  }
}
