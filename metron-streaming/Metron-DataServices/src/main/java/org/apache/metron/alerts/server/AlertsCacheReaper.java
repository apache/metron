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
