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
package org.apache.metron.integration.components;


import com.google.common.base.Function;
import kafka.admin.AdminUtils;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.*;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.apache.metron.integration.InMemoryComponent;

import java.nio.ByteBuffer;
import java.util.*;


public class KafkaWithZKComponent implements InMemoryComponent {

  public static final String ZOOKEEPER_PROPERTY = "kafka.zk";

  public static class Topic {
    public int numPartitions;
    public String name;

    public Topic(String name, int numPartitions) {
      this.numPartitions = numPartitions;
      this.name = name;
    }
  }
  private transient KafkaServer kafkaServer;
  private transient EmbeddedZookeeper zkServer;
  private transient ZkClient zkClient;
  private transient ConsumerConnector consumer;
  private String zookeeperConnectString;
  private int brokerPort = 6667;
  private List<Topic> topics = Collections.emptyList();
  private Function<KafkaWithZKComponent, Void> postStartCallback;

  public KafkaWithZKComponent withPostStartCallback(Function<KafkaWithZKComponent, Void> f) {
    postStartCallback = f;
    return this;
  }

  public KafkaWithZKComponent withExistingZookeeper(String zookeeperConnectString) {
    this.zookeeperConnectString = zookeeperConnectString;
    return this;
  }

  public KafkaWithZKComponent withBrokerPort(int brokerPort) {
    if(brokerPort <= 0)
    {
      brokerPort = TestUtils.choosePort();
    }
    this.brokerPort = brokerPort;
    return this;
  }

  public KafkaWithZKComponent withTopics(List<Topic> topics) {
    this.topics = topics;
    return this;
  }

  public List<Topic> getTopics() {
    return topics;
  }

  public int getBrokerPort() {
    return brokerPort;
  }


  public String getBrokerList()  {
    return "localhost:" + brokerPort;
  }

  public <K,V> KafkaProducer<K, V> createProducer(Class<K> keyClass, Class<V> valueClass) {
    return createProducer(new HashMap<>(), keyClass, valueClass);
  }
  public KafkaProducer<String, byte[]> createProducer()
  {
    return createProducer(String.class, byte[].class);
  }

  public <K,V> KafkaProducer<K,V> createProducer(Map<String, Object> properties, Class<K> keyClass, Class<V> valueClass)
  {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put("bootstrap.servers", getBrokerList());
    producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
    producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
    producerConfig.put("request.required.acks", "-1");
    producerConfig.put("fetch.message.max.bytes", ""+ 1024*1024*10);
    producerConfig.put("replica.fetch.max.bytes", "" + 1024*1024*10);
    producerConfig.put("message.max.bytes", "" + 1024*1024*10);
    producerConfig.put("message.send.max.retries", "10");
    producerConfig.putAll(properties);
    return new KafkaProducer<>(producerConfig);
  }

  @Override
  public void start() {
    // setup Zookeeper
    if(zookeeperConnectString == null) {
      String zkConnect = TestZKUtils.zookeeperConnect();
      zkServer = new EmbeddedZookeeper(zkConnect);
      zookeeperConnectString = zkServer.connectString();
    }
    zkClient = new ZkClient(zookeeperConnectString, 30000, 30000, ZKStringSerializer$.MODULE$);

    // setup Broker
    Properties props = TestUtils.createBrokerConfig(0, brokerPort, true);
    props.setProperty("zookeeper.connection.timeout.ms","1000000");
    KafkaConfig config = new KafkaConfig(props);
    Time mock = new MockTime();
    kafkaServer = TestUtils.createServer(config, mock);
    for(Topic topic : getTopics()) {
      try {
        createTopic(topic.name, topic.numPartitions, true);
      } catch (InterruptedException e) {
        throw new RuntimeException("Unable to create topic", e);
      }
    }
    postStartCallback.apply(this);
  }

  public String getZookeeperConnect() {
    return zookeeperConnectString;
  }

  @Override
  public void stop() {
    kafkaServer.shutdown();
    zkClient.close();
    if(zkServer != null) {
      zkServer.shutdown();
    }

  }

  public List<byte[]> readMessages(String topic) {
    SimpleConsumer consumer = new SimpleConsumer("localhost", 6667, 100000, 64 * 1024, "consumer");
    FetchRequest req = new FetchRequestBuilder()
            .clientId("consumer")
            .addFetch(topic, 0, 0, 100000)
            .build();
    FetchResponse fetchResponse = consumer.fetch(req);
    Iterator<MessageAndOffset> results = fetchResponse.messageSet(topic, 0).iterator();
    List<byte[]> messages = new ArrayList<>();
    while(results.hasNext()) {
      ByteBuffer payload = results.next().message().payload();
      byte[] bytes = new byte[payload.limit()];
      payload.get(bytes);
      messages.add(bytes);
    }
    consumer.close();
    return messages;
  }

  public ConsumerIterator<byte[], byte[]> getStreamIterator(String topic) {
    return getStreamIterator(topic, "group0", "consumer0");
  }
  public ConsumerIterator<byte[], byte[]> getStreamIterator(String topic, String group, String consumerName) {
    // setup simple consumer
    Properties consumerProperties = TestUtils.createConsumerProperties(zkServer.connectString(), group, consumerName, -1);
    consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(topic, 1);
    Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
    KafkaStream<byte[], byte[]> stream = consumerMap.get(topic).get(0);
    ConsumerIterator<byte[], byte[]> iterator = stream.iterator();
    return iterator;
  }

  public void shutdownConsumer() {
    consumer.shutdown();
  }

  public void createTopic(String name) throws InterruptedException {
    createTopic(name, 1, true);
  }

  public void waitUntilMetadataIsPropagated(String topic, int numPartitions) {
    List<KafkaServer> servers = new ArrayList<>();
    servers.add(kafkaServer);
    for(int part = 0;part < numPartitions;++part) {
      TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(servers), topic, part, 5000);
    }
  }

  public void createTopic(String name, int numPartitions, boolean waitUntilMetadataIsPropagated) throws InterruptedException {
    AdminUtils.createTopic(zkClient, name, numPartitions, 1, new Properties());
    if(waitUntilMetadataIsPropagated) {
      waitUntilMetadataIsPropagated(name, numPartitions);
    }
  }

  public void writeMessages(String topic, Collection<byte[]> messages) {
    KafkaProducer<String, byte[]> kafkaProducer = createProducer();
    for(byte[] message: messages) {
      kafkaProducer.send(new ProducerRecord<String, byte[]>(topic, message));
    }
    kafkaProducer.close();
  }
}
