package org.apache.metron.dataservices.websocket;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@WebServlet(name = "Message Sender Servlet", urlPatterns = { "/messages" })
public class KafkaMessageSenderServlet extends WebSocketServlet
{	
	private static final Logger logger = LoggerFactory.getLogger( KafkaMessageSenderServlet.class );	
	
	@Inject
	private KafkaWebSocketCreator socketCreator;
	
	@Override
	public void configure(WebSocketServletFactory factory) 
	{
		factory.getPolicy().setIdleTimeout(600000);
		factory.setCreator( socketCreator );
	}
}