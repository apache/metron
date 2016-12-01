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

import java.util.Set;

@RestController
@RequestMapping("/api/v1/kafka")
public class KafkaController {

    @Autowired
    private KafkaService kafkaService;

    @RequestMapping(value = "/topic", method = RequestMethod.POST)
    ResponseEntity<KafkaTopic> save(@RequestBody KafkaTopic topic) throws Exception {
        return new ResponseEntity<>(kafkaService.createTopic(topic), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/topic/{name}", method = RequestMethod.GET)
    ResponseEntity<KafkaTopic> get(@PathVariable String name) throws Exception {
        KafkaTopic kafkaTopic = kafkaService.getTopic(name);
        if (kafkaTopic != null) {
            return new ResponseEntity<>(kafkaTopic, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/topic", method = RequestMethod.GET)
    ResponseEntity<Set<String>> list() throws Exception {
        return new ResponseEntity<>(kafkaService.listTopics(), HttpStatus.OK);
    }

    @RequestMapping(value = "/topic/{name}", method = RequestMethod.DELETE)
    ResponseEntity<Void> delete(@PathVariable String name) throws Exception {
        if (kafkaService.deleteTopic(name)) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/topic/{name}/sample", method = RequestMethod.GET)
    ResponseEntity<String> getSample(@PathVariable String name) throws Exception {
        String sampleMessage = kafkaService.getSampleMessage(name);
        if (sampleMessage != null) {
            return new ResponseEntity<>(sampleMessage, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
