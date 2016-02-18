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
package org.apache.metron.dataservices.kafkaclient;

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
