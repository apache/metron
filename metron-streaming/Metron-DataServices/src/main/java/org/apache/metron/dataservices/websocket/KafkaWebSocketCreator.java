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

import java.net.HttpCookie;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import org.apache.metron.dataservices.auth.AuthToken;

public class KafkaWebSocketCreator implements WebSocketCreator
{
	private static final Logger logger = LoggerFactory.getLogger( KafkaWebSocketCreator.class );
	
	@Inject
	private Properties configProps;
	
	@Override
	public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) 
	{
		boolean authGood = false;
		List<HttpCookie> cookies = request.getCookies();
		for( HttpCookie cookie : cookies )
		{
			String name = cookie.getName();
			if( name!= null && name.equals( "authToken" ))
			{
				String value = cookie.getValue();
				
				try
				{
					if( value != null && AuthToken.validateToken(configProps, value))
					{
						authGood = true;
						break;
					}
				}
				catch( Exception e )
				{
					logger.error(" Exception validating authToken:", e );
					authGood = false;
					break;
				}
				
			}
			else
			{
				continue;
			}
		}

		return new KafkaMessageSenderSocket( configProps, authGood );
	}
}
