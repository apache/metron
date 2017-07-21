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
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.HdfsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/api/v1/hdfs")
public class HdfsController {

  @Autowired
  private HdfsService hdfsService;

  @ApiOperation(value = "Lists an HDFS directory")
  @ApiResponse(message = "HDFS directory list", code = 200)
  @RequestMapping(value = "/list", method = RequestMethod.GET)
  ResponseEntity<List<String>> list(@ApiParam(name = "path", value = "Path to HDFS directory", required = true) @RequestParam String path) throws RestException {
    return new ResponseEntity<>(hdfsService.list(new Path(path)), HttpStatus.OK);
  }

  @ApiOperation(value = "Reads a file from HDFS and returns the contents")
  @ApiResponse(message = "Returns file contents", code = 200)
  @RequestMapping(method = RequestMethod.GET)
  ResponseEntity<String> read(@ApiParam(name = "path", value = "Path to HDFS file", required = true) @RequestParam String path) throws RestException {
    String contents = hdfsService.read(new Path(path));
    if (contents != null) {
      return new ResponseEntity<>(hdfsService.read(new Path(path)), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

  }

  @ApiOperation(value = "Writes contents to an HDFS file.  Warning: this will overwrite the contents of a file if it already exists.")
  @ApiResponse(message = "Contents were written", code = 200)
  @RequestMapping(method = RequestMethod.POST)
  ResponseEntity<Void> write(@ApiParam(name="path", value="Path to HDFS file", required=true) @RequestParam String path,
                                              @ApiParam(name="contents", value="File contents", required=true) @RequestBody String contents) throws RestException {
    hdfsService.write(new Path(path), contents.getBytes(UTF_8));
    return new ResponseEntity<>(HttpStatus.OK);

  }

  @ApiOperation(value = "Deletes a file from HDFS")
  @ApiResponses(value = { @ApiResponse(message = "File was deleted", code = 200),
          @ApiResponse(message = "File was not found in HDFS", code = 404) })
  @RequestMapping(method = RequestMethod.DELETE)
  ResponseEntity<Boolean> delete(@ApiParam(name = "path", value = "Path to HDFS file", required = true) @RequestParam String path,
                                 @ApiParam(name = "recursive", value = "Delete files recursively") @RequestParam(required = false, defaultValue = "false") boolean recursive) throws RestException {
    if (hdfsService.delete(new Path(path), recursive)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
