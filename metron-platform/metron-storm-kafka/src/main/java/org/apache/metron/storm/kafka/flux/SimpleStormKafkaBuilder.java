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

package org.apache.metron.storm.kafka.flux;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.storm.kafka.spout.*;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.OutputFieldsGetter;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This is a convenience layer on top of the KafkaSpoutConfig.Builder available in storm-kafka-client.
 * The justification for this class is two-fold.  First, there are a lot of moving parts and a simplified
 * approach to constructing spouts is useful.  Secondly, and perhaps more importantly, the Builder pattern
 * is decidedly unfriendly to use inside of Flux.  Finally, we can make things a bit more friendly by only requiring
 * zookeeper and automatically figuring out the brokers for the bootstrap server.
 *
 * @param <K> The kafka key type
 * @param <V> The kafka value type
 */
public class SimpleStormKafkaBuilder<K, V> extends KafkaSpoutConfig.Builder<K, V> {

  /**
   * The fields exposed by the kafka consumer.  These will show up in the Storm tuple.
   */
  public enum FieldsConfiguration {
    KEY("key", record -> record.key()),
    VALUE("value", record -> record.value()),
    PARTITION("partition", record -> record.partition()),
    OFFSET("offset", record -> record.offset()),
    TOPIC("topic", record -> record.topic()),
    TIMESTAMP("timestamp", record -> record.timestamp())
    ;
    String fieldName;
    Function<ConsumerRecord,Object> recordExtractor;

    FieldsConfiguration(String fieldName, Function<ConsumerRecord,Object> recordExtractor) {
      this.recordExtractor = recordExtractor;
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }

    /**
     * Return a list of the enums
     * @param configs
     * @return ret a list of enums
     */
    public static List<FieldsConfiguration> toList(String... configs) {
      List<FieldsConfiguration> ret = new ArrayList<>();
      for(String config : configs) {
        ret.add(FieldsConfiguration.valueOf(config.toUpperCase()));
      }
      return ret;
    }

    /**
     * Return a list of the enums from their string representation.
     * @param configs
     * @return ret a list of enums from string representation
     */
    public static List<FieldsConfiguration> toList(List<String> configs) {
      List<FieldsConfiguration> ret = new ArrayList<>();
      for(String config : configs) {
        ret.add(FieldsConfiguration.valueOf(config.toUpperCase()));
      }
      return ret;
    }

    /**
     * Construct a Fields object from an iterable of enums.  These fields are the fields
     * exposed in the Storm tuple emitted from the spout.
     * @param configs
     * @return Fields object from enums iterable
     */
    public static Fields getFields(Iterable<FieldsConfiguration> configs) {
      List<String> fields = new ArrayList<>();
      for(FieldsConfiguration config : configs) {
        fields.add(config.fieldName);
      }
      return new Fields(fields);
    }
  }

  /**
   * Build a tuple given the fields and the topic.  We want to use our FieldsConfiguration enum
   * to define what this tuple looks like.
   * @param <K> The key type in kafka
   * @param <V> The value type in kafka
   */
  public static class SpoutRecordTranslator<K, V> implements RecordTranslator<K,V> {
    private List<FieldsConfiguration> configurations;
    private Fields fields;
    private SpoutRecordTranslator(List<FieldsConfiguration> configurations) {
      this.configurations = configurations;
      this.fields = FieldsConfiguration.getFields(configurations);
    }

    /**
     * Builds a list of tuples using the ConsumerRecord specified as parameter
     *
     * @param consumerRecord whose contents are used to build tuples
     * @return list of tuples
     */
    @Override
    public List<Object> apply(ConsumerRecord<K, V> consumerRecord) {
      Values ret = new Values();
      for(FieldsConfiguration config : configurations) {
        ret.add(config.recordExtractor.apply(consumerRecord));
      }
      return ret;
    }

    @Override
    public Fields getFieldsFor(String s) {
      return fields;
    }

    @Override
    public List<String> streams() {
      return DEFAULT_STREAM;
    }
  }

  public static String DEFAULT_DESERIALIZER = ByteArrayDeserializer.class.getName();


  /**
   * Create an object with the specified properties.  This will expose fields "key" and "value."
   * @param kafkaProps The special kafka properties
   * @param topic The kafka topic.
   * @param zkQuorum The zookeeper quorum.  We will use this to pull the brokers from this.
   */
  public SimpleStormKafkaBuilder( Map<String, Object> kafkaProps
                                , String topic
                                , String zkQuorum
                                )
  {
    this(kafkaProps, topic, zkQuorum, Arrays.asList("key", "value"));
  }

