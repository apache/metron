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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import kafka.admin.AclCommand;
import kafka.admin.AdminOperationException;
import kafka.admin.AdminUtils$;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
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

  private final ZkUtils zkUtils;
  private final ConsumerFactory<String, String> kafkaConsumerFactory;
  private final KafkaProducer<String, String> kafkaProducer;
  private final AdminUtils$ adminUtils;

  @Autowired
  private Environment environment;

  /**
   * @param zkUtils              A utility class used to interact with ZooKeeper.
   * @param kafkaConsumerFactory A class used to create {@link KafkaConsumer} in order to interact with Kafka.
   * @param kafkaProducer        A class used to produce messages to Kafka.
   * @param adminUtils           A utility class used to do administration operations on Kafka.
   */
  @Autowired
  public KafkaServiceImpl(final ZkUtils zkUtils,
                          final ConsumerFactory<String, String> kafkaConsumerFactory,
                          final KafkaProducer<String, String> kafkaProducer,
                          final AdminUtils$ adminUtils) {
    this.zkUtils = zkUtils;
    this.kafkaConsumerFactory = kafkaConsumerFactory;
    this.kafkaProducer = kafkaProducer;
    this.adminUtils = adminUtils;
  }

  @Override
  public KafkaTopic createTopic(final KafkaTopic topic) throws RestException {
    if (!listTopics().contains(topic.getName())) {
      try {
        adminUtils.createTopic(zkUtils, topic.getName(), topic.getNumPartitions(), topic.getReplicationFactor(), topic.getProperties(), RackAwareMode.Disabled$.MODULE$);
        if (environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false)){
          addACLToCurrentUser(topic.getName());
        }
      } catch (AdminOperationException e) {
        throw new RestException(e);
      }
    }
    return topic;
  }

  @Override
  public boolean deleteTopic(final String name) {
    final Set<String> topics = listTopics();
    if (topics != null && topics.contains(name)) {
      adminUtils.deleteTopic(zkUtils, name);
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

        final ConsumerRecords<String, String> records = kafkaConsumer.poll(KAFKA_CONSUMER_TIMEOUT);
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
      String zkServers = environment.getProperty(MetronRestConstants.ZK_URL_SPRING_PROPERTY);
      User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      String user = principal.getUsername();
      List<String> cmd = new ArrayList<>();
      cmd.add("--add");
      cmd.add("--allow-principal");
      cmd.add("User:" + user);
      cmd.add("--topic");
      cmd.add(name);
      cmd.add("--authorizer-properties");
      cmd.add("zookeeper.connect=" + String.join(",", zkServers));
      AclCommand.main(cmd.toArray(new String[cmd.size()]));
    } else {
      return false;
    }
    return true;
  }
}
