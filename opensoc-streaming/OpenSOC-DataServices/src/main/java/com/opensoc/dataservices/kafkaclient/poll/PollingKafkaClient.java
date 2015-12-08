package com.opensoc.dataservices.kafkaclient.poll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingKafkaClient 
{
	private static final Logger logger = LoggerFactory.getLogger( PollingKafkaClient.class );
	
	private final ConsumerConnector consumer;
    private final String topic;
    private  ExecutorService executor;	
       
    public PollingKafkaClient(String zooKeeper, String groupId, String topic) 
    {
        this.consumer = kafka.consumer.Consumer.createJavaConsumerConnector(
                createConsumerConfig(zooKeeper, groupId ));
        
        this.topic = topic;
	}

	private static ConsumerConfig createConsumerConfig(String a_zookeeper, String a_groupId) {
        Properties props = new Properties();
        props.put("zookeeper.connect", a_zookeeper);
        props.put("group.id", a_groupId);
        props.put( "zookeeper.session.timeout.ms", "1000");
        props.put( "zookeeper.sync.time.ms", "200");
        props.put( "auto.commit.interval.ms", "200");
        props.put( "auto.offset.reset", "smallest");
        // props.put( "fetch.min.bytes",  "1" );
        props.put( "consumer.timeout.ms", "1000" );
        
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

    } 
    
    
    public List fetchMessages()
    {
    	List<String> messages = new ArrayList<String>();
    	
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(1));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
 
        logger.debug( "streams.size = " + streams.size() );
        
        // now launch all the threads
        //
        executor = Executors.newFixedThreadPool(1);
        
        // now create an object to consume the messages
        //
        int threadNumber = 0;
        CountDownLatch latch = new CountDownLatch( streams.size() );
        
        for (final KafkaStream stream : streams) {
            executor.submit(new PollingKafkaConsumer(messages, stream, threadNumber, latch ));
            threadNumber++;
        }    	
    	
        try {
        	latch.await();
        } 
        catch (InterruptedException e) {
        	// TODO: handle
        }
        
    	return messages;
    }
}