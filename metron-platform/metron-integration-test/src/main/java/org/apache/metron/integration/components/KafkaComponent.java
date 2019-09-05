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
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.common.TopicExistsException;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.MockTime;
import kafka.utils.TestUtils;
import kafka.utils.Time;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.wrapper.AdminUtilsWrapper;
import org.apache.metron.integration.wrapper.TestUtilsWrapper;
import org.apache.metron.test.utils.UnitTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KafkaComponent implements InMemoryComponent {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final long KAFKA_PROPAGATE_TIMEOUT_MS = 10000l;
  public static final int ZK_SESSION_TIMEOUT_MS = 30000;
  public static final int ZK_CONNECTION_TIMEOUT_MS = 30000;
  public static final int KAFKA_ZOOKEEPER_TIMEOUT_MS = 1000000;

  public static class Topic {
    public int numPartitions;
    public String name;

    public Topic(String name, int numPartitions) {
      this.numPartitions = numPartitions;
      this.name = name;
    }
  }

  private List<KafkaProducer> producersCreated = new ArrayList<>();
  private transient KafkaServer kafkaServer;
  private transient ZkClient zkClient;
  private transient ConsumerConnector consumer;
  private String zookeeperConnectString;
  private Properties topologyProperties;

  private int brokerPort = 6667;
  private List<Topic> topics = Collections.emptyList();
  private Function<KafkaComponent, Void> postStartCallback;

  public KafkaComponent withPostStartCallback(Function<KafkaComponent, Void> f) {
    postStartCallback = f;
    return this;
  }

  public KafkaComponent withExistingZookeeper(String zookeeperConnectString) {
    this.zookeeperConnectString = zookeeperConnectString;
    return this;
  }

  public KafkaComponent withTopologyProperties(Properties properties){
    this.topologyProperties = properties;
    return this;
  }
  public KafkaComponent withBrokerPort(int brokerPort) {
    if(brokerPort <= 0)
    {
      brokerPort = TestUtils.RandomPort();
    }

    this.brokerPort = brokerPort;
    return this;
  }

  public KafkaComponent withTopics(List<Topic> topics) {
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
    KafkaProducer<K, V> ret = new KafkaProducer<>(producerConfig);
    producersCreated.add(ret);
    return ret;
  }

  @Override
  public void start() {
    // setup Zookeeper
    zookeeperConnectString = topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY);

    zkClient = new ZkClient(zookeeperConnectString, ZK_SESSION_TIMEOUT_MS, ZK_CONNECTION_TIMEOUT_MS, ZKStringSerializer$.MODULE$);

    // setup Broker
    Properties props = TestUtilsWrapper.createBrokerConfig(0, zookeeperConnectString, brokerPort);
    props.setProperty("zookeeper.connection.timeout.ms", Integer.toString(KAFKA_ZOOKEEPER_TIMEOUT_MS));
    KafkaConfig config = new KafkaConfig(props);
    Time mock = new MockTime();
    kafkaServer = TestUtils.createServer(config, mock);

    org.apache.log4j.Level oldLevel = UnitTestHelper.getLog4jLevel(KafkaServer.class);
    UnitTestHelper.setLog4jLevel(KafkaServer.class, org.apache.log4j.Level.OFF);
    // do not proceed until the broker is up
    TestUtilsWrapper.waitUntilBrokerIsRunning(kafkaServer,"Timed out waiting for RunningAsBroker State",100000);

    for(Topic topic : getTopics()) {
      try {
        createTopic(topic.name, topic.numPartitions, KAFKA_PROPAGATE_TIMEOUT_MS);
      } catch (InterruptedException e) {
        throw new RuntimeException("Unable to create topic", e);
      }
    }
    UnitTestHelper.setLog4jLevel(KafkaServer.class, oldLevel);
    if(postStartCallback != null) {
      postStartCallback.apply(this);
    }
  }

  public String getZookeeperConnect() {
    return zookeeperConnectString;
  }

  @Override
  public void stop() {
    shutdownConsumer();
    shutdownProducers();

    if(kafkaServer != null) {
      try {
        kafkaServer.shutdown();
        kafkaServer.awaitShutdown();
      }
      catch(Throwable fnf) {
        if(!fnf.getMessage().contains("Error writing to highwatermark file")) {
          throw fnf;
        }
      }
    }
    if(zkClient != null) {
      // Delete data in ZK to avoid startup interference.
      for(Topic topic : topics) {
        zkClient.deleteRecursive(ZkUtils.getTopicPath(topic.name));
      }

      zkClient.deleteRecursive(ZkUtils.BrokerIdsPath());
      zkClient.deleteRecursive(ZkUtils.BrokerTopicsPath());
      zkClient.deleteRecursive(ZkUtils.ConsumersPath());
      zkClient.deleteRecursive(ZkUtils.ControllerPath());
      zkClient.deleteRecursive(ZkUtils.ControllerEpochPath());
      zkClient.deleteRecursive(ZkUtils.ReassignPartitionsPath());
      zkClient.deleteRecursive(ZkUtils.DeleteTopicsPath());
      zkClient.deleteRecursive(ZkUtils.PreferredReplicaLeaderElectionPath());
      zkClient.deleteRecursive(ZkUtils.BrokerSequenceIdPath());
      zkClient.deleteRecursive(ZkUtils.IsrChangeNotificationPath());
      zkClient.deleteRecursive(ZkUtils.EntityConfigPath());
      zkClient.deleteRecursive(ZkUtils.EntityConfigChangesPath());
      zkClient.close();
    }
  }

  @Override
  public void reset() {
    // Unfortunately, there's no clean way to (quickly) purge or delete a topic.
    // At least without killing and restarting broker anyway.
    stop();
    start();
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
    Properties consumerProperties = TestUtils.createConsumerProperties(zookeeperConnectString, group, consumerName, -1);
    consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
    Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    topicCountMap.put(topic, 1);
    Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
    KafkaStream<byte[], byte[]> stream = consumerMap.get(topic).get(0);
    ConsumerIterator<byte[], byte[]> iterator = stream.iterator();
    return iterator;
  }

  public void shutdownConsumer() {
    if(consumer != null) {
      consumer.shutdown();
    }
  }

  public void shutdownProducers() {
    for(KafkaProducer kp : producersCreated) {
      try {
        kp.close();
      }
      catch(Exception ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }
  }

  public void createTopic(String name) throws InterruptedException {
    createTopic(name, 1, KAFKA_PROPAGATE_TIMEOUT_MS);
  }

  public void waitUntilMetadataIsPropagated(String topic, int numPartitions, long timeOutMS) {
    List<KafkaServer> servers = new ArrayList<>();
    servers.add(kafkaServer);
    for(int part = 0;part < numPartitions;++part) {
      TestUtils.waitUntilMetadataIsPropagated(scala.collection.JavaConversions.asScalaBuffer(servers), topic, part, timeOutMS);
    }
  }

  public void createTopic(String name, int numPartitions, long waitThisLongForMetadataToPropagate) throws InterruptedException {
    ZkUtils zkUtils = null;
    Level oldLevel = UnitTestHelper.getJavaLoggingLevel();
    try {
      UnitTestHelper.setJavaLoggingLevel(Level.OFF);
      zkUtils = ZkUtils.apply(zookeeperConnectString, 30000, 30000, false);
      AdminUtilsWrapper.createTopic(zkUtils, name, numPartitions, 1, new Properties());
      if (waitThisLongForMetadataToPropagate > 0) {
        waitUntilMetadataIsPropagated(name, numPartitions, waitThisLongForMetadataToPropagate);
      }
    }catch(TopicExistsException tee) {
    }finally {
      if(zkUtils != null){
        zkUtils.close();
      }
      UnitTestHelper.setJavaLoggingLevel(oldLevel);
    }
  }

  /**
   * Write a collection of messages to a Kafka topic.
   *
   * @param topic The name of the Kafka topic.
   * @param messages The collection of messages to write.
   */
  public void writeMessages(String topic, Collection<byte[]> messages) {
    try(KafkaProducer<String, byte[]> kafkaProducer = createProducer()) {
      for (byte[] message : messages) {
        kafkaProducer.send(new ProducerRecord<>(topic, message));
      }
    }
  }

  /**
   * Write messages to a Kafka topic.
   *
   * @param topic The name of the Kafka topic.
   * @param messages The messages to write.
   */
  public void writeMessages(String topic, String ...messages) {

    // convert each message to raw bytes
    List<byte[]> messagesAsBytes = Stream.of(messages)
            .map(Bytes::toBytes)
            .collect(Collectors.toList());

    writeMessages(topic, messagesAsBytes);
  }

  /**
   * Write messages to a Kafka topic.
   *
   * @param topic The name of the Kafka topic.
   * @param messages The messages to write.
   */
  public void writeMessages(String topic, List<String> messages) {

    writeMessages(topic, messages.toArray(new String[] {}));
  }
}
