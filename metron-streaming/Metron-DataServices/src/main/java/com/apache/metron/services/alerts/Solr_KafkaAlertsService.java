package com.opensoc.services.alerts;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensoc.dataservices.common.OpenSOCService;

@Singleton
public class Solr_KafkaAlertsService implements OpenSOCService {

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
