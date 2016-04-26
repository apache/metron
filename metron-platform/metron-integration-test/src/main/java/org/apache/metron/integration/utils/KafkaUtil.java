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
package org.apache.metron.integration.utils;


import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Map;

public class KafkaUtil {
    public static <K,V> void send(Producer<K,V> producer, K key, V value, String topic) {
        ProducerRecord<K,V> record = new ProducerRecord<K, V>(topic, key, value);
        producer.send(record);
    }

    public static <K,V> void send(Producer<K,V> producer, Iterable<Map.Entry<K,V>> messages, String topic, long sleepBetween) throws InterruptedException {
        for(Map.Entry<K,V> kv : messages) {
            send(producer, kv.getKey(), kv.getValue(), topic);
            if(sleepBetween > 0) {
                Thread.sleep(sleepBetween);
            }
        }
    }

}
