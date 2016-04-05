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
package org.apache.metron.writer;

import backtype.storm.tuple.Tuple;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.metron.Constants;
import org.apache.metron.domain.Configurations;
import org.apache.metron.writer.interfaces.MessageWriter;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class KafkaWriter implements MessageWriter<JSONObject>, Serializable {

  private String brokerUrl;
  private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private int requiredAcks = 1;
  private KafkaProducer kafkaProducer;

  public KafkaWriter(String brokerUrl) {
    this.brokerUrl = brokerUrl;
  }

  public KafkaWriter withKeySerializer(String keySerializer) {
    this.keySerializer = keySerializer;
    return this;
  }

  public KafkaWriter withValueSerializer(String valueSerializer) {
    this.valueSerializer = valueSerializer;
    return this;
  }

  public KafkaWriter withRequiredAcks(int requiredAcks) {
    this.requiredAcks = requiredAcks;
    return this;
  }

  @Override
  public void init() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", brokerUrl);
    producerConfig.put("key.serializer", keySerializer);
    producerConfig.put("value.serializer", valueSerializer);
    producerConfig.put("request.required.acks", requiredAcks);
    this.kafkaProducer = new KafkaProducer<>(producerConfig);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(String sourceType, Configurations configurations, Tuple tuple, JSONObject message) throws Exception {
    kafkaProducer.send(new ProducerRecord<String, String>(Constants.ENRICHMENT_TOPIC, message.toJSONString()));
  }

  @Override
  public void close() throws Exception {
    kafkaProducer.close();
  }
}
