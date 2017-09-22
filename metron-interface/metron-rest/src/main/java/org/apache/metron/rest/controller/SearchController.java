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
import org.apache.metron.indexing.dao.search.GetRequest;
import org.apache.metron.indexing.dao.search.GroupRequest;
import org.apache.metron.indexing.dao.search.GroupResponse;
import org.apache.metron.indexing.dao.search.FieldType;
import org.apache.metron.rest.RestException;
import org.apache.metron.indexing.dao.search.SearchRequest;
import org.apache.metron.indexing.dao.search.SearchResponse;
import org.apache.metron.rest.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

  @Autowired
  private SearchService searchService;

  @ApiOperation(value = "Searches the indexing store")
  @ApiResponse(message = "Search results", code = 200)
  @RequestMapping(value = "/search", method = RequestMethod.POST)
  ResponseEntity<SearchResponse> search(final @ApiParam(name = "searchRequest", value = "Search request", required = true) @RequestBody SearchRequest searchRequest) throws RestException {
    return new ResponseEntity<>(searchService.search(searchRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Searches the indexing store and returns field groups. "
      + "Groups are hierarchical and nested in the order the fields appear in the 'groups' request parameter. "
      + "The default sorting within groups is by count descending.")
  @ApiResponse(message = "Group response", code = 200)
  @RequestMapping(value = "/group", method = RequestMethod.POST)
  ResponseEntity<GroupResponse> group(final @ApiParam(name = "groupRequest", value = "Group request", required = true) @RequestBody GroupRequest groupRequest) throws RestException {
    return new ResponseEntity<>(searchService.group(groupRequest), HttpStatus.OK);
  }

  @ApiOperation(value = "Returns latest document for a guid and sensor")
  @ApiResponse(message = "Document representing the output", code = 200)
  @RequestMapping(value = "/findOne", method = RequestMethod.POST)
  ResponseEntity<Map<String, Object>> getLatest(
          final @ApiParam(name = "getRequest", value = "Get Request", required = true)
                @RequestBody
          GetRequest request
  ) throws RestException
  {
    Optional<Map<String, Object>> latest = searchService.getLatest(request);
    if(latest.isPresent()) {
      return new ResponseEntity<>(latest.get(), HttpStatus.OK);
    }
    else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Get column metadata for each index in the list of indices")
  @ApiResponse(message = "Column Metadata", code = 200)
  @RequestMapping(value = "/column/metadata", method = RequestMethod.POST)
  ResponseEntity<Map<String, Map<String, FieldType>>> getColumnMetadata(final @ApiParam(name = "indices", value = "Indices", required = true) @RequestBody List<String> indices) throws RestException {
    return new ResponseEntity<>(searchService.getColumnMetadata(indices), HttpStatus.OK);
  }

  @ApiOperation(value = "Get metadata for columns shared by the list of indices")
  @ApiResponse(message = "Common Column Metadata", code = 200)
  @RequestMapping(value = "/column/metadata/common", method = RequestMethod.POST)
  ResponseEntity<Map<String, FieldType>> getCommonColumnMetadata(final @ApiParam(name = "indices", value = "Indices", required = true) @RequestBody List<String> indices) throws RestException {
    return new ResponseEntity<>(searchService.getCommonColumnMetadata(indices), HttpStatus.OK);
  }
}
