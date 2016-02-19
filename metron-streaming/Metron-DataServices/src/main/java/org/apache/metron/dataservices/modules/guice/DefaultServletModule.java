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
package org.apache.metron.dataservices.modules.guice;

import java.util.Properties;

import org.apache.shiro.guice.web.ShiroWebModule;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import org.apache.metron.dataservices.servlet.LoginServlet;
import org.apache.metron.dataservices.servlet.LogoutServlet;
import org.apache.metron.dataservices.websocket.KafkaMessageSenderServlet;
import org.apache.metron.dataservices.websocket.KafkaWebSocketCreator;

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
