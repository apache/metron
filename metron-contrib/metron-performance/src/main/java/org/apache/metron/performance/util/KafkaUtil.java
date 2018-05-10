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
package org.apache.metron.performance.util;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum KafkaUtil {
  INSTANCE;

  public List<TopicPartition> getTopicPartition(KafkaConsumer<String, String> consumer, String topic) {

    List<PartitionInfo> partitions = consumer.partitionsFor(topic);
    List<TopicPartition> ret = new ArrayList<>(partitions.size());
    for(PartitionInfo par : partitions) {
      ret.add(new TopicPartition(topic, par.partition()));
    }
    return ret;
  }

  public Map<Integer, Long> getKafkaOffsetMap(KafkaConsumer<String, String> consumer, String topic ) {
    Map<Integer, Long> ret = new HashMap<>();
    if(!consumer.subscription().contains(topic)) {
      consumer.subscribe(Collections.singletonList(topic));
    }
    consumer.poll(0);
    List<TopicPartition> partitions = getTopicPartition(consumer, topic);
    consumer.seekToEnd(partitions);
    for(TopicPartition par : partitions) {
      ret.put(par.partition(), consumer.position(par)-1);
    }
    return ret;
  }
}
