package com.apache.metron.dataservices.common;

public interface MetronService {

	//secure service that front elastic search or solr
	//and the message broker
	
    public String identify();
    public boolean init(String topicname);
    public boolean login();
    
  //standing query operations
    public boolean registerRulesFromFile();
    public boolean registerRules();
    public String viewRules();
    public boolean editRules();
    public boolean deleteRules();
    
  //register for writing to kafka topic
    public boolean registerForAlertsTopic(String topicname);
    
  //client registers for alerts  
    public String receiveAlertAll();
    public String receiveAlertLast();
    public boolean disconnectFromAlertsTopic(String topicname);
    
}
