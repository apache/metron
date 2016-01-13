package org.apache.metron.dataservices.modules.guice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import org.apache.metron.dataservices.auth.RestSecurityInterceptor;
import org.apache.metron.dataservices.rest.Index;
import org.apache.metron.pcapservice.PcapReceiverImplRestEasy;

public class RestEasyModule extends AbstractModule {
	
	private static final Logger logger = LoggerFactory.getLogger( RestEasyModule.class );
	
	@Override
	protected void configure() {
		
		bind( Index.class );
		bind( PcapReceiverImplRestEasy.class );
		bind( RestSecurityInterceptor.class );
	}
}
