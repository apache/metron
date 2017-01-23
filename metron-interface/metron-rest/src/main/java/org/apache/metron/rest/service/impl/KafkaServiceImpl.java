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

import kafka.admin.AdminOperationException;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZkUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.KafkaTopic;
import org.apache.metron.rest.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KafkaServiceImpl implements KafkaService {

    @Autowired
    private ZkUtils zkUtils;

    @Autowired
    private KafkaConsumer<String, String> kafkaConsumer;

    private String offsetTopic = "__consumer_offsets";

    public KafkaTopic createTopic(KafkaTopic topic) throws RestException {
        if (!listTopics().contains(topic.getName())) {
          try {
              AdminUtils.createTopic(zkUtils, topic.getName(), topic.getNumPartitions(), topic.getReplicationFactor(), topic.getProperties(), RackAwareMode.Disabled$.MODULE$);
          } catch (AdminOperationException e) {
              throw new RestException(e);
          }
        }
        return topic;
    }

    public boolean deleteTopic(String name) {
        if (listTopics().contains(name)) {
            AdminUtils.deleteTopic(zkUtils, name);
            return true;
        } else {
            return false;
        }
    }

    public KafkaTopic getTopic(String name) {
        KafkaTopic kafkaTopic = null;
        if (listTopics().contains(name)) {
            List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(name);
            if (partitionInfos.size() > 0) {
                PartitionInfo partitionInfo = partitionInfos.get(0);
                kafkaTopic = new KafkaTopic();
                kafkaTopic.setName(name);
                kafkaTopic.setNumPartitions(partitionInfos.size());
                kafkaTopic.setReplicationFactor(partitionInfo.replicas().length);
            }
        }
        return kafkaTopic;
    }

    public Set<String> listTopics() {
        Set<String> topics;
        synchronized (this) {
            topics = kafkaConsumer.listTopics().keySet();
            topics.remove(offsetTopic);
        }
        return topics;
    }

    public String getSampleMessage(String topic) {
        String message = null;
        if (listTopics().contains(topic)) {
            synchronized (this) {
                kafkaConsumer.assign(kafkaConsumer.partitionsFor(topic).stream().map(partitionInfo ->
                        new TopicPartition(topic, partitionInfo.partition())).collect(Collectors.toList()));
                for (TopicPartition topicPartition : kafkaConsumer.assignment()) {
                    long offset = kafkaConsumer.position(topicPartition) - 1;
                    if (offset >= 0) {
                        kafkaConsumer.seek(topicPartition, offset);
                    }
                }
                ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                Iterator<ConsumerRecord<String, String>> iterator = records.iterator();
                if (iterator.hasNext()) {
                    ConsumerRecord<String, String> record = iterator.next();
                    message = record.value();
                }
                kafkaConsumer.unsubscribe();
            }
        }
        return message;
    }

}
