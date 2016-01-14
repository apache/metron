package org.apache.metron.dataservices.modules.guice;

import javax.inject.Singleton;

import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;
import org.apache.metron.dataservices.common.MetronService;
import org.apache.metron.services.alerts.ElasticSearch_KafkaAlertsService;
import org.apache.metron.services.alerts.Solr_KafkaAlertsService;

public class ServiceModule extends RequestScopeModule {

	private static final Logger logger = LoggerFactory.getLogger( ServiceModule.class );
	
    private String[] args;

    public ServiceModule(String[] args) {
        this.args = args;
    }

    @Provides
    @Singleton
    public MetronService socservice() {
        if (args.length > 0 && args[0].equals("ElasticSearch_KafkaAlertsService")) {
            return new ElasticSearch_KafkaAlertsService();
        } else {
            return new Solr_KafkaAlertsService();
        }
    }
}
