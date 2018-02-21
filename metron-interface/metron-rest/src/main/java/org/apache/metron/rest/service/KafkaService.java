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
package org.apache.metron.rest.service;

import java.util.Set;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.KafkaTopic;

/**
 * This is a set of operations created to interact with Kafka.
 */
public interface KafkaService {
  /**
   * Please see the following for documentation.
   *
   * @see <a href="https://kafka.apache.org/documentation/#impl_offsettracking">Kafka offset tracking documentation</a>.
   */
  String CONSUMER_OFFSETS_TOPIC = "__consumer_offsets";

  /**
   * Create a topic in Kafka for given information.
   * @param topic The information used to create a Kafka topic.
   * @return The Kafka topic created.
   * @throws RestException If exceptions occur when creating a topic they should be wrapped in a {@link RestException}.
   */
  KafkaTopic createTopic(KafkaTopic topic) throws RestException;

  /**
   * Delete a topic for a given name.
   * @param name The name of the topic to delete.
   * @return If topic was deleted true; otherwise false.
   */
  boolean deleteTopic(String name);

  /**
   * Retrieves the Kafka topic for a given name.
   * @param name The name of the Kafka topic to retrieve.
   * @return A {@link KafkaTopic} with the name of {@code name}. Null if topic with name, {@code name}, doesn't exist.
   */
  KafkaTopic getTopic(String name);

  /**
   * Returns a set of all topics.
   * @return A set of all topics in Kafka.
   */
  Set<String> listTopics();

  /**
   * Return a single sample message from a given topic.
   * @param topic The name of the topic to retrieve a sample message from.
   * @return A string representation of the sample message retrieved. If topic doesn't exist null will be returned.
   */
  String getSampleMessage(String topic);

  void produceMessage(String topic, String message) throws RestException;


  /**
   *
   * @param name The name of the Kafka topic to add the ACL.
   * @return If topic was present true; otherwise false.
   */
  boolean addACLToCurrentUser(String name);

}
