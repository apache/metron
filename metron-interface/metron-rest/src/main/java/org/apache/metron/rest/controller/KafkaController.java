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
import java.util.Set;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.KafkaTopic;
import org.apache.metron.rest.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The API resource that is use to interact with Kafka.
 */
@RestController
@RequestMapping("/api/v1/kafka")
public class KafkaController {

  /**
   * Service used to interact with Kafka.
   */
  @Autowired
  private KafkaService kafkaService;

  @ApiOperation(value = "Creates a new Kafka topic")
  @ApiResponses({
    @ApiResponse(message = "Returns saved Kafka topic", code = 200)
  })
  @RequestMapping(value = "/topic", method = RequestMethod.POST)
  ResponseEntity<KafkaTopic> save(final @ApiParam(name = "topic", value = "Kafka topic", required = true) @RequestBody KafkaTopic topic) throws RestException {
    return new ResponseEntity<>(kafkaService.createTopic(topic), HttpStatus.CREATED);
  }

  @ApiOperation(value = "Retrieves a Kafka topic")
  @ApiResponses(value = {
    @ApiResponse(message = "Returns Kafka topic", code = 200),
    @ApiResponse(message = "Kafka topic is missing", code = 404)
  })
  @RequestMapping(value = "/topic/{name}", method = RequestMethod.GET)
  ResponseEntity<KafkaTopic> get(final @ApiParam(name = "name", value = "Kafka topic name", required = true) @PathVariable String name) throws RestException {
    KafkaTopic kafkaTopic = kafkaService.getTopic(name);
    if (kafkaTopic != null) {
      return new ResponseEntity<>(kafkaTopic, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Retrieves all Kafka topics")
  @ApiResponses({
    @ApiResponse(message = "Returns a list of all Kafka topics", code = 200)
  })
  @RequestMapping(value = "/topic", method = RequestMethod.GET)
  ResponseEntity<Set<String>> list() throws Exception {
    return new ResponseEntity<>(kafkaService.listTopics(), HttpStatus.OK);
  }

  @ApiOperation(value = "Deletes a Kafka topic")
  @ApiResponses(value = {
    @ApiResponse(message = "Kafka topic was deleted", code = 200),
    @ApiResponse(message = "Kafka topic is missing", code = 404)
  })
  @RequestMapping(value = "/topic/{name}", method = RequestMethod.DELETE)
  ResponseEntity<Void> delete(final @ApiParam(name = "name", value = "Kafka topic name", required = true) @PathVariable String name) throws RestException {
    if (kafkaService.deleteTopic(name)) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Retrieves a sample message from a Kafka topic using the most recent offset")
  @ApiResponses(value = {
    @ApiResponse(message = "Returns sample message", code = 200),
    @ApiResponse(message = "Either Kafka topic is missing or contains no messages", code = 404)
  })
  @RequestMapping(value = "/topic/{name}/sample", method = RequestMethod.GET)
  ResponseEntity<String> getSample(final @ApiParam(name = "name", value = "Kafka topic name", required = true) @PathVariable String name) throws RestException {
    String sampleMessage = kafkaService.getSampleMessage(name);
    if (sampleMessage != null) {
      return new ResponseEntity<>(sampleMessage, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @ApiOperation(value = "Produces a message to a Kafka topic")
  @ApiResponses(value = {
      @ApiResponse(message = "Message produced successfully", code = 200)
  })
  @RequestMapping(value = "/topic/{name}/produce", method = RequestMethod.POST)
  ResponseEntity<String> produce(final @ApiParam(name = "name", value = "Kafka topic name", required = true) @PathVariable String name,
      final @ApiParam(name = "message", value = "Message", required = true) @RequestBody String message) throws RestException {
    kafkaService.produceMessage(name, message);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
