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
package org.apache.metron.rest.service.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.KafkaTopic;
import org.apache.metron.rest.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

/**
 * The default service layer implementation of {@link KafkaService}.
 *
 * @see KafkaService
 */
@Service
public class KafkaServiceImpl implements KafkaService {
  /**
   * The timeout used when polling Kafka.
   */
  private static final int KAFKA_CONSUMER_TIMEOUT = 100;

  private final ConsumerFactory<String, String> kafkaConsumerFactory;
  private final KafkaProducer<String, String> kafkaProducer;
  private final AdminClient adminClient;

  @Autowired
  private Environment environment;

  /**
   * @param kafkaConsumerFactory A class used to create {@link KafkaConsumer} in order to interact with Kafka.
   * @param kafkaProducer        A class used to produce messages to Kafka.
   * @param adminClient           A utility class used to do administration operations on Kafka.
   */
  @Autowired
  public KafkaServiceImpl(final ConsumerFactory<String, String> kafkaConsumerFactory,
                          final KafkaProducer<String, String> kafkaProducer,
                          final AdminClient adminClient) {
    this.kafkaConsumerFactory = kafkaConsumerFactory;
    this.kafkaProducer = kafkaProducer;
    this.adminClient = adminClient;
  }

  @Override
  public KafkaTopic createTopic(final KafkaTopic topic) throws RestException {
    if (!listTopics().contains(topic.getName())) {
      try {
        CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singletonList(new NewTopic(topic.getName(), topic.getNumPartitions(), (short) topic.getReplicationFactor())));
        createTopicsResult.all();
        if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)){
          addACLToCurrentUser(topic.getName());
        }
      } catch (KafkaException e) {
        throw new RestException(e);
      }
    }
    return topic;
  }

  @Override
  public boolean deleteTopic(final String name) {
    final Set<String> topics = listTopics();
    if (topics != null && topics.contains(name)) {
      adminClient.deleteTopics(Collections.singletonList(name));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public KafkaTopic getTopic(final String name) {
    KafkaTopic kafkaTopic = null;
    if (listTopics().contains(name)) {
      try (Consumer<String, String> consumer = kafkaConsumerFactory.createConsumer()) {
        final List<PartitionInfo> partitionInfos = consumer.partitionsFor(name);
        if (partitionInfos.size() > 0) {
          final PartitionInfo partitionInfo = partitionInfos.get(0);
          kafkaTopic = new KafkaTopic();
          kafkaTopic.setName(name);
          kafkaTopic.setNumPartitions(partitionInfos.size());
          kafkaTopic.setReplicationFactor(partitionInfo.replicas().length);
        }
      }
    }
    return kafkaTopic;
  }

  @Override
  public Set<String> listTopics() {
    try (Consumer<String, String> consumer = kafkaConsumerFactory.createConsumer()) {
      final Map<String, List<PartitionInfo>> topicsInfo = consumer.listTopics();
      final Set<String> topics = topicsInfo == null ? new HashSet<>() : topicsInfo.keySet();
      topics.remove(CONSUMER_OFFSETS_TOPIC);
      return topics;
    }
  }

  @Override
  public String getSampleMessage(final String topic) {
    String message = null;
    if (listTopics().contains(topic)) {
      try (Consumer<String, String> kafkaConsumer = kafkaConsumerFactory.createConsumer()) {
        kafkaConsumer.assign(kafkaConsumer.partitionsFor(topic).stream()
          .map(partitionInfo -> new TopicPartition(topic, partitionInfo.partition()))
          .collect(Collectors.toList()));

        kafkaConsumer.assignment().stream()
          .filter(p -> (kafkaConsumer.position(p) - 1) >= 0)
          .forEach(p -> kafkaConsumer.seek(p, kafkaConsumer.position(p) - 1));

        final ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(KAFKA_CONSUMER_TIMEOUT));
        message = records.isEmpty() ? null : records.iterator().next().value();
        kafkaConsumer.unsubscribe();
      }
    }
    return message;
  }

  @Override
  public void produceMessage(String topic, String message) throws RestException {
    kafkaProducer.send(new ProducerRecord<>(topic, message));
  }

  @Override
  public boolean addACLToCurrentUser(String name){
    if(listTopics().contains(name)) {
      User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      String user = principal.getUsername();
      ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, name, PatternType.LITERAL);
      AccessControlEntry accessControlEntry = new AccessControlEntry(user, "*", AclOperation.ALL, AclPermissionType.ALLOW);
      adminClient.createAcls(Collections.singletonList(new AclBinding(resourcePattern, accessControlEntry)));
    } else {
      return false;
    }
    return true;
  }
}
