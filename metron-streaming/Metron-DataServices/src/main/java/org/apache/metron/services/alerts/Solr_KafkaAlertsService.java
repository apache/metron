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
package org.apache.metron.services.alerts;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.dataservices.common.MetronService;

@Singleton
public class Solr_KafkaAlertsService implements MetronService {

	private static final Logger logger = LoggerFactory.getLogger( Solr_KafkaAlertsService.class );	
	
	@Override
	public String identify() {
		// TODO Auto-generated method stub
		return "Elastic Search to Solr Alerts Service";
	}

	@Override
	public boolean init(String topicname) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean login() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean registerRulesFromFile() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean registerRules() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String viewRules() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean editRules() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRules() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean registerForAlertsTopic(String topicname) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String receiveAlertAll() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean disconnectFromAlertsTopic(String topicname) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String receiveAlertLast() {
		// TODO Auto-generated method stub
		return null;
	}


}
