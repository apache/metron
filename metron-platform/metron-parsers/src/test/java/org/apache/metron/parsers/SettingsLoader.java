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
package org.apache.metron.parsers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SettingsLoader {

	@SuppressWarnings("unchecked")
	public static JSONObject loadEnvironmentIdnetifier(String config_path)
			throws ConfigurationException {
		Configuration config = new PropertiesConfiguration(config_path);

		String customer = config.getString("customer.id", "unknown");
		String datacenter = config.getString("datacenter.id", "unknown");
		String instance = config.getString("instance.id", "unknown");

		JSONObject identifier = new JSONObject();
		identifier.put("customer", customer);
		identifier.put("datacenter", datacenter);
		identifier.put("instance", instance);

		return identifier;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject loadTopologyIdnetifier(String config_path)
			throws ConfigurationException {
		Configuration config = new PropertiesConfiguration(config_path);

		String topology = config.getString("topology.id", "unknown");
		String instance = config.getString("instance.id", "unknown");

		JSONObject identifier = new JSONObject();
		identifier.put("topology", topology);
		identifier.put("topology_instance", instance);

		return identifier;
	}
	

	public static String generateTopologyName(JSONObject env, JSONObject topo) {

		return (env.get("customer") + "_" + env.get("datacenter") + "_"
				+ env.get("instance") + "_" + topo.get("topology") + "_" + topo.get("topology_instance"));
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject generateAlertsIdentifier(JSONObject env, JSONObject topo)
	{
		JSONObject identifier = new JSONObject();
		identifier.put("environment", env);
		identifier.put("topology", topo);
		
		return identifier;
	}

	public static Map<String, JSONObject> loadRegexAlerts(String config_path)
			throws ConfigurationException, ParseException {
		XMLConfiguration alert_rules = new XMLConfiguration();
		alert_rules.setDelimiterParsingDisabled(true);
		alert_rules.load(config_path);

		//int number_of_rules = alert_rules.getList("rule.pattern").size();

		String[] patterns = alert_rules.getStringArray("rule.pattern");
		String[] alerts = alert_rules.getStringArray("rule.alert");

		JSONParser pr = new JSONParser();
		Map<String, JSONObject> rules = new HashMap<String, JSONObject>();

		for (int i = 0; i < patterns.length; i++)
			rules.put(patterns[i], (JSONObject) pr.parse(alerts[i]));

		return rules;
	}

	public static Map<String, JSONObject> loadKnownHosts(String config_path)
			throws ConfigurationException, ParseException {
		Configuration hosts = new PropertiesConfiguration(config_path);

		Iterator<String> keys = hosts.getKeys();
		Map<String, JSONObject> known_hosts = new HashMap<String, JSONObject>();
		JSONParser parser = new JSONParser();

		while (keys.hasNext()) {
			String key = keys.next().trim();
			JSONArray value = (JSONArray) parser.parse(hosts.getProperty(key)
					.toString());
			known_hosts.put(key, (JSONObject) value.get(0));
		}

		return known_hosts;
	}

	public static void printConfigOptions(PropertiesConfiguration config, String path_fragment)
	{
		Iterator<String> itr = config.getKeys();
		
		while(itr.hasNext())
		{
			String key = itr.next();
			
			if(key.contains(path_fragment))
			{
				
				System.out.println("[Metron] Key: " + key + " -> " + config.getString(key));
			}
		}

	}
	
	public static void printOptionalSettings(Map<String, String> settings)
	{
		for(String setting: settings.keySet())
		{
			System.out.println("[Metron] Optional Setting: " + setting + " -> " +settings.get(setting));
		}

	}
	
	public static Map<String, String> getConfigOptions(PropertiesConfiguration config, String path_fragment)
	{
		Iterator<String> itr = config.getKeys();
		Map<String, String> settings = new HashMap<String, String>();
		
		while(itr.hasNext())
		{
			String key = itr.next();
			
			if(key.contains(path_fragment))
			{
				String tmp_key = key.replace(path_fragment, "");
				settings.put(tmp_key, config.getString(key));
			}
		}

		return settings;
	}
}
