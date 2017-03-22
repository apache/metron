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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.storm.kafka.spout.*;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.OutputFieldsGetter;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SimpleStormKafkaBuilder<K, V> extends KafkaSpoutConfig.Builder<K, V> {
  final static String STREAM = "default";

  public enum FieldsConfiguration {
    KEY("key", record -> record.key()),
    VALUE("value", record -> record.value()),
    PARTITION("partition", record -> record.partition()),
    TOPIC("topic", record -> record.topic())
    ;
    String fieldName;
    Function<ConsumerRecord,Object> recordExtractor;

    FieldsConfiguration(String fieldName, Function<ConsumerRecord,Object> recordExtractor) {
      this.recordExtractor = recordExtractor;
      this.fieldName = fieldName;
    }

    public static List<FieldsConfiguration> toList(String... configs) {
      List<FieldsConfiguration> ret = new ArrayList<>();
      for(String config : configs) {
        ret.add(FieldsConfiguration.valueOf(config.toUpperCase()));
      }
      return ret;
    }

    public static List<FieldsConfiguration> toList(List<String> configs) {
      List<FieldsConfiguration> ret = new ArrayList<>();
      for(String config : configs) {
        ret.add(FieldsConfiguration.valueOf(config.toUpperCase()));
      }
      return ret;
    }

    public static Fields getFields(Iterable<FieldsConfiguration> configs) {
      List<String> fields = new ArrayList<>();
      for(FieldsConfiguration config : configs) {
        fields.add(config.fieldName);
      }
      return new Fields(fields);
    }
  }

  public static class TupleBuilder<K, V> extends KafkaSpoutTupleBuilder<K,V> {
    private List<FieldsConfiguration> configurations;
    private TupleBuilder(String topic, List<FieldsConfiguration> configurations) {
      super(topic);
      this.configurations = configurations;
    }

    /**
     * Builds a list of tuples using the ConsumerRecord specified as parameter
     *
     * @param consumerRecord whose contents are used to build tuples
     * @return list of tuples
     */
    @Override
    public List<Object> buildTuple(ConsumerRecord<K, V> consumerRecord) {
      Values ret = new Values();
      for(FieldsConfiguration config : configurations) {
        ret.add(config.recordExtractor.apply(consumerRecord));
      }
      return ret;
    }
  }

  private String topic;

  /**
   *
   * @param kafkaProps
   * @param topic
   */
  public SimpleStormKafkaBuilder( Map<String, Object> kafkaProps
                                , String topic
                                , String zkQuorum
                                )
  {
    this(kafkaProps, topic, zkQuorum, Arrays.asList("key", "value"));
  }

  /**
   *
   * @param kafkaProps
   * @param topic
   */
  public SimpleStormKafkaBuilder( Map<String, Object> kafkaProps
                                , String topic
                                , String zkQuorum
                                , List<String> fieldsConfiguration
                                )
  {
    super( modifyKafkaProps(kafkaProps, zkQuorum)
         , createStreams(fieldsConfiguration, topic)
         , createTuplesBuilder(fieldsConfiguration, topic)
         );
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }

  public static StormKafkaSpout create( String topic
                                      , String zkQuorum
                                      , List<String> fieldsConfiguration
                                      , Map<String, Object> kafkaProps
                                      )
  {
    Map<String, Object> spoutConfig = SpoutConfiguration.separate(kafkaProps);

    SimpleStormKafkaBuilder builder = new SimpleStormKafkaBuilder(kafkaProps, topic, zkQuorum, fieldsConfiguration);
    SpoutConfiguration.configure(builder, spoutConfig);
    return new StormKafkaSpout(builder);
  }

  private static Map<String, Object> modifyKafkaProps(Map<String, Object> props, String zkQuorum) {
    try {
      List<String> brokers = KafkaUtils.INSTANCE.getBrokersFromZookeeper(zkQuorum);
      props.put(KafkaSpoutConfig.Consumer.BOOTSTRAP_SERVERS, Joiner.on(",").join(brokers));
      if(!props.containsKey(KafkaSpoutConfig.Consumer.KEY_DESERIALIZER)) {
        props.put(KafkaSpoutConfig.Consumer.KEY_DESERIALIZER, ByteArrayDeserializer.class.getName());
      }
      if(!props.containsKey(KafkaSpoutConfig.Consumer.VALUE_DESERIALIZER)) {
        props.put(KafkaSpoutConfig.Consumer.VALUE_DESERIALIZER, ByteArrayDeserializer.class.getName());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to retrieve brokers from zookeeper: " + e.getMessage(), e);
    }
    return props;
  }

  private static <K,V> KafkaSpoutTuplesBuilder<K, V> createTuplesBuilder(List<String> config, String topic) {
    TupleBuilder<K, V> tb =  new TupleBuilder<K, V>(topic, FieldsConfiguration.toList(config));
    return new KafkaSpoutTuplesBuilderNamedTopics.Builder<>(tb).build();
  }


  private static KafkaSpoutStreams createStreams(List<String> config, String topic) {
    final Fields fields = FieldsConfiguration.getFields(FieldsConfiguration.toList(config));
    return new KafkaSpoutStreamsNamedTopics.Builder(fields, STREAM, new String[] { topic} ).build();
  }

}
