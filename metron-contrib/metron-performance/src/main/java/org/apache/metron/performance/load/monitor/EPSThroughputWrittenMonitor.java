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
package org.apache.metron.performance.load.monitor;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.metron.performance.util.KafkaUtil;

import java.util.Map;
import java.util.Optional;

public class EPSThroughputWrittenMonitor extends AbstractMonitor {
  Map<Integer, Long> lastOffsetMap = null;
  KafkaConsumer<String, String> consumer;
  public EPSThroughputWrittenMonitor(Optional<?> kafkaTopic, Map<String, Object> kafkaProps) {
    super(kafkaTopic);
    consumer = new KafkaConsumer<>(kafkaProps);
  }

  private Long writtenSince(Map<Integer, Long> partitionOffsets, Map<Integer, Long> lastOffsetMap) {
    if(partitionOffsets == null) {
      return null;
    }
    long sum = 0;
    for(Map.Entry<Integer, Long> partitionOffset : partitionOffsets.entrySet()) {
      sum += partitionOffset.getValue() - lastOffsetMap.get(partitionOffset.getKey());
    }
    return sum;
  }

  @Override
  protected Long monitor(double deltaTs) {
    Optional<Long> epsWritten = Optional.empty();
    if(kafkaTopic.isPresent()) {
      if(lastOffsetMap != null) {
        Map<Integer, Long> currentOffsets = KafkaUtil.INSTANCE.getKafkaOffsetMap(consumer, (String) kafkaTopic.get());
        Long eventsWrittenSince = writtenSince(currentOffsets, lastOffsetMap);
        if (eventsWrittenSince != null) {
          epsWritten = Optional.of((long) (eventsWrittenSince / deltaTs));
        }
        lastOffsetMap = currentOffsets == null ? lastOffsetMap : currentOffsets;
        if (epsWritten.isPresent()) {
          return epsWritten.get();
        }
      }
      else {
        lastOffsetMap = KafkaUtil.INSTANCE.getKafkaOffsetMap(consumer, (String)kafkaTopic.get());
      }
    }
    return null;
  }

  @Override
  public String format() {
    return "%d eps throughput measured for " + kafkaTopic.get();
  }

  @Override
  public String name() {
    return "throughput measured";
  }

}
