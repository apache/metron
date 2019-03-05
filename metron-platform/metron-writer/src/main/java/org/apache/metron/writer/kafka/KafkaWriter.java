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

import com.google.common.base.Joiner;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.common.utils.StringUtils;
import org.apache.metron.common.writer.BulkMessageWriter;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.common.writer.MessageId;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.writer.AbstractWriter;
import org.apache.storm.task.TopologyContext;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaWriter extends AbstractWriter implements BulkMessageWriter<JSONObject>, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public enum Configurations {
     BROKER("kafka.brokerUrl")
    ,KEY_SERIALIZER("kafka.keySerializer")
    ,ZK_QUORUM("kafka.zkQuorum")
    ,VALUE_SERIALIZER("kafka.valueSerializer")
    ,REQUIRED_ACKS("kafka.requiredAcks")
    ,TOPIC("kafka.topic")
    ,TOPIC_FIELD("kafka.topicField")
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

  /**
   * Default batch size in bytes. Note, we don't want to expose this to clients. End users should
   * set the writer batchSize setting via the BulkWriterComponent.
   *
   * @see ProducerConfig#BATCH_SIZE_DOC
   * @see <a href="https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.4/bk_kafka-component-guide/content/kafka-producer-settings.html">https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.6.4/bk_kafka-component-guide/content/kafka-producer-settings.html</a>
   */
  private static final int DEFAULT_BATCH_SIZE = 1_024 * 64; // 64 kilobytes
  private String brokerUrl;
  private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";
  private int requiredAcks = 1;
  private String kafkaTopic = Constants.ENRICHMENT_TOPIC;
  private String kafkaTopicField = null;
  private KafkaProducer kafkaProducer;
  private String configPrefix = null;
  private String zkQuorum = null;
  private Map<String, Object> producerConfigs = new HashMap<>();

  public KafkaWriter() {}

  public KafkaWriter(String brokerUrl) {
    this.brokerUrl = brokerUrl;
  }

  public KafkaWriter withBrokerUrl(String brokerUrl) {
    this.brokerUrl = brokerUrl;
    return this;
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

  public KafkaWriter withTopicField(String topicField) {
    this.kafkaTopicField = topicField;
    return this;
  }

  public KafkaWriter withConfigPrefix(String prefix) {
    this.configPrefix = prefix;
    return this;
  }

  public KafkaWriter withProducerConfigs(Map<String, Object> extraConfigs) {
    if(producerConfigs == null) {
      this.producerConfigs = extraConfigs;
    }
    else if(extraConfigs != null){
      producerConfigs.putAll(extraConfigs);
    }
    return this;
  }

  public Optional<String> getConfigPrefix() {
    return Optional.ofNullable(configPrefix);
  }

  /**
   * Used only for unit testing
   */
  protected void setKafkaProducer(KafkaProducer kafkaProducer) {
    this.kafkaProducer = kafkaProducer;
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
    String topicField = Configurations.TOPIC_FIELD.getAndConvert(getConfigPrefix(), configMap, String.class);
    if(topicField != null) {
      withTopicField(topicField);
    }
    Map<String, Object> producerConfigs = (Map)Configurations.PRODUCER_CONFIGS.get(getConfigPrefix(), configMap);
    if(producerConfigs != null) {
      withProducerConfigs(producerConfigs);
    }
  }

  @Override
  public void init(Map stormConf, TopologyContext topologyContext, WriterConfiguration config)
      throws Exception {
    if(this.zkQuorum != null && this.brokerUrl == null) {
      try {
        this.brokerUrl = Joiner.on(",").join(KafkaUtils.INSTANCE.getBrokersFromZookeeper(this.zkQuorum));
      } catch (Exception e) {
        throw new IllegalStateException("Cannot read kafka brokers from zookeeper and you didn't specify them, giving up!", e);
      }
    }
    this.kafkaProducer = new KafkaProducer<>(createProducerConfigs());
  }

  public Map<String, Object> createProducerConfigs() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", brokerUrl);
    producerConfig.put("key.serializer", keySerializer);
    producerConfig.put("value.serializer", valueSerializer);
    producerConfig.put("request.required.acks", requiredAcks);
    producerConfig.put(ProducerConfig.BATCH_SIZE_CONFIG, DEFAULT_BATCH_SIZE);
    producerConfig.putAll(producerConfigs == null?new HashMap<>():producerConfigs);
    producerConfig = KafkaUtils.INSTANCE.normalizeProtocol(producerConfig);
    return producerConfig;
  }

  public Optional<String> getKafkaTopic(JSONObject message) {
    String t = null;
    if(kafkaTopicField != null) {
      t = (String)message.get(kafkaTopicField);
      LOG.debug("Sending to topic: {} based on the field {}", t, kafkaTopicField);
    }
    else {
      t = kafkaTopic;
      LOG.debug("Sending to topic: {}", t);
    }
    return Optional.ofNullable(t);
  }

  @Override
  public BulkWriterResponse write(String sensorType, WriterConfiguration configurations,
                                  List<BulkMessage<JSONObject>> messages) {
    BulkWriterResponse writerResponse = new BulkWriterResponse();
    List<Map.Entry<MessageId, Future>> results = new ArrayList<>();
    for (BulkMessage<JSONObject> bulkWriterMessage: messages) {
      MessageId messageId = bulkWriterMessage.getId();
      JSONObject message = bulkWriterMessage.getMessage();
      String jsonMessage;
      try {
         jsonMessage = message.toJSONString();
      } catch (Throwable t) {
        writerResponse.addError(t, messageId);
        continue;
      }
      Optional<String> topic = getKafkaTopic(message);
      if(topic.isPresent()) {
        Future future = kafkaProducer
            .send(new ProducerRecord<String, String>(topic.get(), jsonMessage));
        // we want to manage the batching
        results.add(new AbstractMap.SimpleEntry<>(messageId, future));
      }
      else {
        LOG.debug("Dropping {} because no topic is specified.", jsonMessage);
      }
    }

    Collection<MessageId> ids = messages.stream().map(BulkMessage::getId).collect(Collectors.toList());
    try {
      // ensures all Future.isDone() == true
      kafkaProducer.flush();
    } catch (InterruptException e) {
      writerResponse.addAllErrors(e, ids);
      return writerResponse;
    }

    for (Map.Entry<MessageId, Future> kv : results) {
      try {
        kv.getValue().get();
        writerResponse.addSuccess(kv.getKey());
      } catch (Exception e) {
        writerResponse.addError(e, kv.getKey());
      }
    }
    return writerResponse;
  }

  @Override
  public String getName() {
    return "kafka";
  }

  @Override
  public void close() throws Exception {
    kafkaProducer.close();
  }
}
