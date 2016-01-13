package com.apache.metron.dataservices.modules.guice;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.apache.metron.alerts.server.AlertsCacheReaper;
import com.apache.metron.alerts.server.AlertsProcessingServer;
import com.apache.metron.alerts.server.AlertsSearcher;

public class AlertsServerModule extends AbstractModule {
	
	private static final Logger logger = LoggerFactory.getLogger( AlertsServerModule.class );	
	
	private Properties configProps;
	
	public AlertsServerModule( final Properties configProps ) {
		this.configProps = configProps;
	}
	
	@Override
	protected void configure() {
		bind( AlertsProcessingServer.class).in(Singleton.class);
		bind( AlertsSearcher.class).in(Singleton.class);
		bind( AlertsCacheReaper.class ).in(Singleton.class );
	}
	
	@Provides Properties getConfigProps()
	{
		return configProps;
	}
}