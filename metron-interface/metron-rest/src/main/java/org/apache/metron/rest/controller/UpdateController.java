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
import org.apache.metron.indexing.dao.update.CommentAddRemoveRequest;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.indexing.dao.update.OriginalNotFoundException;
import org.apache.metron.indexing.dao.update.PatchRequest;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/update")
public class UpdateController {

  @Autowired
  private UpdateService service;

  @ApiOperation(value = "Update a document with a patch")
  @ApiResponse(message = "Returns the complete patched document.", code = 200)
  @RequestMapping(value = "/patch", method = RequestMethod.PATCH)
  ResponseEntity<Document> patch(
          final @ApiParam(name = "request", value = "Patch request", required = true)
                @RequestBody
          PatchRequest request
  ) throws RestException {
    try {
      return new ResponseEntity<>(service.patch(request), HttpStatus.OK);
    } catch (OriginalNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Add a comment to an alert")
  @ApiResponse(message = "Returns the complete alert document with comments added.", code = 200)
  @RequestMapping(value = "/add/comment", method = RequestMethod.POST)
  ResponseEntity<Document> addCommentToAlert(
      @RequestBody @ApiParam(name = "request", value = "Comment add request", required = true) final
      CommentAddRemoveRequest request
  ) throws RestException {
    return new ResponseEntity<>(service.addComment(request), HttpStatus.OK);
  }

  @ApiOperation(value = "Remove a comment from an alert")
  @ApiResponse(message = "Returns the complete alert document with comments removed.", code = 200)
  @RequestMapping(value = "/remove/comment", method = RequestMethod.POST)
  ResponseEntity<Document> removeCommentFromAlert(
      @RequestBody @ApiParam(name = "request", value = "Comment remove request", required = true) final
      CommentAddRemoveRequest request
  ) throws RestException {
    return new ResponseEntity<>(service.removeComment(request), HttpStatus.OK);
  }
}
