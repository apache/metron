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
