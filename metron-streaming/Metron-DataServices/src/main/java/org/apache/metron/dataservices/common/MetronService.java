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
package org.apache.metron.dataservices.common;

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
