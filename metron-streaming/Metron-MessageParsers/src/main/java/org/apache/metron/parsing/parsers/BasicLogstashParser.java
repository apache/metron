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
package org.apache.metron.parsing.parsers;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class BasicLogstashParser extends BasicParser {

	@Override
	public void init() {

	}

	@Override
	public List<JSONObject> parse(byte[] raw_message) {
		List<JSONObject> messages = new ArrayList<>();
		try {
			
			/*
			 * We need to create a new JSONParser each time because its 
			 * not serializable and the parser is created on the storm nimbus
			 * node, then transfered to the workers.
			 */
			JSONParser jsonParser = new JSONParser();
			String rawString = new String(raw_message, "UTF-8");
			JSONObject rawJson = (JSONObject) jsonParser.parse(rawString);
			
			// remove logstash meta fields
			rawJson.remove("@version");
			rawJson.remove("type");
			rawJson.remove("host");
			rawJson.remove("tags");
			
			// rename other keys
			rawJson = mutate(rawJson, "message", "original_string");
			rawJson = mutate(rawJson, "src_ip", "ip_src_addr");
			rawJson = mutate(rawJson, "dst_ip", "ip_dst_addr");
			rawJson = mutate(rawJson, "src_port", "ip_src_port");
			rawJson = mutate(rawJson, "dst_port", "ip_dst_port");
			rawJson = mutate(rawJson, "src_ip", "ip_src_addr");
			
			// convert timestamp to milli since epoch
			long timestamp = LogstashToEpoch((String) rawJson.remove("@timestamp"));
			rawJson.put("timestamp", timestamp);
			messages.add(rawJson);
			return messages;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
	}
	
	private JSONObject mutate(JSONObject json, String oldKey, String newKey) {
		if (json.containsKey(oldKey)) {
			json.put(newKey, json.remove(oldKey));
		}	
		return json;
	}
	
	private long LogstashToEpoch(String timestamp) throws java.text.ParseException {
		SimpleDateFormat logstashDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		return logstashDateFormat.parse(timestamp).getTime();
		
	}

	

}
