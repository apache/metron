package com.apache.metron.dataservices.kafkaclient.poll;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollingKafkaConsumer implements Runnable 
{
	private static final Logger logger = LoggerFactory.getLogger( PollingKafkaConsumer.class );

	private KafkaStream m_stream;
    private int m_threadNumber;
    private List<String> messages;
    private CountDownLatch latch;
    
    public PollingKafkaConsumer( List<String> messages, KafkaStream a_stream, int a_threadNumber, CountDownLatch latch ) 
    {
        this.m_threadNumber = a_threadNumber;
        this.m_stream = a_stream;
        this.messages = messages;
        this.latch = latch;
    }
 
    public void run() 
    {
		logger.warn( "calling PollingKafkaConsumer.run()" );
		ConsumerIterator<byte[], byte[]> it = m_stream.iterator();
    
		try
		{
			while (it.hasNext())
			{    
				String message = new String(it.next().message());
				logger.warn( "adding message: " + message);
				messages.add(message);
			}
		}
		catch( Exception e)
		{
			logger.error( "Exception waiting on Kafka...", e );
		}
		
		latch.countDown();
		
		logger.warn("Shutting down Thread: " + m_threadNumber);
    }
}