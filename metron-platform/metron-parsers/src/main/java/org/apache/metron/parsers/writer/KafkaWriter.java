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
package org.apache.metron.parsers.writer;

import backtype.storm.tuple.Tuple;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.Configurations;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.MessageWriter;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.StringUtils;
import org.apache.metron.common.writer.AbstractWriter;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KafkaWriter extends AbstractWriter implements MessageWriter<JSONObject>, Serializable {
  public enum Configurations {
     BROKER("kafka.brokerUrl")
    ,KEY_SERIALIZER("kafka.keySerializer")
    ,VALUE_SERIALIZER("kafka.valueSerializer")
    ,REQUIRED_ACKS("kafka.requiredAcks")
    ,TOPIC("kafka.topic");
    ;
    String key;
    Configurations(String key) {
      this.key = key;
    }
    public Object get(Optional<String> configPrefix, Map<String, Object> config) {
      return config.get(StringUtils.join(".", configPrefix, Optional.of(key)));
    }
    public <T> T getAndConvert(Optional<String> configPrefix, Map<String, Object> config, Class<T> clazz) {
      Object o = get(configPrefix, config);
      if(o != null) {
        return ConversionUtils.convert(o, clazz);
      }
      return null;
    }
  }
  private String brokerUrl;
  private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private int requiredAcks = 1;
  private String kafkaTopic = Constants.ENRICHMENT_TOPIC;
  private KafkaProducer kafkaProducer;
  private String configPrefix = null;

  public KafkaWriter() {}

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

  public KafkaWriter withRequiredAcks(Integer requiredAcks) {
    this.requiredAcks = requiredAcks;
    return this;
  }

  public KafkaWriter withTopic(String topic) {
    this.kafkaTopic= topic;
    return this;
  }
  public KafkaWriter withConfigPrefix(String prefix) {
    this.configPrefix = prefix;
    return this;
  }

  public Optional<String> getConfigPrefix() {
    return Optional.ofNullable(configPrefix);
  }
  @Override
  public void configure(String sensorName, WriterConfiguration configuration) {
    Map<String, Object> configMap = configuration.getSensorConfig(sensorName);
    String brokerUrl = Configurations.BROKER.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(brokerUrl != null) {
      this.brokerUrl = brokerUrl;
    }
    String keySerializer = Configurations.KEY_SERIALIZER.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(keySerializer != null) {
      withKeySerializer(keySerializer);
    }
    String valueSerializer = Configurations.VALUE_SERIALIZER.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(valueSerializer != null) {
      withValueSerializer(keySerializer);
    }
    Integer requiredAcks = Configurations.REQUIRED_ACKS.getAndConvert(getConfigPrefix(), configMap, Integer.class);
    if(requiredAcks!= null) {
      withRequiredAcks(requiredAcks);
    }
    String topic = Configurations.TOPIC.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(topic != null) {
      withTopic(topic);
    }
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
  public void write(String sourceType, WriterConfiguration configurations, Tuple tuple, JSONObject message) throws Exception {
    kafkaProducer.send(new ProducerRecord<String, String>(kafkaTopic, message.toJSONString()));
  }

  @Override
  public void close() throws Exception {
    kafkaProducer.close();
  }
}
