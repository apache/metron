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
import java.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.TestUtils;
import org.apache.kafka.common.utils.MockTime;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.clients.admin.AdminClient;

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
  private static final int POLL_SECONDS = 1;

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
  private transient AdminClient adminClient;
  private transient KafkaConsumer<byte[], byte[]> consumer;
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

  public AdminClient createAdminClient() {
    Map<String, Object> adminConfig = new HashMap<>();
    adminConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerList());
    AdminClient adminClient = AdminClient.create(adminConfig);
    return adminClient;
  }

  public <K,V> KafkaProducer<K,V> createProducer(Map<String, Object> properties, Class<K> keyClass, Class<V> valueClass)
  {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerList());
    producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
    producerConfig.put(ProducerConfig.ACKS_CONFIG, "-1");
    producerConfig.put("fetch.message.max.bytes", ""+ 1024*1024*10);
    producerConfig.put("replica.fetch.max.bytes", "" + 1024*1024*10);
    producerConfig.put("message.max.bytes", "" + 1024*1024*10);
    producerConfig.put("message.send.max.retries", "10");
    producerConfig.putAll(properties);
    KafkaProducer<K, V> ret = new KafkaProducer<>(producerConfig);
    producersCreated.add(ret);
    return ret;
  }

  public <K,V> KafkaConsumer<K, V> createConsumer(Map<String, Object> properties) {
    Properties consumerConfig = new Properties();
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerList());
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer");
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    consumerConfig.putAll(properties);
    KafkaConsumer consumer = new KafkaConsumer<>(consumerConfig);
    return consumer;
  }

  @Override
  public void start() {
    // setup Zookeeper
    zookeeperConnectString = topologyProperties.getProperty(ZKServerComponent.ZOOKEEPER_PROPERTY);

    zkClient = new ZkClient(zookeeperConnectString, 30000, 30000, ZKStringSerializer$.MODULE$);

    // setup Broker
    Properties props = TestUtilsWrapper.createBrokerConfig(0, zookeeperConnectString, brokerPort);
    props.setProperty("zookeeper.connection.timeout.ms","1000000");
    KafkaConfig config = new KafkaConfig(props);
    Time mock = new MockTime();
    kafkaServer = TestUtils.createServer(config, mock);

    org.apache.log4j.Level oldLevel = UnitTestHelper.getLog4jLevel(KafkaServer.class);
    UnitTestHelper.setLog4jLevel(KafkaServer.class, org.apache.log4j.Level.OFF);
    // do not proceed until the broker is up
    TestUtilsWrapper.waitUntilBrokerIsRunning(kafkaServer,"Timed out waiting for RunningAsBroker State",100000);

    for(Topic topic : getTopics()) {
      try {
        createTopic(topic.name, topic.numPartitions, true);
      } catch (InterruptedException e) {
        throw new RuntimeException("Unable to create topic", e);
      }
    }
    UnitTestHelper.setLog4jLevel(KafkaServer.class, oldLevel);
    if(postStartCallback != null) {
      postStartCallback.apply(this);
    }

    adminClient = createAdminClient();
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
    if (consumer == null) {
      consumer = createConsumer(new HashMap<>());
    }
    consumer.assign(Arrays.asList(new TopicPartition(topic, 0)));
    consumer.seek(new TopicPartition(topic, 0), 0);
    List<byte[]> messages = new ArrayList<>();
    ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(POLL_SECONDS));
    for(ConsumerRecord<byte[], byte[]> record: records) {
      messages.add(record.value());
    }

    return messages;
  }

  public void shutdownConsumer() {
    if(consumer != null) {
      consumer.close();
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
    ZkUtils zkUtils = null;
    Level oldLevel = UnitTestHelper.getJavaLoggingLevel();
    try {
      UnitTestHelper.setJavaLoggingLevel(Level.OFF);
      zkUtils = ZkUtils.apply(zookeeperConnectString, 30000, 30000, false);
      AdminUtilsWrapper.createTopic(zkUtils, name, numPartitions, 1, new Properties());
      if (waitUntilMetadataIsPropagated) {
        waitUntilMetadataIsPropagated(name, numPartitions);
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
