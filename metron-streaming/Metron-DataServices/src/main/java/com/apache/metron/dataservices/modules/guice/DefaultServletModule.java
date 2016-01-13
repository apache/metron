package com.apache.metron.dataservices.modules.guice;

import java.util.Properties;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.apache.metron.dataservices.servlet.LoginServlet;
import com.apache.metron.dataservices.servlet.LogoutServlet;
import com.apache.metron.dataservices.websocket.KafkaMessageSenderServlet;
import com.apache.metron.dataservices.websocket.KafkaWebSocketCreator;

public class DefaultServletModule extends ServletModule {
    
	private static final Logger logger = LoggerFactory.getLogger( DefaultServletModule.class );	
	
    private Properties configProps;

    public DefaultServletModule( final Properties configProps ) {
        this.configProps = configProps;
    }	
	
	@Override
    protected void configureServlets() {
        
		ShiroWebModule.bindGuiceFilter(binder());
		
		bind( KafkaWebSocketCreator.class ).in(Singleton.class);
		
        bind( HttpServletDispatcher.class ).in(Singleton.class);
        serve( "/rest/*").with(HttpServletDispatcher.class);
        
        bind( KafkaMessageSenderServlet.class ).in(Singleton.class);
		serve( "/ws/*").with(KafkaMessageSenderServlet.class );
		
		bind( LoginServlet.class).in(Singleton.class);
		serve( "/login" ).with( LoginServlet.class );
        
		bind( LogoutServlet.class).in(Singleton.class);
		serve( "/logout" ).with( LogoutServlet.class );
		
    }
}