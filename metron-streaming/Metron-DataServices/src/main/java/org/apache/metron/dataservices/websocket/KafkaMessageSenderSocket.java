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
package org.apache.metron.dataservices.websocket;


import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.dataservices.kafkaclient.KafkaClient;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class KafkaMessageSenderSocket 
{
	private static final Logger logger = LoggerFactory.getLogger( KafkaMessageSenderSocket.class );
	
	// config Properties object for our zooKeeper URL, etc. here 
	private Properties configProps;
	
	boolean authGood = false;
	int threads = 1;
	KafkaClient client;

	
    public KafkaMessageSenderSocket( Properties configProps, final boolean authGood )
	{
    	this.configProps = configProps;
		this.authGood = authGood;  
	}
	
	@OnWebSocketConnect
	public void onConnect( Session session )
	{
		logger.info( "WebSocket connected!" );
	}
	
	@OnWebSocketClose
	public void onClose( Session session, int closeCode, String closeReason )
	{
		logger.info( "WebSocket closed.  Shutting down Kafka client." );
		client.shutdown();
	}

	@OnWebSocketMessage
	public void onMessage( Session session, String text )
	{	
		
		if( !authGood )
		{
			try 
			{
				session.getRemote().sendString( "Must login first" );
			} 
			catch (IOException e) {
				logger.error( "Unexpected error sending to remote", e );
			}
			
			return;
		}
		
		logger.debug( "WebSocket TEXT message received: " + text );
		
		String msg = text.trim();
		if( msg.startsWith( "startMessages" ))
		{
			String zooKeeperHost = configProps.getProperty( "kafkaZookeeperHost" );
			logger.info( "kafkaZookeeperHost: " + zooKeeperHost );
			String zooKeeperPort = configProps.getProperty( "kafkaZookeeperPort" );
			logger.info( "kafkaZookeeperPort: " + zooKeeperPort );
			String groupId = null; // configProps.getProperty( "kafkaGroupId" );
		    String topic = configProps.getProperty( "kafkaTopicName" );
			logger.info( "kafkaTopic: " + topic );
			
		    // try to parse out a groupID.  If one exists, use it. If not
		    // generate a new one and return to the client. 
		    
		    if( msg.contains( ":" ))
		    {
		    	String[] parts = msg.split( ":" );
		    	groupId = parts[1];
		    }
		    else
		    {
		    	groupId = UUID.randomUUID().toString();
		    	try
		    	{
		    		session.getRemote().sendString( "groupId:" + groupId );
		    	}
		    	catch( IOException e )
		    	{
		    		throw new RuntimeException( e );
		    	}
		    }
			
		    System.out.println( "using groupId: " + groupId );
		    client = new KafkaClient(zooKeeperHost + ":" + zooKeeperPort, groupId, topic, session.getRemote() );
	        client.run(threads);
		}
		else if( msg.equals( "stopMessages" ))
		{
			client.shutdown();
		}
		else
		{
			logger.error("Received invalid message from remote: " + text );
		}
	}
	
	@OnWebSocketMessage
	public void onMessage( Session session, byte[] buff, int offset, int length )
	{
		logger.info( "WebSocket BINARY message received!" );
	}
}
