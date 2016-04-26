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
package org.apache.metron.test.utils;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class KafkaLoader {

  private String brokerUrl;
  private String topic;
  private String samplePath;
  private int delay = 1000;
  private int iterations = -1;
  private KafkaProducer kafkaProducer;

  public KafkaLoader(String brokerUrl, String topic, String samplePath) {
    this.brokerUrl = brokerUrl;
    this.topic = topic;
    this.samplePath = samplePath;
  }

  public KafkaLoader withDelay(int delay) {
    this.delay = delay;
    return this;
  }

  public KafkaLoader withIterations(int iterations) {
    this.iterations = iterations;
    return this;
  }

  public void start() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", brokerUrl);
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaProducer = new KafkaProducer<>(producerConfig);
    try {
      while (iterations == -1 || iterations-- > 0) {
        BufferedReader reader = new BufferedReader(new FileReader(samplePath));
        String line;
        while((line = reader.readLine()) != null) {
          kafkaProducer.send(new ProducerRecord<String, String>(topic, line));
          Thread.sleep(delay);
        }
        reader.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    kafkaProducer.close();
  }


  public static void main(String[] args) {
    KafkaLoader kafkaLoader = new KafkaLoader(args[0], args[1], args[2]);
    if (args.length > 3) kafkaLoader.withDelay(Integer.parseInt(args[3]));
    if (args.length > 4) kafkaLoader.withIterations(Integer.parseInt(args[4]));
    kafkaLoader.start();
    kafkaLoader.stop();
  }
}
