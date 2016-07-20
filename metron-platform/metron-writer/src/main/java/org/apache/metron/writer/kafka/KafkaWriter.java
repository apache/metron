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
package org.apache.metron.writer.kafka;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Joiner;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.interfaces.MessageWriter;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.common.utils.StringUtils;
import org.apache.metron.writer.AbstractWriter;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KafkaWriter extends AbstractWriter implements MessageWriter<JSONObject>, Serializable {
  public enum Configurations {
     BROKER("kafka.brokerUrl")
    ,KEY_SERIALIZER("kafka.keySerializer")
    ,ZK_QUORUM("kafka.zkQuorum")
    ,VALUE_SERIALIZER("kafka.valueSerializer")
    ,REQUIRED_ACKS("kafka.requiredAcks")
    ,TOPIC("kafka.topic")
    ,PRODUCER_CONFIGS("kafka.producerConfigs");
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
  private String zkQuorum = null;
  private Map<String, Object> producerConfigs = new HashMap<>();

  public KafkaWriter() {}

  public KafkaWriter(String brokerUrl) {
    this.brokerUrl = brokerUrl;
  }

  public KafkaWriter withZkQuorum(String zkQuorum) {
    this.zkQuorum = zkQuorum;
    return this;
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

  public KafkaWriter withProducerConfigs(Map<String, Object> extraConfigs) {
    this.producerConfigs = extraConfigs;
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
    String zkQuorum = Configurations.ZK_QUORUM.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(zkQuorum != null) {
      withZkQuorum(zkQuorum);
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
    Map<String, Object> producerConfigs = (Map)Configurations.PRODUCER_CONFIGS.get(getConfigPrefix(), configMap);
    if(producerConfigs != null) {
      withProducerConfigs(producerConfigs);
    }
  }

  public Map<String, Object> createProducerConfigs() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", brokerUrl);
    producerConfig.put("key.serializer", keySerializer);
    producerConfig.put("value.serializer", valueSerializer);
    producerConfig.put("request.required.acks", requiredAcks);
    producerConfig.putAll(producerConfigs == null?new HashMap<>():producerConfigs);
    return producerConfig;
  }

  @Override
  public void init() {
    if(this.zkQuorum != null && this.brokerUrl == null) {
      try {
        this.brokerUrl = Joiner.on(",").join(KafkaUtils.INSTANCE.getBrokersFromZookeeper(this.zkQuorum));
      } catch (Exception e) {
        throw new IllegalStateException("Cannot read kafka brokers from zookeeper and you didn't specify them, giving up!", e);
      }
    }
    this.kafkaProducer = new KafkaProducer<>(createProducerConfigs());
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
