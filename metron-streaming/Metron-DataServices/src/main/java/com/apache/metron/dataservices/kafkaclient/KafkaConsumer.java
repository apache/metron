package com.opensoc.dataservices.kafkaclient;

import java.io.IOException;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumer implements Runnable 
{
	private static final Logger logger = LoggerFactory.getLogger( KafkaConsumer.class );

	private KafkaStream m_stream;
    private int m_threadNumber;
    private RemoteEndpoint remote;
    
    public KafkaConsumer( RemoteEndpoint remote, KafkaStream a_stream, int a_threadNumber) 
    {
        this.m_threadNumber = a_threadNumber;
        this.m_stream = a_stream;
        this.remote = remote;
    }
 
    public void run() 
    {
		logger.debug( "calling ConsumerTest.run()" );
		ConsumerIterator<byte[], byte[]> it = m_stream.iterator();
    
		while (it.hasNext())
		{    
			String message = new String(it.next().message());
			try 
			{
				remote.sendString( message );
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
    	
		logger.debug("Shutting down Thread: " + m_threadNumber);
    }
}
