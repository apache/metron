package org.apache.metron.integration.util.integration.components;


import com.google.common.base.Function;
import kafka.Kafka;
import kafka.admin.AdminUtils;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.*;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.apache.metron.integration.util.integration.InMemoryComponent;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class KafkaWithZKComponent implements InMemoryComponent {


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
    private int brokerPort = 6667;
    private List<Topic> topics = Collections.emptyList();
    private Function<KafkaWithZKComponent, Void> postStartCallback;

    public KafkaWithZKComponent withPostStartCallback(Function<KafkaWithZKComponent, Void> f) {
        postStartCallback = f;
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

    public <K,V> Producer<K,V> createProducer( Class<K> keyClass, Class<V> valueClass)
    {
        return createProducer(new Properties(), keyClass, valueClass);
    }
    public <K,V> Producer<K,V> createProducer(Properties properties, Class<K> keyClass, Class<V> valueClass)
    {
        Properties props = properties;
        props.put("metadata.broker.list", getBrokerList());
        props.put("request.required.acks", "-1");
        props.put("fetch.message.max.bytes", ""+ 1024*1024*10);
        props.put("replica.fetch.max.bytes", "" + 1024*1024*10);
        props.put("message.max.bytes", "" + 1024*1024*10);
        props.put("message.send.max.retries", "10");
        return new Producer<>(new ProducerConfig(props));
    }

    @Override
    public void start() {
        // setup Zookeeper
        String zkConnect = TestZKUtils.zookeeperConnect();
        zkServer = new EmbeddedZookeeper(zkConnect);
        zkClient = new ZkClient(zkServer.connectString(), 30000, 30000, ZKStringSerializer$.MODULE$);

        // setup Broker
        Properties props = TestUtils.createBrokerConfig(0, brokerPort, true);
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
        return zkServer.connectString();
    }

    @Override
    public void stop() {
        kafkaServer.shutdown();
        zkClient.close();
        zkServer.shutdown();

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
}

