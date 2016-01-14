package org.apache.metron.alerts.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.inject.Inject;

public class AlertsCacheReaper implements Runnable {
	
	private AlertsSearcher searcher;
	
	@Inject
	private Properties configProps;
	
	public void setSearcher(AlertsSearcher searcher) {
		this.searcher = searcher;
	}
	
	@Override
	public void run() {
	
		long expireAlertsCacheInterval = Long.parseLong( configProps.getProperty( "alerts.cache.expiration.interval", "360000" ) );
		long timeNow = System.currentTimeMillis();
		
		long cutOffTime = timeNow - expireAlertsCacheInterval;
		
		List<String> forRemoval = new ArrayList<String>(); 
		
		for( Map.Entry<String, AlertsFilterCacheEntry> entry : searcher.alertsFilterCache.entrySet()  )
		{
			// if entry was saved more than X timeunits ago, remove it
			if( entry.getValue().storedAtTime < cutOffTime )
			{
				forRemoval.add(entry.getKey());
			}
		}
		
		for( String key : forRemoval )
		{
			searcher.alertsFilterCache.remove(key);
		}
	}
}