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
