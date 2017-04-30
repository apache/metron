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
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;


@RestController
@RequestMapping("/api/v1/ext/parsers")
public class ParserExtensionController {
  @Autowired
  private ExtensionService extensionService;

  @ApiOperation(value = "Install a Metron Parser Extension into the system")
  @ApiResponses(value = { @ApiResponse(message = "Parser Extension Installed", code = 201)})
  @PostMapping()
  DeferredResult<ResponseEntity<Void>> install(@ApiParam(name="extensionTgz", value="Metron Parser Extension tar.gz", required=true)@RequestParam("extensionTgz") MultipartFile extensionTgz) throws RestException {
    DeferredResult<ResponseEntity<Void>> result = new DeferredResult<>();

    try(TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
            new GzipCompressorInputStream(
                    new BufferedInputStream(
                            extensionTgz.getInputStream())))){
        extensionService.install(ExtensionService.ExtensionType.PARSER, extensionTgz.getName(), tarArchiveInputStream);
    }catch(Exception e){
      throw new RestException(e);
    }
    return result;
  }

}
