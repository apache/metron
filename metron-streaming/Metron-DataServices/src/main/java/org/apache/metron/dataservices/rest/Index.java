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
package org.apache.metron.dataservices.rest;

import java.util.List;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import org.apache.metron.dataservices.auth.AuthTokenFilter;
import org.apache.metron.dataservices.kafkaclient.poll.PollingKafkaClient;

@Path("/")
public class Index 
{
	private static final Logger logger = LoggerFactory.getLogger( Index.class );
	
	@Inject
	private Properties configProps;
	
	@AuthTokenFilter
	@GET
	@Path("/alerts/{groupId}")
	public Response getAlerts( @PathParam("groupId") String groupId ) 
	{
		String zooKeeperHost = configProps.getProperty( "kafkaZookeeperHost" );
		logger.info( "kafkaZookeeperHost: " + zooKeeperHost );
		String zooKeeperPort = configProps.getProperty( "kafkaZookeeperPort" );
		logger.info( "kafkaZookeeperPort: " + zooKeeperPort );
		
		logger.warn( "got groupId from path as: " + groupId );
		
		PollingKafkaClient client = new PollingKafkaClient( zooKeeperHost + ":" + zooKeeperPort, groupId, "test"); 
		List<String> messages = client.fetchMessages();
		logger.warn( "found " + messages.size() + " messages in Kafka" );
		
		String respString1 = "<html><body><h2>Messages:</h2><ul>";
		String respString2 = "</ul></body></html>";
		
		for( String msg : messages )
		{
			respString1 = respString1 + "<li>" + msg + "</li>";
		}
		
		return Response.status(200).entity( respString1 + respString2 ).build();
				
	}
}
