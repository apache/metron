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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.metron.alerts.server.AlertsCacheReaper;
import org.apache.metron.alerts.server.AlertsProcessingServer;
import org.apache.metron.alerts.server.AlertsSearcher;

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
