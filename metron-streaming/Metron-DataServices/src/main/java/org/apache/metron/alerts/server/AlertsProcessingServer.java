package org.apache.metron.alerts.server;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class AlertsProcessingServer {
	
	private static final Logger logger = LoggerFactory.getLogger( AlertsProcessingServer.class );
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	@Inject
	private AlertsSearcher searcher;
	@Inject
	private AlertsCacheReaper reaper;
	@Inject
	private Properties configProps;
	
	public void startProcessing() {
		
		logger.debug( "startProcessing() invoked" );
		
		int initialDelayTime = Integer.parseInt( configProps.getProperty( "searchInitialDelayTime", "30" ) );
		int searchIntervalTime = Integer.parseInt( configProps.getProperty( "searchIntervalTime", "30" ) );
		
		reaper.setSearcher(searcher);
		
		final ScheduledFuture<?> alertsSearcherHandle =
			       scheduler.scheduleAtFixedRate( searcher, initialDelayTime, searchIntervalTime, SECONDS );
				
		scheduler.scheduleAtFixedRate(reaper, 120, 380, SECONDS);
		
	}
}