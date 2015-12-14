package com.opensoc.dataservices.modules.guice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.opensoc.dataservices.auth.RestSecurityInterceptor;
import com.opensoc.dataservices.rest.Index;
import com.opensoc.pcapservice.PcapReceiverImplRestEasy;

public class RestEasyModule extends AbstractModule {
	
	private static final Logger logger = LoggerFactory.getLogger( RestEasyModule.class );
	
	@Override
	protected void configure() {
		
		bind( Index.class );
		bind( PcapReceiverImplRestEasy.class );
		bind( RestSecurityInterceptor.class );
	}
}
