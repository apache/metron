package org.apache.metron.dataservices.kafkaclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaClient 
{
	private static final Logger logger = LoggerFactory.getLogger( KafkaClient.class );
	
	private final ConsumerConnector consumer;
    private final String topic;
    private  ExecutorService executor;	
	private RemoteEndpoint remote;
       
    public KafkaClient(String zooKeeper, String groupId, String topic, RemoteEndpoint remote) 
    {
        this.consumer = kafka.consumer.Consumer.createJavaConsumerConnector(
                createConsumerConfig(zooKeeper, groupId ));
        
        this.topic = topic;
        this.remote = remote;
	}

	private static ConsumerConfig createConsumerConfig(String a_zookeeper, String a_groupId) {
        Properties props = new Properties();
        props.put("zookeeper.connect", a_zookeeper);
        props.put("group.id", a_groupId);
        props.put("zookeeper.session.timeout.ms", "1000");
        props.put("zookeeper.sync.time.ms", "1000");
        props.put("auto.commit.interval.ms", "1000");
        props.put("auto.offset.reset", "smallest");
        
        return new ConsumerConfig(props);
    }    
    
    public void shutdown() {
    	
    	logger.info( "Client shutdown() method invoked" );
    	
        if (consumer != null) { 
        	consumer.shutdown();
        }
        
        if (executor != null) { 
        	executor.shutdown(); 
        }
    }    
    
    public void run(int a_numThreads) {
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(a_numThreads));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
 
        logger.debug( "streams.size = " + streams.size() );
        
        // now launch all the threads
        //
        executor = Executors.newFixedThreadPool(a_numThreads);

        // now create an object to consume the messages
        //
        int threadNumber = 0;
        for (final KafkaStream stream : streams) {
            executor.submit(new KafkaConsumer(this.remote, stream, threadNumber));
            threadNumber++;
        }
    }   	
}