  /**
   * Create an object with the specified properties and exposing the specified fields.
   * @param kafkaProps The special kafka properties
   * @param topic The kafka topic. TODO: In the future, support multiple topics and regex patterns.
   * @param zkQuorum The zookeeper quorum.  We will use this to pull the brokers from this.
   * @param fieldsConfiguration The fields to expose in the storm tuple emitted.
   */
  public SimpleStormKafkaBuilder( Map<String, Object> kafkaProps
                                , String topic
                                , String zkQuorum
                                , List<String> fieldsConfiguration
                                )
  {
    this(kafkaProps, toSubscription(topic), zkQuorum, fieldsConfiguration);
  }

  /**
   * Create an object with the specified properties and exposing the specified fields.
   * @param kafkaProps The special kafka properties
   * @param subscription The subscription to the kafka topic(s)
   * @param zkQuorum The zookeeper quorum.  We will use this to pull the brokers from this.
   * @param fieldsConfiguration The fields to expose in the storm tuple emitted.
   */
  public SimpleStormKafkaBuilder( Map<String, Object> kafkaProps
                                , Subscription subscription
                                , String zkQuorum
                                , List<String> fieldsConfiguration
                                )
  {
    super( getBootstrapServers(zkQuorum, kafkaProps)
         , createDeserializer(Optional.ofNullable((String)kafkaProps.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)), DEFAULT_DESERIALIZER)
         , createDeserializer(Optional.ofNullable((String)kafkaProps.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG)), DEFAULT_DESERIALIZER)
         , subscription
    );

    kafkaProps = KafkaUtils.INSTANCE.normalizeProtocol(kafkaProps);
    setProp(kafkaProps);
    setRecordTranslator(new SpoutRecordTranslator<>(FieldsConfiguration.toList(fieldsConfiguration)));
  }


  private static Subscription toSubscription(String topicOrSubscription) {
    if (StringUtils.isEmpty(topicOrSubscription)) {
      throw new IllegalArgumentException("Topic name is invalid (empty or null): " + topicOrSubscription);
    }
    int length = topicOrSubscription.length();
    if(topicOrSubscription.charAt(0) == '/' && topicOrSubscription.charAt(length - 1) == '/') {
      //pattern, so strip off the preceding and ending slashes
      String substr = topicOrSubscription.substring(1, length - 1);
      return new PatternSubscription(Pattern.compile(substr));
    }
    else {
      return new NamedSubscription(topicOrSubscription);
    }
  }


  private static <T> Class<Deserializer<T>> createDeserializer( Optional<String> deserializerClass
                                                , String defaultDeserializerClass
                                                )
  {
    try {
      return (Class<Deserializer<T>>) Class.forName(deserializerClass.orElse(defaultDeserializerClass));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create a deserializer: " + deserializerClass.orElse(defaultDeserializerClass) + ": " + e.getMessage(), e);
    }
  }

  private static String getBootstrapServers(String zkQuorum, Map<String, Object> kafkaProps) {
    String brokers = (String)kafkaProps.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
    if(brokers == null) {
      try {
        return Joiner.on(",").join(KafkaUtils.INSTANCE.getBrokersFromZookeeper(zkQuorum));
      } catch (Exception e) {
        throw new IllegalStateException("Unable to find the bootstrap servers: " + e.getMessage(), e);
      }
    }
    return brokers;
  }

  /**
   * Create a StormKafkaSpout from a given topic, zookeeper quorum and fields.  Also, configure the spout
   * using a Map that configures both kafka as well as the spout (see the properties in SpoutConfiguration).
   * @param topic
   * @param zkQuorum
   * @param fieldsConfiguration
   * @param kafkaProps  The aforementioned map.
   * @return StormKafkaSpout from a given topic, zk quorum and fields
   */
  public static <K, V> StormKafkaSpout<K, V> create( String topic
                                                   , String zkQuorum
                                                   , List<String> fieldsConfiguration
                                                   , Map<String, Object> kafkaProps
  )
  {
    Map<String, Object> spoutConfig = SpoutConfiguration.separate(kafkaProps);

    SimpleStormKafkaBuilder<K, V> builder = new SimpleStormKafkaBuilder<>(kafkaProps, topic, zkQuorum, fieldsConfiguration);
    SpoutConfiguration.configure(builder, spoutConfig);
    return new StormKafkaSpout<>(builder);
  }

}
