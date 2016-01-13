package com.apache.metron.dataservices.websocket;

import java.net.HttpCookie;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.apache.metron.dataservices.auth.AuthToken;

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